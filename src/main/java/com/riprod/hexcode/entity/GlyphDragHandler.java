package com.riprod.hexcode.entity;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles click-drag movement for selected glyph entities.
 *
 * This handler tracks dragging state per player and provides methods to:
 * - Start dragging a selected glyph
 * - Update the glyph position based on player look direction
 * - End dragging and finalize position
 */
public class GlyphDragHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static GlyphDragHandler instance;

    // Default distance from player to place glyph while dragging
    private static final float DEFAULT_DRAG_DISTANCE = 3.0f;

    // Drag state per player
    private final Map<UUID, DragState> playerDragStates;

    /**
     * Internal class to track drag state for a player.
     */
    private static class DragState {
        final UUID glyphEntityUUID;
        final Vector3d startPosition;
        float dragDistance;

        DragState(UUID glyphEntityUUID, Vector3d startPosition, float dragDistance) {
            this.glyphEntityUUID = glyphEntityUUID;
            this.startPosition = new Vector3d(startPosition);
            this.dragDistance = dragDistance;
        }
    }

    private GlyphDragHandler() {
        this.playerDragStates = new ConcurrentHashMap<>();
    }

    /**
     * Get the singleton instance.
     *
     * @return The GlyphDragHandler instance
     */
    public static synchronized GlyphDragHandler getInstance() {
        if (instance == null) {
            instance = new GlyphDragHandler();
        }
        return instance;
    }

    /**
     * Start dragging the currently selected glyph for a player.
     *
     * @param playerId The player's UUID
     * @param store The entity store
     * @return true if drag started successfully
     */
    public boolean startDrag(UUID playerId, Store<EntityStore> store) {
        return startDrag(playerId, store, DEFAULT_DRAG_DISTANCE);
    }

    /**
     * Start dragging the currently selected glyph for a player.
     *
     * @param playerId The player's UUID
     * @param store The entity store
     * @param dragDistance Distance from player to place glyph
     * @return true if drag started successfully
     */
    public boolean startDrag(UUID playerId, Store<EntityStore> store, float dragDistance) {
        if (isDragging(playerId)) {
            LOGGER.atWarning().log("Player %s is already dragging", playerId);
            return false;
        }

        GlyphEntityManager manager = GlyphEntityManager.getInstance();
        SelectableGlyphEntity selectedEntity = manager.getSelectedGlyph(playerId);

        if (selectedEntity == null || !selectedEntity.isSpawned()) {
            LOGGER.atFine().log("Player %s has no selected glyph to drag", playerId);
            return false;
        }

        // Get start position
        Vector3d startPos = selectedEntity.getPosition(store);
        if (startPos == null) {
            return false;
        }

        // Create drag state
        DragState dragState = new DragState(
            selectedEntity.getEntityUUID(),
            startPos,
            dragDistance
        );
        playerDragStates.put(playerId, dragState);

        LOGGER.atInfo().log("Player %s started dragging glyph %s",
            playerId, selectedEntity.getGlyphId());

        return true;
    }

    /**
     * Update the position of the glyph being dragged based on player position and look direction.
     *
     * @param playerId The player's UUID
     * @param playerRef The player entity reference
     * @param store The entity store
     */
    public void updateDrag(UUID playerId, Ref<EntityStore> playerRef, Store<EntityStore> store) {
        DragState dragState = playerDragStates.get(playerId);
        if (dragState == null) {
            return;
        }

        GlyphEntityManager manager = GlyphEntityManager.getInstance();
        SelectableGlyphEntity glyphEntity = manager.getGlyph(dragState.glyphEntityUUID);

        if (glyphEntity == null || !glyphEntity.isSpawned()) {
            // Entity no longer valid - end drag
            endDrag(playerId);
            return;
        }

        // Get player transform
        TransformComponent playerTransform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (playerTransform == null) {
            return;
        }

        // Calculate new position based on player's eye position and look direction
        Vector3d eyePos = getPlayerEyePosition(playerTransform);
        Vector3d lookDir = getPlayerLookDirection(playerTransform);

        // Position the glyph at dragDistance in front of the player
        Vector3d newPosition = new Vector3d(
            eyePos.x + lookDir.x * dragState.dragDistance,
            eyePos.y + lookDir.y * dragState.dragDistance,
            eyePos.z + lookDir.z * dragState.dragDistance
        );

        // Update glyph position
        glyphEntity.updatePosition(store, newPosition);
    }

    /**
     * End dragging for a player.
     *
     * @param playerId The player's UUID
     * @return true if drag was ended (was actually dragging)
     */
    public boolean endDrag(UUID playerId) {
        DragState dragState = playerDragStates.remove(playerId);
        if (dragState != null) {
            LOGGER.atInfo().log("Player %s ended drag", playerId);
            return true;
        }
        return false;
    }

    /**
     * Cancel dragging and return glyph to start position.
     *
     * @param playerId The player's UUID
     * @param store The entity store
     * @return true if drag was cancelled
     */
    public boolean cancelDrag(UUID playerId, Store<EntityStore> store) {
        DragState dragState = playerDragStates.remove(playerId);
        if (dragState != null) {
            // Return glyph to start position
            GlyphEntityManager manager = GlyphEntityManager.getInstance();
            SelectableGlyphEntity glyphEntity = manager.getGlyph(dragState.glyphEntityUUID);

            if (glyphEntity != null && glyphEntity.isSpawned()) {
                glyphEntity.updatePosition(store, dragState.startPosition);
            }

            LOGGER.atInfo().log("Player %s cancelled drag, glyph returned to start position", playerId);
            return true;
        }
        return false;
    }

    /**
     * Check if a player is currently dragging.
     *
     * @param playerId The player's UUID
     * @return true if dragging
     */
    public boolean isDragging(UUID playerId) {
        return playerDragStates.containsKey(playerId);
    }

    /**
     * Get the UUID of the glyph being dragged by a player.
     *
     * @param playerId The player's UUID
     * @return The glyph entity UUID, or null if not dragging
     */
    public UUID getDraggedGlyphUUID(UUID playerId) {
        DragState dragState = playerDragStates.get(playerId);
        return dragState != null ? dragState.glyphEntityUUID : null;
    }

    /**
     * Set the drag distance for a player.
     *
     * @param playerId The player's UUID
     * @param distance The new drag distance
     */
    public void setDragDistance(UUID playerId, float distance) {
        DragState dragState = playerDragStates.get(playerId);
        if (dragState != null) {
            dragState.dragDistance = distance;
        }
    }

    /**
     * Adjust drag distance by an offset (for scroll wheel adjustment).
     *
     * @param playerId The player's UUID
     * @param offset The offset to add to drag distance
     * @param minDistance Minimum allowed distance
     * @param maxDistance Maximum allowed distance
     */
    public void adjustDragDistance(UUID playerId, float offset, float minDistance, float maxDistance) {
        DragState dragState = playerDragStates.get(playerId);
        if (dragState != null) {
            dragState.dragDistance = Math.max(minDistance, Math.min(maxDistance,
                dragState.dragDistance + offset));
        }
    }

    /**
     * Get the player's eye position from their transform.
     */
    private Vector3d getPlayerEyePosition(TransformComponent transform) {
        Vector3d pos = transform.getPosition();
        // Eye height is approximately 1.62 blocks above feet
        return new Vector3d(pos.x, pos.y + 1.62, pos.z);
    }

    /**
     * Get the player's look direction from their transform.
     */
    private Vector3d getPlayerLookDirection(TransformComponent transform) {
        Vector3f rotation = transform.getRotation();

        // Convert pitch (X) and yaw (Y) to direction vector
        double pitch = Math.toRadians(rotation.x);
        double yaw = Math.toRadians(rotation.y);

        double cosP = Math.cos(pitch);
        double sinP = Math.sin(pitch);
        double cosY = Math.cos(yaw);
        double sinY = Math.sin(yaw);

        return new Vector3d(-sinY * cosP, -sinP, cosY * cosP);
    }

    /**
     * Clear all drag states (called on shutdown).
     */
    public void clearAll() {
        playerDragStates.clear();
    }
}
