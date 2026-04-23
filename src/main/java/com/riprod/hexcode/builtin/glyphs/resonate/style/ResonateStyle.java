package com.riprod.hexcode.builtin.glyphs.resonate.style;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class ResonateStyle {

    private ResonateStyle() {
    }

    public static void renderResonate(Vector3d pos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Gust_Smoke", "SFX_Block_Break", pos, accessor);
    }

    public static void renderNoSignal(Vector3d pos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Gust_Smoke", "SFX_Block_Break", pos, accessor);
    }
}
