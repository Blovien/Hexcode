package com.riprod.hexcode.mode;

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
import com.riprod.hexcode.entity.GlyphComponent;
import com.riprod.hexcode.util.HexMathUtil;
import com.riprod.hexcode.util.RaycastUtil;

import java.util.UUID;

/**
 * System that updates orbital glyph positions each tick.
 *
 * <p>
 * This system runs every tick and updates the position of orbital glyph
 * entities
 * based on their orbital parameters and the owner player's position.
 *
 * <p>
 * Performance optimizations:
 * <ul>
 * <li>Component types cached to avoid repeated lookups</li>
 * <li>Reusable Vector3d for drag position calculation (no allocation per
 * tick)</li>
 * <li>Efficient ECS query - only processes entities with
 * OrbitalGlyphComponent</li>
 * </ul>
 */
public class GlyphModeSystem extends EntityTickingSystem<EntityStore> {

    private final ComponentType<EntityStore, GlyphComponent> orbitalGlyphComponentType;

    public GlyphModeSystem(
            ComponentType<EntityStore, GlyphComponent> orbitalGlyphComponentType) {
        this.orbitalGlyphComponentType = orbitalGlyphComponentType;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return orbitalGlyphComponentType;
    }
    
    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> archetypeChunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        GlyphModeManager.getInstance().tickAll(dt, commandBuffer);
    }
}
