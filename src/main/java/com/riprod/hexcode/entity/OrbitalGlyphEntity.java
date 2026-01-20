package com.riprod.hexcode.entity;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.config.HexcodeConfig;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.util.HexMathUtil;
import com.hypixel.hytale.math.vector.Vector3d;

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
        // TODO: Implement actual entity spawning using Hytale's ECS
        // This would create an entity with:
        // - ModelComponent (glyph visual)
        // - TransformComponent (position)
        // - DynamicLight (glyph glow)
        // - Custom OrbitalGlyphComponent

        Vector3d position = calculatePosition(playerPosition);
        LOGGER.atInfo().log("Spawning orbital glyph '%s' at (%.1f, %.1f, %.1f)",
                glyph.getDisplayName(), position.x, position.y, position.z);
    }

    /**
     * Despawn this orbital glyph entity.
     *
     * @param store The entity store
     */
    public void despawn(Store<EntityStore> store) {
        if (entityRef != null) {
            // TODO: Implement actual entity despawning
            LOGGER.atInfo().log("Despawning orbital glyph '%s'", glyph.getDisplayName());
            entityRef = null;
        }
    }
}
