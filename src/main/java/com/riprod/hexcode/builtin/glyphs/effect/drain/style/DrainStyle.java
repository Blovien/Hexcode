package com.riprod.hexcode.builtin.glyphs.effect.drain.style;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class DrainStyle {

    private static final Vector3f DEFAULT_COLOR = new Vector3f(0.4f, 0.0f, 0.6f);

    public static void renderTick(Vector3d pos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        Vector3f color = DEFAULT_COLOR;
        if (colors != null && colors.getPrimaryColor() != null) {
            color = HexColors.toVector3f(colors.getPrimaryColor());
        }
        VfxUtil.particle("Hexcode_Drain_Channel", pos, accessor);
    }

    public static void renderComplete(Vector3d pos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        Vector3f color = DEFAULT_COLOR;
        if (colors != null && colors.getPrimaryColor() != null) {
            color = HexColors.toVector3f(colors.getPrimaryColor());
        }
        VfxUtil.effect("Hexcode_Drain_Complete", "SFX_Drain_Complete", pos, accessor);
    }
}
