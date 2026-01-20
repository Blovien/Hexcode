package com.riprod.hexcode.entity;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.mode.GlyphMode;
import com.riprod.hexcode.mode.GlyphModeManager;
import com.riprod.hexcode.util.HexMathUtil;

import java.util.UUID;

/**
 * System that updates orbital glyph positions each tick.
 *
 * This system runs every tick and updates the position of orbital glyph entities
 * based on their orbital parameters and the owner player's position.
 */
public class OrbitalGlyphSystem extends EntityTickingSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final ComponentType<EntityStore, OrbitalGlyphComponent> orbitalGlyphComponentType;
    private final ComponentType<EntityStore, TransformComponent> transformComponentType;

    public OrbitalGlyphSystem(
            ComponentType<EntityStore, OrbitalGlyphComponent> orbitalGlyphComponentType,
            ComponentType<EntityStore, TransformComponent> transformComponentType) {
        this.orbitalGlyphComponentType = orbitalGlyphComponentType;
        this.transformComponentType = transformComponentType;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return orbitalGlyphComponentType;
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> archetypeChunk,
                     Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        OrbitalGlyphComponent orbitalComp = archetypeChunk.getComponent(index, orbitalGlyphComponentType);
        TransformComponent transform = archetypeChunk.getComponent(index, transformComponentType);

        if (orbitalComp == null || transform == null) {
            return;
        }

        // Don't update if being dragged
        if (orbitalComp.isDragging()) {
            return;
        }

        // Update orbital angle
        orbitalComp.updateOrbit(dt);

        // Get owner player position
        UUID ownerPlayerId = orbitalComp.getOwnerPlayerId();
        GlyphModeManager manager = GlyphModeManager.getInstance();
        GlyphMode mode = manager.getSession(ownerPlayerId);

        if (mode == null || !mode.isActive()) {
            // Player is no longer in glyph mode - entity should be despawned
            return;
        }

        // Get player position from their entity
        Ref<EntityStore> playerRef = mode.getPlayer();
        if (playerRef == null) {
            return;
        }

        TransformComponent playerTransform = store.getComponent(playerRef, TransformComponent.getComponentType());
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
}
