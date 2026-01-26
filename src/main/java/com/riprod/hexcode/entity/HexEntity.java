package com.riprod.hexcode.entity;

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
import com.riprod.hexcode.config.HexcodeConfig;
import com.riprod.hexcode.glyph.GlyphVisual;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.hex.HexNode;
import com.riprod.hexcode.util.HexMathUtil;

/**
 * Represents a floating hex entity in the orbital ring around a player.
 *
 * When in glyph mode, saved hexes from the player's book appear as
 * HexEntity objects that slowly orbit around the player.
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

    private float orbitAngle;
    private float orbitSpeed;
    private float orbitalRadius;
    private float height;

    private boolean isHovered;
    private boolean isDragging;
    private boolean isAvailable;
    private boolean excludedFromOrbit;

    // Visual rotation for spinning effect
    private float visualRotation;

    // Tier-based visual sizing for drop detection
    // tierRadii[0] = outermost (largest), tierRadii[depth-1] = innermost (smallest)
    private float[] tierRadii;
    private static final float BASE_TIER_RADIUS = 0.5f;
    private static final float TIER_RADIUS_STEP = 0.15f;

    public HexEntity(Hex hex, Ref<EntityStore> ownerPlayer, float initialAngle) {
        this.hex = hex;
        this.ownerPlayer = ownerPlayer;
        this.orbitAngle = initialAngle;

        HexcodeConfig config = HexcodeConfig.getInstance();
        this.orbitSpeed = config.getOrbitSpeed();
        this.orbitalRadius = config.getOrbitalRadius();
        this.height = 1.0f; // Chest height offset

        this.isHovered = false;
        this.isDragging = false;
        this.isAvailable = true;
        this.excludedFromOrbit = false;
        this.visualRotation = 0.0f;

        // Calculate tier radii based on hex tree depth
        calculateTierRadii();
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

    // --- Tier Radius System ---

    /**
     * Calculate tier radii based on hex tree depth.
     * Outermost tier (index 0) is largest, innermost is smallest.
     */
    public void calculateTierRadii() {
        int depth = hex.getMaxDepth();
        if (depth <= 0) {
            depth = 1;
        }
        tierRadii = new float[depth];
        for (int i = 0; i < depth; i++) {
            // Outermost (i=0) is largest
            tierRadii[i] = BASE_TIER_RADIUS + (TIER_RADIUS_STEP * (depth - 1 - i));
        }
    }

    /**
     * @return Array of tier radii, index 0 = outermost (largest)
     */
    public float[] getTierRadii() {
        if (tierRadii == null) {
            calculateTierRadii();
        }
        return tierRadii;
    }

    /**
     * Get the radius for a specific tier level.
     * @param tier The tier level (0 = outermost)
     * @return The radius for that tier
     */
    public float getTierRadius(int tier) {
        if (tierRadii == null) {
            calculateTierRadii();
        }
        if (tier < 0 || tier >= tierRadii.length) {
            return BASE_TIER_RADIUS;
        }
        return tierRadii[tier];
    }

    /**
     * Get the number of tiers (depth) in this hex.
     * @return Number of tiers
     */
    public int getTierCount() {
        return hex.getMaxDepth();
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
     * @return The glyph this entity represents
     */
    public Hex getHex() {
        return hex;
    }

    /**
     * @return The player who owns this orbital glyph
     */
    @Override
    public Ref<EntityStore> getOwnerPlayer() {
        return ownerPlayer;
    }

    /**
     * @return The entity reference (if spawned)
     */
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

    /**
     * @return Current orbit angle in radians
     */
    @Override
    public float getOrbitAngle() {
        return orbitAngle;
    }

    @Override
    public void setOrbitAngle(float angle) {
        this.orbitAngle = angle;
    }

    /**
     * @return Current orbit speed
     */
    @Override
    public float getOrbitSpeed() {
        return orbitSpeed;
    }

    /**
     * @return Orbital radius
     */
    @Override
    public float getOrbitalRadius() {
        return orbitalRadius;
    }

    @Override
    public float getHeight() {
        return height;
    }

    /**
     * @return true if this hex is being hovered
     */
    @Override
    public boolean isHovered() {
        return isHovered;
    }

    /**
     * Set hover state.
     */
    @Override
    public void setHovered(boolean hovered) {
        this.isHovered = hovered;
    }

    /**
     * @return true if this hex is being dragged
     */
    @Override
    public boolean isDragging() {
        return isDragging;
    }

    /**
     * Set dragging state.
     */
    @Override
    public void setDragging(boolean dragging) {
        this.isDragging = dragging;
    }

    /**
     * @return true if this hex is available for use
     */
    @Override
    public boolean isAvailable() {
        return isAvailable;
    }

    /**
     * Set availability (greyed out if incompatible).
     */
    @Override
    public void setAvailable(boolean available) {
        this.isAvailable = available;
    }

    @Override
    public boolean isExcludedFromOrbit() {
        return excludedFromOrbit;
    }

    @Override
    public void setExcludedFromOrbit(boolean excluded) {
        this.excludedFromOrbit = excluded;
    }

    /**
     * Get the current visual rotation angle.
     *
     * @return Rotation in radians
     */
    public float getVisualRotation() {
        return visualRotation;
    }

    /**
     * Calculate the world position of this hex.
     *
     * @param playerPosition The player's current position
     * @return World position of the hex
     */
    @Override
    public Vector3d calculatePosition(Vector3d playerPosition) {
        return HexMathUtil.calculateOrbitalPosition(playerPosition, orbitalRadius, orbitAngle, height);
    }

    /**
     * Spawn this orbital hex entity in the world using CommandBuffer.
     * Must be used when calling from within a system tick.
     *
     * @param commandBuffer  The command buffer for deferred entity operations
     * @param playerPosition The player's position
     */
    @Override
    public void spawn(CommandBuffer<EntityStore> commandBuffer, Vector3d playerPosition) {
        spawn(commandBuffer, playerPosition, null);
    }

    /**
     * Spawn this orbital hex entity in the world using CommandBuffer.
     * Always uses blockymodel-based rendering (particle rendering is deprecated).
     * Loads all glyphs within the hex as child entities with their relative offsets and scales.
     * Must be used when calling from within a system tick.
     *
     * @param commandBuffer  The command buffer for deferred entity operations
     * @param playerPosition The player's position
     * @param ownerPlayerId  The owner player's UUID (for component-based system)
     */
    public void spawn(CommandBuffer<EntityStore> commandBuffer, Vector3d playerPosition, UUID ownerPlayerId) {
        if (entityRef != null) {
            LOGGER.atWarning().log("Hex entity '%s' already spawned", hex.getId());
            return;
        }

        Vector3d position = calculatePosition(playerPosition);

        // Create root entity holder for the hex
        Holder<EntityStore> rootHolder = EntityStore.REGISTRY.newHolder();

        // Add UUID component to root
        rootHolder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(UUID.randomUUID()));

        // Add transform component with position at root
        TransformComponent rootTransform = new TransformComponent(position, new Vector3f(0, 0, 0));
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
                Vector3d glyphPosition = new Vector3d(position.x + offsetX, position.y + offsetY, position.z + offsetZ);
                
                // Note: GlyphVisual.getScale() is stored but model scaling would need to be handled
                // through a custom component or baked into the asset if the Model API supports it.
                // For now, the scale is preserved in the GlyphVisual for future renderer updates.
                
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
                
                // Spawn the glyph child entity
                commandBuffer.addEntity(glyphHolder, AddReason.SPAWN);
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
                
                TransformComponent glyphTransform = new TransformComponent(position, new Vector3f(0, 0, 0));
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
                
                commandBuffer.addEntity(glyphHolder, AddReason.SPAWN);
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

        // Add orbital hex component if component type is registered
        if (GlyphComponent.getComponentType() != null && ownerPlayerId != null) {
            GlyphComponent orbitalComp = new GlyphComponent(
                    hex.getId(),
                    ownerPlayerId,
                    orbitAngle,
                    orbitSpeed,
                    orbitalRadius,
                    height);
            rootHolder.addComponent(GlyphComponent.getComponentType(), orbitalComp);
        }

        // Add root entity via CommandBuffer (deferred execution after tick completes)
        entityRef = commandBuffer.addEntity(rootHolder, AddReason.SPAWN);

        LOGGER.atInfo().log("Spawned hex entity '%s' with %d glyphs at (%.1f, %.1f, %.1f)",
                hex.getId(), glyphCount, position.x, position.y, position.z);
    }

    /**
     * Despawn this orbital hex entity using CommandBuffer.
     * Must be used when calling from within a system tick.
     *
     * @param commandBuffer The command buffer for deferred entity operations
     */
    @Override
    public void despawn(CommandBuffer<EntityStore> commandBuffer) {
        if (entityRef != null) {
            commandBuffer.removeEntity(entityRef, RemoveReason.REMOVE);
            LOGGER.atInfo().log("Despawned hex entity '%s'", hex.getId());
            entityRef = null;
        }
    }

    /**
     * Update the entity's position in the world.
     *
     * @param store          The entity store
     * @param playerPosition The player's current position
     */
    @Override
    public void updateWorldPosition(Store<EntityStore> store, Vector3d playerPosition) {
        if (entityRef == null) {
            return;
        }

        TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform != null) {
            Vector3d newPosition = calculatePosition(playerPosition);
            transform.setPosition(newPosition);
        }
    }

    /**
     * Update the entity's position directly to a specific location (for dragging).
     *
     * @param store    The entity store
     * @param position The target position
     */
    @Override
    public void updateWorldPositionDirect(Store<EntityStore> store, Vector3d position) {
        if (entityRef == null) {
            return;
        }

        TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform != null) {
            transform.setPosition(position);
        }
    }

    /**
     * Update the hover highlight visual.
     *
     * @param store The entity store
     */
    @Override
    public void updateHoverVisual(Store<EntityStore> store) {
        if (entityRef == null) {
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
                if (isHovered) {
                    colorLight.radius = (byte) Math.min(255, colorLight.radius + 4);
                }

                light.setColorLight(colorLight);
            }
        }
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
