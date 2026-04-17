package com.riprod.hexcode.builtin.glyphs.effect.glaciate.style;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class GlaciateStyle {

    private GlaciateStyle() {
    }

    public static void renderSpawn(Vector3d pos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.particle("Glaciate_Spawn", pos, accessor);
        VfxUtil.sound("SFX_Ice_Build", pos, accessor);
    }

    public static void renderImpact(Vector3d pos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.particle("Glaciate_Impact", pos, accessor);
        VfxUtil.sound("SFX_Arrow_Frost_Hit", pos, accessor);
    }

    public static void renderMelt(Vector3d pos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.particle("Glaciate_Melt", pos, accessor);
        VfxUtil.sound("SFX_Ice_Break", pos, accessor);
    }
}
