package com.riprod.hexcode.casting.styles;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.glyph.GlyphVisual;

/**
 * Common interface for orbital elements (Glyphs and Hexes) that can orbit around a player.
 *
 * This interface provides a unified way for the style system to manage both single glyphs
 * and composed hexes in the orbital ring.
 */
public interface OrbitalElement {

    /**
     * @return Unique identifier for this element (glyph ID or hex ID)
     */
    String getId();

    /**
     * @return true if this element is a saved/composed hex, false if single glyph
     */
    boolean isSavedHex();

    /**
     * @return The primary visual properties for this element
     */
    GlyphVisual getVisual();

    /**
     * @return The spawned entity reference, or null if not spawned
     */
    Ref<EntityStore> getEntityRef();

    /**
     * @return The player who owns this orbital element
     */
    Ref<EntityStore> getOwnerPlayer();

    // --- Orbital Parameters ---

    /**
     * @return Current orbit angle in radians
     */
    float getOrbitAngle();

    /**
     * Set the orbit angle.
     * @param angle Angle in radians
     */
    void setOrbitAngle(float angle);

    /**
     * @return Orbital rotation speed in radians per second
     */
    float getOrbitSpeed();

    /**
     * @return Orbital radius from center
     */
    float getOrbitalRadius();

    /**
     * @return Height offset from player position
     */
    float getHeight();

    // --- Position Control ---

    /**
     * Calculate the world position based on player position and orbital parameters.
     * @param playerPosition The player's current position
     * @return The calculated world position
     */
    Vector3d calculatePosition(Vector3d playerPosition);

    /**
     * Update the entity's world position.
     * @param store The entity store
     * @param playerPosition The player's current position
     */
    void updateWorldPosition(Store<EntityStore> store, Vector3d playerPosition);

    /**
     * Update the entity's world position to a specific location (for dragging).
     * @param store The entity store
     * @param position The target position
     */
    void updateWorldPositionDirect(Store<EntityStore> store, Vector3d position);

    // --- State Flags ---

    /**
     * @return true if this element is currently being dragged
     */
    boolean isDragging();

    /**
     * Set the dragging state.
     * @param dragging true if being dragged
     */
    void setDragging(boolean dragging);

    /**
     * @return true if this element is excluded from orbital rotation updates
     */
    boolean isExcludedFromOrbit();

    /**
     * Set whether this element should be excluded from orbital updates.
     * Elements are typically excluded while being dragged or after being placed.
     * @param excluded true to exclude from orbital updates
     */
    void setExcludedFromOrbit(boolean excluded);

    /**
     * @return true if this element is currently being hovered by the player
     */
    boolean isHovered();

    /**
     * Set the hover state.
     * @param hovered true if being hovered
     */
    void setHovered(boolean hovered);

    /**
     * @return true if this element is available for use (not greyed out)
     */
    boolean isAvailable();

    /**
     * Set availability state.
     * @param available true if available for interaction
     */
    void setAvailable(boolean available);

    // --- Lifecycle ---

    /**
     * Spawn this element into the world.
     * @param commandBuffer The command buffer for deferred operations
     * @param playerPosition The player's position for initial placement
     */
    void spawn(CommandBuffer<EntityStore> commandBuffer, Vector3d playerPosition);

    /**
     * Despawn this element from the world.
     * @param commandBuffer The command buffer for deferred operations
     */
    void despawn(CommandBuffer<EntityStore> commandBuffer);

    /**
     * Update visual state (hover highlights, etc).
     * @param store The entity store
     */
    void updateHoverVisual(Store<EntityStore> store);
}
