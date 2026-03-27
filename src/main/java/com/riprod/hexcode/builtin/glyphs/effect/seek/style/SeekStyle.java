package com.riprod.hexcode.builtin.glyphs.effect.seek.style;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class SeekStyle {

    private static final Vector3f DEFAULT_COLOR = new Vector3f(0.4f, 0.2f, 0.67f);
    private static final double LINE_THICKNESS = 0.12;
    private static final float LINE_DURATION = 1.5f;

    private SeekStyle() {
    }

    public enum HitType {
        ENTITY, BLOCK, MISS
    }

    public static void render(Vector3d origin, Vector3d endPoint, HitType hitType,
            HexColors colors, ComponentAccessor<EntityStore> accessor) {
        Vector3f beamColor = DEFAULT_COLOR;
        if (colors != null && colors.getPrimaryColor() != null) {
            beamColor = HexColors.toVector3f(colors.getPrimaryColor());
        }

        World world = accessor.getExternalData().getWorld();
        VfxUtil.line(accessor, world, origin, endPoint, beamColor, LINE_THICKNESS, LINE_DURATION, DebugUtils.FLAG_FADE);

        if (hitType != HitType.MISS) {
            VfxUtil.effect("Seek_Impact", "SFX_Arrow_Frost_Hit", endPoint, accessor);
        }
    }
}
