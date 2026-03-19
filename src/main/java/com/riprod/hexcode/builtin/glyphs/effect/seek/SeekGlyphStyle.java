package com.riprod.hexcode.builtin.glyphs.effect.seek;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.VfxUtil;

public class SeekGlyphStyle {

    private static final Vector3f ENTITY_HIT_COLOR = new Vector3f(1.0f, 0.27f, 0.67f);
    private static final Vector3f BLOCK_HIT_COLOR = new Vector3f(0.27f, 0.0f, 0.8f);
    private static final Vector3f MISS_COLOR = new Vector3f(0.4f, 0.2f, 0.67f);
    private static final double LINE_THICKNESS = 0.12;
    private static final float LINE_DURATION = 1.5f;

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
        VfxUtil.line(accessor, world, origin, endPoint, beamColor, LINE_THICKNESS, LINE_DURATION, true);

        if (hitType != HitType.MISS) {
            VfxUtil.effect("Seek_Impact", "SFX_Arrow_Frost_Hit", endPoint, accessor);
        }
    }
}
