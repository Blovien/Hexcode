package com.riprod.hexcode.builtin.glyphs.seek;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolLaserPointer;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.PlayerUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class SeekGlyphStyle {

    private static final int ENTITY_HIT_COLOR = 0xFF3399;
    private static final int BLOCK_HIT_COLOR = 0x6644FF;
    private static final int MISS_COLOR = 0x553388;

    private static final String HIT_PARTICLE = "Shock_Spawner";
    private static final String HIT_SOUND = "SFX_Arrow_Frost_Hit";
    private static final int LASER_DURATION_MS = 1500;
    private static final float IMPACT_SPHERE_SIZE = 0.3f;
    private static final float IMPACT_SPHERE_DURATION = 1.0f;

    private SeekGlyphStyle() {
    }

    public enum HitType {
        ENTITY, BLOCK, MISS
    }

    public static void render(Vector3d origin, Vector3d endPoint, HitType hitType,
            ComponentAccessor<EntityStore> accessor) {
        int beamColor = switch (hitType) {
            case ENTITY -> ENTITY_HIT_COLOR;
            case BLOCK -> BLOCK_HIT_COLOR;
            case MISS -> MISS_COLOR;
        };

        BuilderToolLaserPointer laser = new BuilderToolLaserPointer(
                0,
                (float) origin.x, (float) origin.y, (float) origin.z,
                (float) endPoint.x, (float) endPoint.y, (float) endPoint.z,
                beamColor, LASER_DURATION_MS);
        PlayerUtil.broadcastPacketToPlayers(accessor, laser);

        if (hitType == HitType.MISS) {
            return;
        }

        ParticleUtil.spawnParticleEffect(HIT_PARTICLE, endPoint, accessor);

        World world = accessor.getExternalData().getWorld();
        Vector3f sphereColor = hitType == HitType.ENTITY
                ? new Vector3f(1.0f, 0.2f, 0.6f)
                : new Vector3f(0.4f, 0.27f, 1.0f);
        DebugUtils.addSphere(world, endPoint, sphereColor, IMPACT_SPHERE_SIZE, IMPACT_SPHERE_DURATION);

        int soundIndex = SoundEvent.getAssetMap().getIndex(HIT_SOUND);
        if (soundIndex >= 0) {
            SoundUtil.playSoundEvent3d(soundIndex, SoundCategory.SFX, endPoint, accessor);
        }
    }
}
