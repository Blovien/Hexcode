package com.riprod.hexcode.builtin.glyphs.effect.freeze.style;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class FreezeStyle {

    private static final Vector3f DEFAULT_COLOR = new Vector3f(0.6f, 0.85f, 1.0f);

    private FreezeStyle() {
    }

    public static void renderFreeze(Vector3d pos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.particle("Freeze_Snow", pos, accessor);
        VfxUtil.particle("Freeze_Impact", pos, accessor);
        VfxUtil.sound("SFX_Ice_Build", pos, accessor);
    }

    public static void renderMelt(Vector3d pos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.particle("Freeze_Snow", pos, accessor);
        VfxUtil.sound("SFX_Ice_Break", pos, accessor);
    }

    private static Vector3f resolveColor(HexColors colors) {
        if (colors == null || colors.getPrimaryColor() == null) return DEFAULT_COLOR;
        return HexColors.toVector3f(colors.getPrimaryColor());
    }
}
