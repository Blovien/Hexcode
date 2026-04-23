package com.riprod.hexcode.builtin.glyphs.halt.style;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class HaltStyle {

    private static final Vector3f DEFAULT_COLOR = new Vector3f(0.8f, 0.73f, 1.0f);

    private HaltStyle() {
    }

    public static void render(Vector3d targetPos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Halt_Crystallize", "SFX_Ice_Build", targetPos, accessor);
    }

    public static Vector3f resolveColor(HexColors colors) {
        if (colors == null || colors.getPrimaryColor() == null) return DEFAULT_COLOR;
        return HexColors.toVector3f(colors.getPrimaryColor());
    }
}
