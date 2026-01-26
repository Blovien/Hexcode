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
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.casting.styles.OrbitalElement;
import com.riprod.hexcode.config.HexcodeConfig;
import com.riprod.hexcode.data.GlyphInstance;
import com.riprod.hexcode.glyph.GlyphVisual;
import com.riprod.hexcode.util.HexMathUtil;

/**
 * Represents a floating glyph entity in the orbital ring around a player.
 *
 * When in glyph mode, each glyph in the player's loadout appears as
 * an OrbitalGlyphEntity that slowly orbits around the player.
 */
public class GlyphEntity implements OrbitalElement {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final float BOUNDING_BOX_SIZE = 0.5f;

    private final GlyphInstance glyph;
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

    public GlyphEntity(GlyphInstance glyph, Ref<EntityStore> ownerPlayer, float initialAngle) {
        this.glyph = glyph;
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
     * @return true if this glyph is being hovered
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
     * @return true if this glyph is being dragged
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
     * @return true if this glyph is available for use
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
     * Calculate the world position of this glyph.
     *
     * @param playerPosition The player's current position
     * @return World position of the glyph
     */
    @Override
    public Vector3d calculatePosition(Vector3d playerPosition) {
        return HexMathUtil.calculateOrbitalPosition(playerPosition, orbitalRadius, orbitAngle, height);
    }

    /**
     * Spawn this orbital glyph entity in the world using CommandBuffer.
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
     * Spawn this orbital glyph entity in the world using CommandBuffer.
     * Always uses blockymodel-based rendering (particle rendering is deprecated).
     * Must be used when calling from within a system tick.
     *
     * @param commandBuffer  The command buffer for deferred entity operations
     * @param playerPosition The player's position
     * @param ownerPlayerId  The owner player's UUID (for component-based system)
     */
    public void spawn(CommandBuffer<EntityStore> commandBuffer, Vector3d playerPosition, UUID ownerPlayerId) {
        if (entityRef != null) {
            LOGGER.atWarning().log("Orbital glyph '%s' already spawned", glyph.getGlyph().getDisplayName());
            return;
        }

        Vector3d position = calculatePosition(playerPosition);

        // Create entity holder
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        // Add UUID component
        holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(UUID.randomUUID()));

        // Add transform component with position
        TransformComponent transform = new TransformComponent(position, new Vector3f(0, 0, 0));
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

        holder.addComponent(NetworkId.getComponentType(), new NetworkId(commandBuffer.getExternalData().takeNextNetworkId()));

        // Add BoundingBox for hit detection
        Box box = new Box(
                -BOUNDING_BOX_SIZE, -BOUNDING_BOX_SIZE, -BOUNDING_BOX_SIZE,
                BOUNDING_BOX_SIZE, BOUNDING_BOX_SIZE, BOUNDING_BOX_SIZE);
        holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(box));

        // Add orbital glyph component if component type is registered
        if (GlyphComponent.getComponentType() != null && ownerPlayerId != null) {
            GlyphComponent orbitalComp = new GlyphComponent(
                    glyph.getGlyph().getId(),
                    ownerPlayerId,
                    orbitAngle,
                    orbitSpeed,
                    orbitalRadius,
                    height);
            holder.addComponent(GlyphComponent.getComponentType(), orbitalComp);
        }

        // Add entity via CommandBuffer (deferred execution after tick completes)
        entityRef = commandBuffer.addEntity(holder, AddReason.SPAWN);

        LOGGER.atInfo().log("Spawned orbital glyph '%s' at (%.1f, %.1f, %.1f) with model '%s'",
                glyph.getGlyph().getDisplayName(), position.x, position.y, position.z, modelId);
    }

    /**
     * Despawn this orbital glyph entity using CommandBuffer.
     * Must be used when calling from within a system tick.
     *
     * @param commandBuffer The command buffer for deferred entity operations
     */
    @Override
    public void despawn(CommandBuffer<EntityStore> commandBuffer) {
        if (entityRef != null) {
            commandBuffer.removeEntity(entityRef, RemoveReason.REMOVE);
            LOGGER.atInfo().log("Despawned orbital glyph '%s'", glyph.getGlyph().getDisplayName());
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
            GlyphVisual visual = glyph.getGlyph().getVisual();
            ColorLight colorLight = createColorLightFromGlyph(visual);

            // Increase intensity when hovered
            if (isHovered) {
                colorLight.radius = (byte) Math.min(255, colorLight.radius + 4);
            }

            light.setColorLight(colorLight);
        }
    }

    /**
     * Create a ColorLight from glyph visual properties.
     */
    private ColorLight createColorLightFromGlyph(GlyphVisual visual) {
        int color = visual.getColor();
        float intensity = visual.getGlowIntensity();

        // Extract RGB from color int
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        // Create ColorLight
        ColorLight colorLight = new ColorLight();
        colorLight.radius = (byte) Math.min(255, (int) intensity);
        colorLight.red = (byte) red;
        colorLight.green = (byte) green;
        colorLight.blue = (byte) blue;

        return colorLight;
    }
}
