package com.riprod.hexcode.entity;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.ColorLight;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.DynamicLight;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.config.HexcodeConfig;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphVisual;
import com.riprod.hexcode.util.HexMathUtil;

import java.util.UUID;

/**
 * Represents a floating glyph entity in the orbital ring around a player.
 *
 * When in glyph mode, each glyph in the player's loadout appears as
 * an OrbitalGlyphEntity that slowly orbits around the player.
 */
public class OrbitalGlyphEntity {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Glyph glyph;
    private final Ref<EntityStore> ownerPlayer;
    private Ref<EntityStore> entityRef;

    private float orbitAngle;
    private float orbitSpeed;
    private float orbitalRadius;
    private float height;

    private boolean isHovered;
    private boolean isDragging;
    private boolean isAvailable;

    public OrbitalGlyphEntity(Glyph glyph, Ref<EntityStore> ownerPlayer, float initialAngle) {
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
    }

    /**
     * @return The glyph this entity represents
     */
    public Glyph getGlyph() {
        return glyph;
    }

    /**
     * @return The player who owns this orbital glyph
     */
    public Ref<EntityStore> getOwnerPlayer() {
        return ownerPlayer;
    }

    /**
     * @return The entity reference (if spawned)
     */
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
    public float getOrbitAngle() {
        return orbitAngle;
    }

    /**
     * @return Current orbit speed
     */
    public float getOrbitSpeed() {
        return orbitSpeed;
    }

    /**
     * @return Orbital radius
     */
    public float getOrbitalRadius() {
        return orbitalRadius;
    }

    /**
     * @return true if this glyph is being hovered
     */
    public boolean isHovered() {
        return isHovered;
    }

    /**
     * Set hover state.
     */
    public void setHovered(boolean hovered) {
        this.isHovered = hovered;
    }

    /**
     * @return true if this glyph is being dragged
     */
    public boolean isDragging() {
        return isDragging;
    }

    /**
     * Set dragging state.
     */
    public void setDragging(boolean dragging) {
        this.isDragging = dragging;
    }

    /**
     * @return true if this glyph is available for use
     */
    public boolean isAvailable() {
        return isAvailable;
    }

    /**
     * Set availability (greyed out if incompatible).
     */
    public void setAvailable(boolean available) {
        this.isAvailable = available;
    }

    /**
     * Update the orbital position based on delta time.
     *
     * @param dt Delta time in seconds
     */
    public void update(float dt) {
        if (!isDragging) {
            orbitAngle += orbitSpeed * dt;
            if (orbitAngle > Math.PI * 2) {
                orbitAngle -= Math.PI * 2;
            }
        }
    }

    /**
     * Calculate the world position of this glyph.
     *
     * @param playerPosition The player's current position
     * @return World position of the glyph
     */
    public Vector3d calculatePosition(Vector3d playerPosition) {
        return HexMathUtil.calculateOrbitalPosition(playerPosition, orbitalRadius, orbitAngle, height);
    }

    /**
     * Spawn this orbital glyph entity in the world.
     *
     * @param store The entity store
     * @param playerPosition The player's position
     */
    public void spawn(Store<EntityStore> store, Vector3d playerPosition) {
        spawn(store, playerPosition, null);
    }

    /**
     * Spawn this orbital glyph entity in the world.
     *
     * @param store The entity store
     * @param playerPosition The player's position
     * @param ownerPlayerId The owner player's UUID (for component-based system)
     */
    public void spawn(Store<EntityStore> store, Vector3d playerPosition, UUID ownerPlayerId) {
        if (entityRef != null) {
            LOGGER.atWarning().log("Orbital glyph '%s' already spawned", glyph.getDisplayName());
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

        // Load model from glyph visual
        GlyphVisual visual = glyph.getVisual();
        String modelId = visual.getModelId();
        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(modelId);

        if (modelAsset != null) {
            Model model = Model.createUnitScaleModel(modelAsset);
            holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
        } else {
            LOGGER.atWarning().log("Could not load model '%s' for glyph '%s'", modelId, glyph.getDisplayName());
        }

        // Add dynamic light based on glyph color
        ColorLight colorLight = createColorLightFromGlyph(visual);
        DynamicLight dynamicLight = new DynamicLight(colorLight);
        holder.addComponent(DynamicLight.getComponentType(), dynamicLight);

        // Add orbital glyph component if component type is registered
        if (OrbitalGlyphComponent.getComponentType() != null && ownerPlayerId != null) {
            OrbitalGlyphComponent orbitalComp = new OrbitalGlyphComponent(
                    glyph.getId(),
                    ownerPlayerId,
                    orbitAngle,
                    orbitSpeed,
                    orbitalRadius,
                    height
            );
            holder.addComponent(OrbitalGlyphComponent.getComponentType(), orbitalComp);
        }

        // Add entity to store
        entityRef = store.addEntity(holder, AddReason.SPAWN);

        LOGGER.atInfo().log("Spawned orbital glyph '%s' at (%.1f, %.1f, %.1f)",
                glyph.getDisplayName(), position.x, position.y, position.z);
    }

    /**
     * Despawn this orbital glyph entity.
     *
     * @param store The entity store
     */
    public void despawn(Store<EntityStore> store) {
        if (entityRef != null) {
            store.removeEntity(entityRef, RemoveReason.REMOVE);
            LOGGER.atInfo().log("Despawned orbital glyph '%s'", glyph.getDisplayName());
            entityRef = null;
        }
    }

    /**
     * Update the entity's position in the world.
     *
     * @param store The entity store
     * @param playerPosition The player's current position
     */
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
     * Update the hover highlight visual.
     *
     * @param store The entity store
     */
    public void updateHoverVisual(Store<EntityStore> store) {
        if (entityRef == null) {
            return;
        }

        DynamicLight light = store.getComponent(entityRef, DynamicLight.getComponentType());
        if (light != null) {
            GlyphVisual visual = glyph.getVisual();
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
