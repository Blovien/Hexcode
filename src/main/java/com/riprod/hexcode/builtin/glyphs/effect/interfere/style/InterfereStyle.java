package com.riprod.hexcode.builtin.glyphs.effect.interfere.style;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class InterfereStyle {

    private InterfereStyle() {
    }

    public static void renderHijack(Vector3d pos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Gust_Smoke", "SFX_Block_Break", pos, accessor);
    }

    public static void renderStrip(Vector3d pos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Gust_Smoke", "SFX_Block_Break", pos, accessor);
    }

    public static void renderBlockStrip(Vector3d pos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Gust_Smoke", "SFX_Block_Break", pos, accessor);
    }
}
