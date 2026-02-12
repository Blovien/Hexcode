package com.riprod.hexcode.builtin.glyphs.grow;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class GrowEffect {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private GrowEffect() {}

    public static void applySensory(Vector3d position, ComponentAccessor<EntityStore> accessor) {
        // todo: needs chunkstore access to modify farmingblock growth stage
        LOGGER.atInfo().log("grow: sensory-only at (%.1f, %.1f, %.1f)", position.x, position.y, position.z);
        ParticleUtil.spawnParticleEffect("Hexcode_Grow_Effect", position, accessor);
    }
}
