package com.riprod.hexcode.builtin.glyphs.effect.seek;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.VfxUtil;

public class SeekGlyphStyle {

    private static final Vector3f ENTITY_HIT_COLOR = new Vector3f(1.0f, 0.2f, 0.6f);
    private static final Vector3f BLOCK_HIT_COLOR = new Vector3f(0.4f, 0.27f, 1.0f);
    private static final Vector3f MISS_COLOR = new Vector3f(0.33f, 0.2f, 0.53f);
    private static final double LINE_THICKNESS = 0.45;
    private static final float LINE_DURATION = 1.5f;

    private static final String HIT_PARTICLE = "Shock_Spawner";
    private static final String HIT_SOUND = "SFX_Arrow_Frost_Hit";
    private static final float IMPACT_SPHERE_SIZE = 0.3f;
    private static final float IMPACT_SPHERE_DURATION = 1.0f;

    private SeekGlyphStyle() {
    }

    public enum HitType {
        ENTITY, BLOCK, MISS
    }

    public static void render(Vector3d origin, Vector3d endPoint, HitType hitType,
            ComponentAccessor<EntityStore> accessor) {
        Vector3f beamColor = switch (hitType) {
            case ENTITY -> ENTITY_HIT_COLOR;
            case BLOCK -> BLOCK_HIT_COLOR;
            case MISS -> MISS_COLOR;
        };

        World world = accessor.getExternalData().getWorld();
        VfxUtil.line(world, origin, endPoint, beamColor, LINE_THICKNESS, LINE_DURATION, true);

        if (hitType == HitType.MISS) {
            return;
        }

        ParticleUtil.spawnParticleEffect(HIT_PARTICLE, endPoint, accessor);

        DebugUtils.addSphere(world, endPoint, beamColor, IMPACT_SPHERE_SIZE, IMPACT_SPHERE_DURATION);

        int soundIndex = SoundEvent.getAssetMap().getIndex(HIT_SOUND);
        if (soundIndex >= 0) {
            SoundUtil.playSoundEvent3d(soundIndex, SoundCategory.SFX, endPoint, accessor);
        }
    }
}
