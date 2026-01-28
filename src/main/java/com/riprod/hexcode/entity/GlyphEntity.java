package com.riprod.hexcode.entity;

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
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.casting.styles.OrbitalElement;
import com.riprod.hexcode.data.GlyphInstance;
import com.riprod.hexcode.glyph.GlyphVisual;
import com.riprod.hexcode.math.GlyphRotation;

/**
 * Represents a floating glyph entity that appears around a player.
 *
 * Glyphs are positioned relative to the player and move with them automatically.
 * When dragged, they become independent and can be repositioned freely.
 * Glyphs always face towards the player.
 */
public class GlyphEntity implements OrbitalElement {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final float BOUNDING_BOX_SIZE = 0.5f;

    private final GlyphInstance glyph;
    private final Ref<EntityStore> ownerPlayer;
    private Ref<EntityStore> entityRef;

    /** Rotation (pitch/yaw) from player's eyes to this element */
    private GlyphRotation rotation;

    /** Lifecycle state machine */
    private ElementState state = ElementState.NOT_SPAWNED;

    /** Tracks if entity is pending spawn (entityRef set but not yet valid) */
    private boolean pendingSpawn = false;

    private boolean isAvailable;

    /** Visual rotation for spinning effect */
    private float visualRotation;

    /**
     * Create a new GlyphEntity with a rotation from player's eyes.
     *
     * @param glyph       The glyph instance this entity represents
     * @param ownerPlayer Reference to the player who owns this glyph
     * @param rotation    The rotation (pitch/yaw) from player's eyes
     */
    public GlyphEntity(GlyphInstance glyph, Ref<EntityStore> ownerPlayer, GlyphRotation rotation) {
        this.glyph = glyph;
        this.ownerPlayer = ownerPlayer;
        this.rotation = rotation;

        this.state = ElementState.NOT_SPAWNED;
        this.isAvailable = true;
        this.visualRotation = 0.0f;
    }

    /**
     * Create a new GlyphEntity with a player-relative offset (legacy compatibility).
     *
     * @param glyph       The glyph instance this entity represents
     * @param ownerPlayer Reference to the player who owns this glyph
     * @param initialOffset The initial offset from the player position
     * @deprecated Use {@link #GlyphEntity(GlyphInstance, Ref, GlyphRotation)} instead
     */
    @Deprecated
    public GlyphEntity(GlyphInstance glyph, Ref<EntityStore> ownerPlayer, Vector3d initialOffset) {
        this.glyph = glyph;
        this.ownerPlayer = ownerPlayer;
        // Convert offset to rotation
        // Offset is relative to player feet, rotation is from eyes
        Vector3d direction = new Vector3d(
            initialOffset.x,
            initialOffset.y - GlyphRotation.EYE_HEIGHT,
            initialOffset.z
        );
        this.rotation = GlyphRotation.fromDirection(direction);

        this.state = ElementState.NOT_SPAWNED;
        this.isAvailable = true;
        this.visualRotation = 0.0f;
    }

    // --- OrbitalElement Interface Methods ---

    @Override
    public String getId() {
        return glyph.getGlyph().getId();
    }

    @Override
    public boolean isSavedHex() {
        return false;
    }

    @Override
    public GlyphVisual getVisual() {
        return glyph.getGlyph().getVisual();
    }

    /**
     * @return The glyph this entity represents
     */
    public GlyphInstance getGlyph() {
        return glyph;
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
        return GlyphRotation.BASE_TOLERANCE;
    }

    @Override
    public void updatePositionFromPlayer(Store<EntityStore> store, Vector3d playerPosition) {
        if (isDragging()) {
            return; // Don't update position when being dragged
        }

        if (entityRef == null) {
            return;
        }

        if (!entityRef.isValid()) {
            return; // Still pending - wait for next tick
        }

        // Clear pending flag on first valid update
        if (pendingSpawn) {
            pendingSpawn = false;
            LOGGER.atInfo().log("GlyphEntity '%s' entity became valid", glyph.getGlyph().getDisplayName());
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
     * Rotate glyph to face the player.
     */
    private void updateFacingTowardsPlayer(TransformComponent transform, Vector3d playerPosition, Vector3d glyphPos) {
        // Calculate yaw angle to face player
        double dx = playerPosition.x - glyphPos.x;
        double dz = playerPosition.z - glyphPos.z;
        float yaw = (float) Math.atan2(dx, dz);
        transform.setRotation(new Vector3f(0, yaw, 0));
    }

    @Override
    public void captureRotationFromLook(Store<EntityStore> store, GlyphRotation lookRotation) {
        this.rotation = lookRotation;
        LOGGER.atInfo().log("GlyphEntity '%s' captured rotation: %s",
            glyph.getGlyph().getDisplayName(), lookRotation);
    }

    /**
     * @deprecated Use {@link #captureRotationFromLook(Store, GlyphRotation)} instead.
     */
    @Override
    @Deprecated
    public void captureOffsetFromPlayer(Store<EntityStore> store, Vector3d playerPosition) {
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform != null) {
            Vector3d currentPos = transform.getPosition();
            Vector3d eyePos = new Vector3d(playerPosition.x, playerPosition.y + GlyphRotation.EYE_HEIGHT, playerPosition.z);
            this.rotation = GlyphRotation.fromWorldPosition(eyePos, currentPos);
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
            LOGGER.atWarning().log("Invalid state transition: %s -> %s for glyph '%s'",
                state, newState, glyph.getGlyph().getDisplayName());
            return false;
        }
        ElementState oldState = this.state;
        this.state = newState;
        LOGGER.atInfo().log("Glyph '%s' state: %s -> %s",
            glyph.getGlyph().getDisplayName(), oldState, newState);
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
            LOGGER.atWarning().log("Orbital glyph '%s' cannot spawn - already in state %s",
                glyph.getGlyph().getDisplayName(), state);
            return;
        }
        if (entityRef != null) {
            LOGGER.atWarning().log("Orbital glyph '%s' already spawned", glyph.getGlyph().getDisplayName());
            return;
        }

        // Create entity holder
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        // Add UUID component
        holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(UUID.randomUUID()));

        // Add transform component with position
        TransformComponent transform = new TransformComponent(spawnPosition, new Vector3f(0, 0, 0));
        holder.addComponent(TransformComponent.getComponentType(), transform);

        // Always use blockymodel-based rendering
        GlyphVisual visual = glyph.getGlyph().getVisual();
        String modelId = visual.getModelId();

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(modelId);
        if (modelAsset != null) {
            Model model = Model.createUnitScaleModel(modelAsset);
            holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
            holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
        } else {
            LOGGER.atWarning().log("Could not load model '%s' for glyph '%s', trying default",
                    modelId, glyph.getGlyph().getDisplayName());

            // Try loading the base_glyph model as fallback
            ModelAsset fallbackAsset = ModelAsset.getAssetMap().getAsset("Base_glyph");
            if (fallbackAsset != null) {
                Model model = Model.createUnitScaleModel(fallbackAsset);
                holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
                holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
            } else {
                LOGGER.atWarning().log("Could not load fallback model for glyph '%s'",
                        glyph.getGlyph().getDisplayName());
            }
        }

        // Add dynamic light based on glyph color for glow effect
        ColorLight colorLight = createColorLightFromGlyph(visual);
        DynamicLight dynamicLight = new DynamicLight(colorLight);
        holder.addComponent(DynamicLight.getComponentType(), dynamicLight);

        holder.addComponent(NetworkId.getComponentType(),
                new NetworkId(commandBuffer.getExternalData().takeNextNetworkId()));

        // Add BoundingBox for hit detection
        Box box = new Box(
                -BOUNDING_BOX_SIZE, -BOUNDING_BOX_SIZE, -BOUNDING_BOX_SIZE,
                BOUNDING_BOX_SIZE, BOUNDING_BOX_SIZE, BOUNDING_BOX_SIZE);
        holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(box));

        // Add EntityScaleComponent for hover visual feedback
        holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(1.0f));

        // Add glyph component if component type is registered
        if (GlyphComponent.getComponentType() != null && ownerPlayerId != null) {
            GlyphComponent glyphComp = new GlyphComponent(
                    glyph.getGlyph().getId(),
                    ownerPlayerId,
                    rotation.getPitch(),
                    rotation.getYaw());
            holder.addComponent(GlyphComponent.getComponentType(), glyphComp);
        }

        // Add entity via CommandBuffer (deferred execution after tick completes)
        entityRef = commandBuffer.addEntity(holder, AddReason.SPAWN);
        this.pendingSpawn = true;  // Mark as pending until entity becomes valid

        // Transition to IDLE state
        transitionTo(ElementState.IDLE);

        LOGGER.atInfo().log("Spawned orbital glyph '%s' at (%.1f, %.1f, %.1f) with model '%s'",
                glyph.getGlyph().getDisplayName(), spawnPosition.x, spawnPosition.y, spawnPosition.z, modelId);
    }

    @Override
    public void despawn(CommandBuffer<EntityStore> commandBuffer) {
        // Already consumed - nothing to do
        if (state == ElementState.CONSUMED) {
            return;
        }

        // Transition to CONSUMED first (ensures state is clean even if remove fails)
        transitionTo(ElementState.CONSUMED);

        if (entityRef != null) {
            commandBuffer.removeEntity(entityRef, RemoveReason.REMOVE);
            LOGGER.atInfo().log("Despawned orbital glyph '%s'", glyph.getGlyph().getDisplayName());
            entityRef = null;
        }
    }

    @Override
    public void updateWorldPositionDirect(Store<EntityStore> store, Vector3d position) {
        if (entityRef == null || !entityRef.isValid()) {
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

        // Update scale: 1.1x when hovered, 1.0x when not
        EntityScaleComponent scaleComp = store.getComponent(entityRef, EntityScaleComponent.getComponentType());
        if (scaleComp != null) {
            float targetScale = isHovered() ? 1.1f : 1.0f;
            if (scaleComp.getScale() != targetScale) {
                scaleComp.setScale(targetScale);
            }
        }

        // Update light: increase intensity when hovered
        DynamicLight light = store.getComponent(entityRef, DynamicLight.getComponentType());
        if (light != null) {
            GlyphVisual visual = glyph.getGlyph().getVisual();
            ColorLight colorLight = createColorLightFromGlyph(visual);

            // Increase intensity when hovered
            if (isHovered()) {
                colorLight.radius = (byte) Math.min(255, colorLight.radius + 4);
            }

            light.setColorLight(colorLight);
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
