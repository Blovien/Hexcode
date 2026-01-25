package com.riprod.hexcode.entity;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.mode.GlyphMode;
import com.riprod.hexcode.mode.GlyphModeManager;
import com.riprod.hexcode.util.HexMathUtil;
import com.riprod.hexcode.util.RaycastUtil;

import java.util.UUID;

/**
 * System that updates orbital glyph positions each tick.
 *
 * <p>This system runs every tick and updates the position of orbital glyph entities
 * based on their orbital parameters and the owner player's position.
 *
 * <p>Performance optimizations:
 * <ul>
 *   <li>Component types cached to avoid repeated lookups</li>
 *   <li>Reusable Vector3d for drag position calculation (no allocation per tick)</li>
 *   <li>Efficient ECS query - only processes entities with OrbitalGlyphComponent</li>
 * </ul>
 */
public class OrbitalGlyphSystem extends EntityTickingSystem<EntityStore> {
    /** Distance from player eye to dragged glyph */
    private static final float DRAG_DISTANCE = 2.5f;

    private final ComponentType<EntityStore, OrbitalGlyphComponent> orbitalGlyphComponentType;
    // TransformComponent type is retrieved lazily to avoid null during plugin setup
    private ComponentType<EntityStore, TransformComponent> transformComponentType;

    // Reusable vector for drag position calculations (avoids allocation per tick)
    private final Vector3d dragPositionBuffer = new Vector3d(0, 0, 0);

    public OrbitalGlyphSystem(
            ComponentType<EntityStore, OrbitalGlyphComponent> orbitalGlyphComponentType) {
        this.orbitalGlyphComponentType = orbitalGlyphComponentType;
        this.transformComponentType = null; // Will be fetched lazily
    }

    @Override
    public Query<EntityStore> getQuery() {
        return orbitalGlyphComponentType;
    }

    /**
     * Get the TransformComponent type, fetching it lazily if needed.
     */
    private ComponentType<EntityStore, TransformComponent> getTransformType() {
        if (transformComponentType == null) {
            transformComponentType = TransformComponent.getComponentType();
        }
        return transformComponentType;
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> archetypeChunk,
                     Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        ComponentType<EntityStore, TransformComponent> transformType = getTransformType();
        if (transformType == null) {
            return; // TransformComponent not yet registered
        }

        OrbitalGlyphComponent orbitalComp = archetypeChunk.getComponent(index, orbitalGlyphComponentType);
        TransformComponent transform = archetypeChunk.getComponent(index, transformType);

        if (orbitalComp == null || transform == null) {
            return;
        }

        // Get owner player session
        UUID ownerPlayerId = orbitalComp.getOwnerPlayerId();
        GlyphModeManager manager = GlyphModeManager.getInstance();
        GlyphMode mode = manager.getSession(ownerPlayerId);

        if (mode == null || !mode.isActive()) {
            // Player is no longer in glyph mode - entity should be despawned
            return;
        }

        // Get player reference
        Ref<EntityStore> playerRef = mode.getPlayer();
        if (playerRef == null) {
            return;
        }

        // Handle dragged glyph - follow player's gaze
        if (orbitalComp.isDragging()) {
            updateDragPosition(store, playerRef, transform);
            return;
        }

        // Normal orbital movement - update angle
        orbitalComp.updateOrbit(dt);

        // Get player position
        TransformComponent playerTransform = store.getComponent(playerRef, transformType);
        if (playerTransform == null) {
            return;
        }

        Vector3d playerPosition = playerTransform.getPosition();

        // Calculate new orbital position
        Vector3d newPosition = HexMathUtil.calculateOrbitalPosition(
                playerPosition,
                orbitalComp.getOrbitalRadius(),
                orbitalComp.getOrbitAngle(),
                orbitalComp.getHeight()
        );

        // Update entity position
        transform.setPosition(newPosition);
    }

    /**
     * Update position of a dragged glyph to follow player's gaze.
     *
     * <p>Uses RaycastUtil's no-alloc method to avoid creating Vector3d each tick.
     *
     * @param store The entity store
     * @param playerRef The player entity reference
     * @param glyphTransform The glyph's transform to update
     */
    private void updateDragPosition(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                     TransformComponent glyphTransform) {
        if (RaycastUtil.calculateDragPositionNoAlloc(store, playerRef, DRAG_DISTANCE, dragPositionBuffer)) {
            glyphTransform.setPosition(dragPositionBuffer);
        }
    }
}
