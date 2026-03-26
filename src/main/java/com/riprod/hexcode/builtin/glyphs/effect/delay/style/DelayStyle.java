package com.riprod.hexcode.builtin.glyphs.effect.delay.style;

import com.hypixel.hytale.math.vector.Vector3d;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;
import com.riprod.hexcode.utils.VfxUtil;

public class DelayStyle {

    private DelayStyle() {
    }

    public static void render(HexContext hexContext) {
        Vector3d casterPos = SpellVarUtil.resolvePosition(
                hexContext.getVariable(1), hexContext.getAccessor());
        if (casterPos == null) return;

        VfxUtil.effect("Delay_Shimmer", "SFX_Hex_Tick", casterPos, hexContext.getAccessor());
    }
}
