package com.riprod.hexcode.builtin.glyphs.effect.growth.style;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class GrowthStyle {

    private GrowthStyle() {
    }

    public static void renderEntityHit(Vector3d pos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Hexcode_Grow_Effect", "SFX_Block_Break", pos, accessor);
    }

    public static void renderBlockHit(Vector3d pos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Hexcode_Grow_Effect", "SFX_Block_Break", pos, accessor);
    }
}
