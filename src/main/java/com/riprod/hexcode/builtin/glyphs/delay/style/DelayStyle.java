package com.riprod.hexcode.builtin.glyphs.delay.style;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.HexDirectionUtil;
import com.riprod.hexcode.utils.HexVarUtil;
import com.riprod.hexcode.utils.VfxUtil;

public class DelayStyle {

    private DelayStyle() {
    }

    public static void render(HexContext hexContext) {
        Vector3d casterPos = HexVarUtil.position(
                hexContext.getVariable(Glyph.DEFAULT_SLOT), hexContext.getAccessor());
        if (casterPos == null) return;

        VfxUtil.effect("Delay_Shimmer", "SFX_Hex_Tick", casterPos, hexContext.getAccessor());
    }

    public static void renderExpiry(Vector3d pos, @Nullable HexColors colors,
            CommandBuffer<EntityStore> buffer) {
        VfxUtil.particle("Delay_Shimmer", pos, buffer);
    }
}
