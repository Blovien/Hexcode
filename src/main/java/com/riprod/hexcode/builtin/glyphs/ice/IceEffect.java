package com.riprod.hexcode.builtin.glyphs.ice;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class IceEffect {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private IceEffect() {}

    public static void applySensory(Vector3d position, ComponentAccessor<EntityStore> accessor) {
        // todo: needs chunkstore access to call worldchunk.setblock() with blocktype "ice"
        LOGGER.atInfo().log("ice: sensory-only at (%.1f, %.1f, %.1f)", position.x, position.y, position.z);
        ParticleUtil.spawnParticleEffect("Hexcode_Ice_Effect", position, accessor);
    }
}
