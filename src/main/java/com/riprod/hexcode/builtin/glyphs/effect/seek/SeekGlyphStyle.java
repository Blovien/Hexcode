package com.riprod.hexcode.builtin.glyphs.effect.seek;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolLaserPointer;
import com.hypixel.hytale.server.core.universe.world.PlayerUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.VfxUtil;

public class SeekGlyphStyle {

    private static final int ENTITY_HIT_COLOR = 0xFF44AA;
    private static final int BLOCK_HIT_COLOR = 0x4400CC;
    private static final int MISS_COLOR = 0x6633AA;
    private static final int BEAM_DURATION_MS = 1500;

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

        BuilderToolLaserPointer beam = new BuilderToolLaserPointer(
                0,
                (float) origin.x, (float) origin.y, (float) origin.z,
                (float) endPoint.x, (float) endPoint.y, (float) endPoint.z,
                beamColor, BEAM_DURATION_MS);
        PlayerUtil.broadcastPacketToPlayers(accessor, beam);

        if (hitType != HitType.MISS) {
            VfxUtil.effect("Seek_Impact", "SFX_Arrow_Frost_Hit", endPoint, accessor);
        }
    }
}
