package com.riprod.hexcode.builtin.glyphs.swap.style;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class SwapStyle {

    private static final Vector3f DEFAULT_COLOR = new Vector3f(0.7f, 0.4f, 1.0f);

    private SwapStyle() {
    }

    public static void render(Vector3d posA, Vector3d posB, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Swap_Rift", "SFX_Portal_Neutral", posA, accessor);
        VfxUtil.effect("Swap_Rift", "SFX_Portal_Neutral", posB, accessor);
    }

    public static Vector3f resolveColor(HexColors colors) {
        if (colors == null || colors.getPrimaryColor() == null) return DEFAULT_COLOR;
        return HexColors.toVector3f(colors.getPrimaryColor());
    }
}
