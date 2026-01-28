package com.riprod.hexcode.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.ColorLight;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.DynamicLight;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.casting.styles.OrbitalElement;
import com.riprod.hexcode.glyph.GlyphVisual;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.hex.HexNode;
import com.riprod.hexcode.math.GlyphRotation;

/**
 * Represents a floating hex entity that appears around a player.
 *
 * Hexes are positioned relative to the player and move with them automatically.
 * When dragged, they become independent and can be repositioned freely.
 * Hexes always face towards the player.
 *
 * Hexes are visualized as concentric shells where outer glyphs are larger
 * and inner glyphs are progressively smaller. This allows tier-based
 * drop detection for inserting glyphs at specific tree levels.
 */
public class HexEntity implements OrbitalElement {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final float BOUNDING_BOX_SIZE = 0.5f;

    private final Hex hex;
    private final Ref<EntityStore> ownerPlayer;
    private Ref<EntityStore> entityRef;

    /** Rotation (pitch/yaw) from player's eyes to this element */
    private GlyphRotation rotation;

    /** Lifecycle state machine */
    private ElementState state = ElementState.NOT_SPAWNED;

    /** Tracks if entity is pending spawn (entityRef set but not yet valid) */
    private boolean pendingSpawn = false;

    /** Child glyph entity references for proper cleanup */
    private final List<Ref<EntityStore>> childEntityRefs = new ArrayList<>();

    private boolean isAvailable;

    /** Visual rotation for spinning effect */
    private float visualRotation;

    // Tier-based angular margins for selection detection
    // tierTolerances[0] = outermost (largest), tierTolerances[depth-1] = innermost (smallest)
    private float[] tierTolerances;

    /**
     * Create a new HexEntity with a rotation from player's eyes.
     *
     * @param hex         The hex this entity represents
     * @param ownerPlayer Reference to the player who owns this hex
     * @param rotation    The rotation (pitch/yaw) from player's eyes
     */
    public HexEntity(Hex hex, Ref<EntityStore> ownerPlayer, GlyphRotation rotation) {
        this.hex = hex;
        this.ownerPlayer = ownerPlayer;
        this.rotation = rotation;

        this.state = ElementState.NOT_SPAWNED;
        this.isAvailable = true;
        this.visualRotation = 0.0f;

        // Calculate tier tolerances based on hex tree depth
        calculateTierTolerances();
    }

    /**
     * Create a new HexEntity with a player-relative offset (legacy compatibility).
     *
     * @param hex         The hex this entity represents
     * @param ownerPlayer Reference to the player who owns this hex
     * @param initialOffset The initial offset from the player position
     * @deprecated Use {@link #HexEntity(Hex, Ref, GlyphRotation)} instead
     */
    @Deprecated
    public HexEntity(Hex hex, Ref<EntityStore> ownerPlayer, Vector3d initialOffset) {
        this.hex = hex;
        this.ownerPlayer = ownerPlayer;
        // Convert offset to rotation
        Vector3d direction = new Vector3d(
            initialOffset.x,
            initialOffset.y - GlyphRotation.EYE_HEIGHT,
            initialOffset.z
        );
        this.rotation = GlyphRotation.fromDirection(direction);

        this.state = ElementState.NOT_SPAWNED;
        this.isAvailable = true;
        this.visualRotation = 0.0f;

        // Calculate tier tolerances based on hex tree depth
        calculateTierTolerances();
    }

    // --- OrbitalElement Interface Methods ---

    @Override
    public String getId() {
        return hex.getId();
    }

    @Override
    public boolean isSavedHex() {
        return true;
    }

    @Override
    public GlyphVisual getVisual() {
        // Return the primary (outermost/root) glyph's visual
        List<GlyphVisual> visuals = hex.getGlyphStyles();
        if (!visuals.isEmpty()) {
            return visuals.get(0);
        }
        // Return a default visual if no glyphs
        return GlyphVisual.select("Base_glyph");
    }

    // --- Tier Tolerance System ---

    /**
     * Calculate tier tolerances based on hex tree depth.
     * Outermost tier (index 0) has COMPOSED_TOLERANCE, inner tiers decay.
     */
    public void calculateTierTolerances() {
        int depth = hex.getMaxDepth();
        if (depth <= 0) {
            depth = 1;
        }
        tierTolerances = new float[depth];
        float tolerance = GlyphRotation.COMPOSED_TOLERANCE;
        for (int i = 0; i < depth; i++) {
            tierTolerances[i] = tolerance;
            tolerance *= GlyphRotation.TOLERANCE_DECAY;
            // Minimum tolerance to ensure innermost tiers are still selectable
            tolerance = Math.max(tolerance, 2.0f);
        }
    }

    /**
     * @return Array of tier tolerances in degrees, index 0 = outermost (largest)
     */
    public float[] getTierTolerances() {
        if (tierTolerances == null) {
            calculateTierTolerances();
        }
        return tierTolerances;
    }

    /**
     * Get the selection tolerance for a specific tier level.
     * @param tier The tier level (0 = outermost)
     * @return The tolerance in degrees for that tier
     */
    public float getTierTolerance(int tier) {
        if (tierTolerances == null) {
            calculateTierTolerances();
        }
        if (tier < 0 || tier >= tierTolerances.length) {
            return GlyphRotation.COMPOSED_TOLERANCE;
        }
        return tierTolerances[tier];
    }

    /**
     * Get the number of tiers (depth) in this hex.
     * @return Number of tiers
     */
    public int getTierCount() {
        return hex.getMaxDepth();
    }

    /**
     * @deprecated Use {@link #getTierTolerances()} instead. Rotation-based positioning uses angular tolerances.
     */
    @Deprecated
    public float[] getTierRadii() {
        // Convert tolerances to approximate radii for backward compatibility
        // This is a rough approximation: radius ≈ distance * tan(tolerance)
        if (tierTolerances == null) {
            calculateTierTolerances();
        }
        float[] radii = new float[tierTolerances.length];
        for (int i = 0; i < tierTolerances.length; i++) {
            radii[i] = (float) (GlyphRotation.DEFAULT_DISTANCE * Math.tan(Math.toRadians(tierTolerances[i])));
        }
        return radii;
    }

    /**
     * @deprecated Use {@link #getTierTolerance(int)} instead.
     */
    @Deprecated
    public float getTierRadius(int tier) {
        float tolerance = getTierTolerance(tier);
        return (float) (GlyphRotation.DEFAULT_DISTANCE * Math.tan(Math.toRadians(tolerance)));
    }

    /**
     * Get the HexNode at a specific tier level using depth-first traversal.
     * @param tier The tier level (0 = root/outermost)
     * @return The HexNode at that tier, or null if not found
     */
    public HexNode getNodeAtTier(int tier) {
        if (!hex.hasRoot()) {
            return null;
        }
        return findNodeAtDepth(hex.getRoot(), 0, tier);
    }

    private HexNode findNodeAtDepth(HexNode node, int currentDepth, int targetDepth) {
        if (currentDepth == targetDepth) {
            return node;
        }
        // Search children
        for (HexNode child : node.getChildren()) {
            HexNode found = findNodeAtDepth(child, currentDepth + 1, targetDepth);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * @return The hex this entity represents
     */
    public Hex getHex() {
        return hex;
    }

    @Override
    public Ref<EntityStore> getOwnerPlayer() {
        return ownerPlayer;
    }

    @Override
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    /**
     * Set the entity reference after spawning.
     */
    public void setEntityRef(Ref<EntityStore> entityRef) {
        this.entityRef = entityRef;
    }

    // --- Rotation-Based Positioning ---

    @Override
    public GlyphRotation getRotation() {
        return rotation;
    }

    @Override
    public void setRotation(GlyphRotation rotation) {
        this.rotation = rotation;
    }

    @Override
    public float getSelectionTolerance() {
        // Hexes (composed elements) have larger tolerance
        return GlyphRotation.COMPOSED_TOLERANCE;
    }

    @Override
    public void updatePositionFromPlayer(Store<EntityStore> store, Vector3d playerPosition) {
        if (isDragging()) {
            return; // Don't update position when being dragged
        }

        if (entityRef == null) {
            LOGGER.atWarning().log("HexEntity '%s' has null entityRef, cannot update position", hex.getId());
            return;
        }

        if (!entityRef.isValid()) {
            // Entity might not be spawned yet (deferred via CommandBuffer)
            // This is normal for the first few ticks after spawn
            return;
        }

        // Clear pending flag on first valid update
        if (pendingSpawn) {
            pendingSpawn = false;
            LOGGER.atInfo().log("HexEntity '%s' entity became valid", hex.getId());
        }

        // Calculate world position from rotation
        Vector3d worldPos = rotation.toWorldPosition(playerPosition);

        TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform != null) {
            transform.setPosition(worldPos);

            // Face the player
            updateFacingTowardsPlayer(transform, playerPosition, worldPos);
        }
    }

    /**
     * Rotate hex to face the player.
     */
    private void updateFacingTowardsPlayer(TransformComponent transform, Vector3d playerPosition, Vector3d hexPos) {
        // Calculate yaw angle to face player
        double dx = playerPosition.x - hexPos.x;
        double dz = playerPosition.z - hexPos.z;
        float yaw = (float) Math.atan2(dx, dz);
        transform.setRotation(new Vector3f(0, yaw, 0));
    }

    @Override
    public void captureRotationFromLook(Store<EntityStore> store, GlyphRotation lookRotation) {
        this.rotation = lookRotation;
        LOGGER.atInfo().log("HexEntity '%s' captured rotation: %s", hex.getId(), lookRotation);
    }

    /**
     * @deprecated Use {@link #captureRotationFromLook(Store, GlyphRotation)} instead.
     */
    @Override
    @Deprecated
    public void captureOffsetFromPlayer(Store<EntityStore> store, Vector3d playerPosition) {
        if (entityRef == null || !entityRef.isValid()) {
            LOGGER.atWarning().log("HexEntity '%s' cannot capture offset - entityRef invalid", hex.getId());
            return;
        }

        TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform != null) {
            Vector3d currentPos = transform.getPosition();
            Vector3d eyePos = new Vector3d(playerPosition.x, playerPosition.y + GlyphRotation.EYE_HEIGHT, playerPosition.z);
            this.rotation = GlyphRotation.fromWorldPosition(eyePos, currentPos);
            LOGGER.atInfo().log("HexEntity '%s' captured rotation: %s", hex.getId(), rotation);
        }
    }

    // --- State Machine ---

    @Override
    public ElementState getState() {
        return state;
    }

    @Override
    public boolean transitionTo(ElementState newState) {
        if (!OrbitalElement.isValidTransition(state, newState)) {
            LOGGER.atWarning().log("Invalid state transition: %s -> %s for hex '%s'",
                state, newState, hex.getId());
            return false;
        }
        ElementState oldState = this.state;
        this.state = newState;
        LOGGER.atInfo().log("Hex '%s' state: %s -> %s", hex.getId(), oldState, newState);
        return true;
    }

    // --- State Flags (backed by state machine) ---

    @Override
    public boolean isHovered() {
        return state == ElementState.HOVERING;
    }

    @Override
    public void setHovered(boolean hovered) {
        if (hovered) {
            if (state == ElementState.IDLE) {
                transitionTo(ElementState.HOVERING);
            }
        } else {
            if (state == ElementState.HOVERING) {
                transitionTo(ElementState.IDLE);
            }
        }
    }

    @Override
    public boolean isDragging() {
        return state == ElementState.DRAGGING;
    }

    @Override
    public void setDragging(boolean dragging) {
        if (dragging) {
            if (state == ElementState.IDLE || state == ElementState.HOVERING) {
                transitionTo(ElementState.DRAGGING);
            }
        } else {
            if (state == ElementState.DRAGGING) {
                transitionTo(ElementState.IDLE);
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return isAvailable;
    }

    @Override
    public void setAvailable(boolean available) {
        this.isAvailable = available;
    }

    @Override
    public boolean isPendingSpawn() {
        return pendingSpawn;
    }

    @Override
    public void clearPendingSpawn() {
        this.pendingSpawn = false;
    }

    /**
     * Get the current visual rotation angle.
     *
     * @return Rotation in radians
     */
    public float getVisualRotation() {
        return visualRotation;
    }

    // --- Lifecycle ---

    @Override
    public void spawn(CommandBuffer<EntityStore> commandBuffer, Vector3d spawnPosition, UUID ownerPlayerId) {
        if (state != ElementState.NOT_SPAWNED) {
            LOGGER.atWarning().log("Hex entity '%s' cannot spawn - already in state %s",
                hex.getId(), state);
            return;
        }
        if (entityRef != null) {
            LOGGER.atWarning().log("Hex entity '%s' already spawned", hex.getId());
            return;
        }

        // Clear any stale child references
        childEntityRefs.clear();

        // Create root entity holder for the hex
        Holder<EntityStore> rootHolder = EntityStore.REGISTRY.newHolder();

        // Add UUID component to root
        rootHolder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(UUID.randomUUID()));

        // Add transform component with position at root
        TransformComponent rootTransform = new TransformComponent(spawnPosition, new Vector3f(0, 0, 0));
        rootHolder.addComponent(TransformComponent.getComponentType(), rootTransform);

        // Load all glyphs within the hex as child entities with their offsets and scales
        List<GlyphVisual> visuals = hex.getGlyphStyles();
        ColorLight dominantLight = null;
        int glyphCount = 0;

        for (GlyphVisual glyphVisual : visuals) {
            String modelId = glyphVisual.getModelId();
            ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(modelId);
            if (modelAsset != null) {
                // Create a child entity for this glyph
                Holder<EntityStore> glyphHolder = EntityStore.REGISTRY.newHolder();

                // Add UUID component
                glyphHolder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(UUID.randomUUID()));

                // Apply glyph's relative position offset from the root
                float offsetX = glyphVisual.getOffsetX();
                float offsetY = glyphVisual.getOffsetY();
                float offsetZ = glyphVisual.getOffsetZ();
                Vector3d glyphPosition = new Vector3d(spawnPosition.x + offsetX, spawnPosition.y + offsetY, spawnPosition.z + offsetZ);

                // Create transform with the offset position
                Vector3f scaledRotation = new Vector3f(0, 0, 0);
                TransformComponent glyphTransform = new TransformComponent(glyphPosition, scaledRotation);
                glyphHolder.addComponent(TransformComponent.getComponentType(), glyphTransform);

                // Load and add the model
                Model model = Model.createUnitScaleModel(modelAsset);
                glyphHolder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
                glyphHolder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));

                // Track the first glyph's light for the dominant light effect
                if (dominantLight == null) {
                    dominantLight = createColorLightFromGlyph(glyphVisual);
                }

                glyphHolder.addComponent(NetworkId.getComponentType(),
                        new NetworkId(commandBuffer.getExternalData().takeNextNetworkId()));

                // Add BoundingBox for hit detection
                Box box = new Box(
                        -BOUNDING_BOX_SIZE, -BOUNDING_BOX_SIZE, -BOUNDING_BOX_SIZE,
                        BOUNDING_BOX_SIZE, BOUNDING_BOX_SIZE, BOUNDING_BOX_SIZE);
                glyphHolder.addComponent(BoundingBox.getComponentType(), new BoundingBox(box));

                // Spawn the glyph child entity and track reference for cleanup
                Ref<EntityStore> childRef = commandBuffer.addEntity(glyphHolder, AddReason.SPAWN);
                childEntityRefs.add(childRef);
                glyphCount++;
            } else {
                LOGGER.atWarning().log("Could not load model '%s' for glyph in hex '%s'",
                        modelId, hex.getId());
            }
        }

        if (glyphCount == 0) {
            // Try loading the base_glyph model as fallback
            ModelAsset fallbackAsset = ModelAsset.getAssetMap().getAsset("Base_glyph");
            if (fallbackAsset != null) {
                Holder<EntityStore> glyphHolder = EntityStore.REGISTRY.newHolder();
                glyphHolder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(UUID.randomUUID()));

                TransformComponent glyphTransform = new TransformComponent(spawnPosition, new Vector3f(0, 0, 0));
                glyphHolder.addComponent(TransformComponent.getComponentType(), glyphTransform);

                Model model = Model.createUnitScaleModel(fallbackAsset);
                glyphHolder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
                glyphHolder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));

                glyphHolder.addComponent(NetworkId.getComponentType(),
                        new NetworkId(commandBuffer.getExternalData().takeNextNetworkId()));

                Box box = new Box(
                        -BOUNDING_BOX_SIZE, -BOUNDING_BOX_SIZE, -BOUNDING_BOX_SIZE,
                        BOUNDING_BOX_SIZE, BOUNDING_BOX_SIZE, BOUNDING_BOX_SIZE);
                glyphHolder.addComponent(BoundingBox.getComponentType(), new BoundingBox(box));

                // Track fallback child entity reference for cleanup
                Ref<EntityStore> childRef = commandBuffer.addEntity(glyphHolder, AddReason.SPAWN);
                childEntityRefs.add(childRef);
                glyphCount++;
                LOGGER.atInfo().log("Using fallback model for hex '%s'", hex.getId());
            } else {
                LOGGER.atWarning().log("Could not load fallback model for hex '%s'",
                        hex.getId());
            }
        }

        // Add dynamic light based on primary glyph color for glow effect on root entity
        if (dominantLight != null) {
            DynamicLight dynamicLight = new DynamicLight(dominantLight);
            rootHolder.addComponent(DynamicLight.getComponentType(), dynamicLight);
        }

        rootHolder.addComponent(NetworkId.getComponentType(),
                new NetworkId(commandBuffer.getExternalData().takeNextNetworkId()));

        // Add BoundingBox for hit detection on root
        Box box = new Box(
                -BOUNDING_BOX_SIZE, -BOUNDING_BOX_SIZE, -BOUNDING_BOX_SIZE,
                BOUNDING_BOX_SIZE, BOUNDING_BOX_SIZE, BOUNDING_BOX_SIZE);
        rootHolder.addComponent(BoundingBox.getComponentType(), new BoundingBox(box));

        // Add glyph component if component type is registered
        if (GlyphComponent.getComponentType() != null && ownerPlayerId != null) {
            GlyphComponent glyphComp = new GlyphComponent(
                    hex.getId(),
                    ownerPlayerId,
                    rotation.getPitch(),
                    rotation.getYaw());
            rootHolder.addComponent(GlyphComponent.getComponentType(), glyphComp);
        }

        // Add root entity via CommandBuffer (deferred execution after tick completes)
        entityRef = commandBuffer.addEntity(rootHolder, AddReason.SPAWN);
        this.pendingSpawn = true;  // Mark as pending until entity becomes valid

        // Transition to IDLE state
        transitionTo(ElementState.IDLE);

        LOGGER.atInfo().log("Spawned hex entity '%s' with %d glyphs (%d child entities) at (%.1f, %.1f, %.1f)",
                hex.getId(), glyphCount, childEntityRefs.size(), spawnPosition.x, spawnPosition.y, spawnPosition.z);
    }

    @Override
    public void despawn(CommandBuffer<EntityStore> commandBuffer) {
        // Already consumed - nothing to do
        if (state == ElementState.CONSUMED) {
            return;
        }

        // Transition to CONSUMED first (ensures state is clean even if remove fails)
        transitionTo(ElementState.CONSUMED);

        // Despawn all child glyph entities first
        int childCount = childEntityRefs.size();
        for (Ref<EntityStore> childRef : childEntityRefs) {
            if (childRef != null && childRef.isValid()) {
                commandBuffer.removeEntity(childRef, RemoveReason.REMOVE);
            }
        }
        childEntityRefs.clear();

        // Despawn root entity
        if (entityRef != null) {
            commandBuffer.removeEntity(entityRef, RemoveReason.REMOVE);
            LOGGER.atInfo().log("Despawned hex entity '%s' with %d child entities", hex.getId(), childCount);
            entityRef = null;
        }
    }

    @Override
    public void updateWorldPositionDirect(Store<EntityStore> store, Vector3d position) {
        if (entityRef == null || !entityRef.isValid()) {
            // Entity might not be spawned yet - silently skip
            return;
        }

        TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform != null) {
            transform.setPosition(position);
        }
    }

    @Override
    public void updateHoverVisual(Store<EntityStore> store) {
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        DynamicLight light = store.getComponent(entityRef, DynamicLight.getComponentType());
        if (light != null) {
            // Get the primary (first) glyph visual for hover effect
            List<GlyphVisual> visuals = hex.getGlyphStyles();
            if (!visuals.isEmpty()) {
                GlyphVisual primaryVisual = visuals.get(0);
                ColorLight colorLight = createColorLightFromGlyph(primaryVisual);

                // Increase intensity when hovered
                if (isHovered()) {
                    colorLight.radius = (byte) Math.min(255, colorLight.radius + 4);
                }

                light.setColorLight(colorLight);
            }
        }
    }

    /**
     * Refresh visual state after hex structure changes (e.g., adding a child node).
     * Recalculates tier tolerances and updates visual components.
     *
     * @param store The entity store for component access
     */
    public void refreshVisuals(Store<EntityStore> store) {
        // Recalculate tier tolerances based on new depth
        calculateTierTolerances();

        // Update hover visual (which handles light intensity)
        updateHoverVisual(store);

        LOGGER.atInfo().log("Refreshed visuals for hex '%s' (depth: %d)", hex.getId(), hex.getMaxDepth());
    }

    /**
     * Refresh visual entities after hex structure changes.
     * This version uses CommandBuffer for deferred entity operations.
     *
     * @param commandBuffer The command buffer for deferred entity operations
     */
    public void refreshVisuals(CommandBuffer<EntityStore> commandBuffer) {
        // Recalculate tier tolerances based on new depth
        calculateTierTolerances();

        // Update hover visual using store
        Store<EntityStore> store = commandBuffer.getStore();
        updateHoverVisual(store);

        // TODO: For major visual changes (like respawning child entities),
        // we would despawn and respawn here. For now, just recalculate.

        LOGGER.atInfo().log("Refreshed visuals for hex '%s' (depth: %d)", hex.getId(), hex.getMaxDepth());
    }

    /**
     * Create a ColorLight from glyph visual properties.
     *
     * glowIntensity is expected to be 0.0-1.0 range:
     * - Used to scale RGB brightness (0.05 = 5% of full color brightness)
     * - Used to calculate radius (0.05 * 60 = 3 blocks of light reach)
     */
    private ColorLight createColorLightFromGlyph(GlyphVisual visual) {
        int color = visual.getColor();
        float intensity = visual.getGlowIntensity();

        // Extract RGB from color int
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        // Scale RGB by intensity to control brightness
        // intensity of 0.05 = 5% brightness, 1.0 = full brightness
        red = (int)(red * intensity);
        green = (int)(green * intensity);
        blue = (int)(blue * intensity);

        // Create ColorLight
        ColorLight colorLight = new ColorLight();
        // Radius: intensity * 60 gives reasonable range (0.05 = 3 blocks, 1.0 = 60 blocks)
        // Minimum of 1 to avoid undefined behavior with radius=0
        colorLight.radius = (byte) Math.max(1, Math.min(255, (int)(intensity)));
        colorLight.red = (byte) red;
        colorLight.green = (byte) green;
        colorLight.blue = (byte) blue;

        return colorLight;
    }
}
