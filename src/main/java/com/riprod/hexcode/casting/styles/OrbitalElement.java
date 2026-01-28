package com.riprod.hexcode.casting.styles;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.glyph.GlyphVisual;
import com.riprod.hexcode.math.GlyphRotation;

/**
 * Common interface for orbital elements (Glyphs and Hexes) that appear around a player.
 *
 * <p>Elements are positioned using rotation-based positioning:
 * <ul>
 *   <li>Each element stores a {@link GlyphRotation} (pitch/yaw from player's eyes)</li>
 *   <li>World position is calculated as: eyePosition + direction(pitch, yaw) * distance</li>
 *   <li>Selection uses angular distance comparison (5-degree tolerance for glyphs)</li>
 * </ul>
 *
 * <p>When dragged, elements follow the player's look direction.
 * Upon drag release, the new rotation is captured from the current look direction.
 */
public interface OrbitalElement {

    /**
     * Lifecycle states for orbital elements.
     *
     * State transitions:
     * - NOT_SPAWNED -> IDLE (on spawn)
     * - IDLE <-> HOVERING (on hover enter/exit)
     * - IDLE/HOVERING -> DRAGGING (on drag start)
     * - DRAGGING -> IDLE (on drag end, element kept)
     * - DRAGGING -> CONSUMED (on drag end, element used)
     * - ANY -> CONSUMED (on despawn)
     * - ANY -> ERROR (on invalid state detection)
     * - ERROR -> CONSUMED (on force cleanup)
     */
    enum ElementState {
        NOT_SPAWNED,  // Before spawn() called
        IDLE,         // Normal orbital state
        HOVERING,     // Mouse over element
        DRAGGING,     // Being dragged
        CONSUMED,     // Despawned/used (terminal state)
        ERROR         // Recovery state for invalid conditions
    }

    /**
     * Get the current lifecycle state of this element.
     * @return The current state
     */
    ElementState getState();

    /**
     * Attempt to transition to a new state.
     * Invalid transitions are logged and rejected.
     *
     * @param newState The target state
     * @return true if transition succeeded, false if invalid
     */
    boolean transitionTo(ElementState newState);

    /**
     * Check if a state transition is valid.
     *
     * @param from The current state
     * @param to The target state
     * @return true if the transition is allowed
     */
    static boolean isValidTransition(ElementState from, ElementState to) {
        if (from == to) return true; // No-op is always valid

        switch (from) {
            case NOT_SPAWNED:
                return to == ElementState.IDLE;
            case IDLE:
                return to == ElementState.HOVERING
                    || to == ElementState.DRAGGING
                    || to == ElementState.CONSUMED
                    || to == ElementState.ERROR;
            case HOVERING:
                return to == ElementState.IDLE
                    || to == ElementState.DRAGGING
                    || to == ElementState.CONSUMED
                    || to == ElementState.ERROR;
            case DRAGGING:
                return to == ElementState.IDLE
                    || to == ElementState.CONSUMED
                    || to == ElementState.ERROR;
            case ERROR:
                return to == ElementState.CONSUMED;
            case CONSUMED:
                return false; // Terminal state - no transitions allowed
            default:
                return false;
        }
    }

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

    // --- Rotation-Based Positioning ---

    /**
     * Get the rotation (pitch/yaw) for this element.
     * This rotation defines the direction from player's eyes to the element.
     *
     * @return The rotation from player's eyes
     */
    GlyphRotation getRotation();

    /**
     * Set the rotation (pitch/yaw) for this element.
     *
     * @param rotation The new rotation from player's eyes
     */
    void setRotation(GlyphRotation rotation);

    /**
     * Get the selection tolerance for this element in degrees.
     * Single glyphs use 5 degrees, composed hexes use 7 degrees.
     *
     * @return The angular tolerance in degrees
     */
    float getSelectionTolerance();

    /**
     * Update the element's world position based on player position and rotation.
     * Also rotates the element to face the player.
     * This is a no-op if the element is currently being dragged.
     *
     * @param store          The entity store
     * @param playerPosition The player's current position (feet)
     */
    void updatePositionFromPlayer(Store<EntityStore> store, Vector3d playerPosition);

    /**
     * Capture the current look rotation as this element's new rotation.
     * Called when a drag operation ends to lock the element at its new position.
     *
     * @param store          The entity store
     * @param lookRotation   The player's current look rotation
     */
    void captureRotationFromLook(Store<EntityStore> store, GlyphRotation lookRotation);

    // --- Deprecated Offset Methods (for migration) ---

    /**
     * @deprecated Use {@link #getRotation()} instead. Rotation-based positioning replaces offsets.
     */
    @Deprecated
    default Vector3d getPlayerOffset() {
        // Convert rotation to offset for backward compatibility
        GlyphRotation rotation = getRotation();
        if (rotation == null) {
            return new Vector3d(0, 0, 0);
        }
        Vector3d direction = rotation.toDirection();
        float distance = GlyphRotation.DEFAULT_DISTANCE;
        return new Vector3d(
            direction.x * distance,
            GlyphRotation.EYE_HEIGHT + direction.y * distance,
            direction.z * distance
        );
    }

    /**
     * @deprecated Use {@link #setRotation(GlyphRotation)} instead. Rotation-based positioning replaces offsets.
     */
    @Deprecated
    default void setPlayerOffset(Vector3d offset) {
        // Convert offset to rotation for backward compatibility
        // Offset is relative to player feet, so we need to account for eye height
        Vector3d direction = new Vector3d(
            offset.x,
            offset.y - GlyphRotation.EYE_HEIGHT,
            offset.z
        );
        setRotation(GlyphRotation.fromDirection(direction));
    }

    /**
     * @deprecated Use {@link #captureRotationFromLook(Store, GlyphRotation)} instead.
     */
    @Deprecated
    default void captureOffsetFromPlayer(Store<EntityStore> store, Vector3d playerPosition) {
        // No-op by default; implementations should use captureRotationFromLook
    }

    // --- Direct Position Control ---

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

    /**
     * @return true if this element is waiting for its entity to spawn
     *         (entityRef was set via CommandBuffer but not yet valid)
     */
    boolean isPendingSpawn();

    /**
     * Clear the pending spawn flag.
     * Called when the entity becomes valid after deferred spawn.
     */
    void clearPendingSpawn();

    // --- Lifecycle ---

    /**
     * Spawn this element into the world at a specific position.
     * @param commandBuffer The command buffer for deferred operations
     * @param spawnPosition The world position where element should spawn
     * @param ownerPlayerId The owner player's UUID
     */
    void spawn(CommandBuffer<EntityStore> commandBuffer, Vector3d spawnPosition, java.util.UUID ownerPlayerId);

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
