package com.riprod.hexcode.builtin.glyphs.effect.glaciate.style;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class GlaciateStyle {

    private GlaciateStyle() {
    }

    public static void renderSpawn(Vector3d pos, HexColors colors,
            ComponentAccessor<EntityStore> accessor) {
        VfxUtil.particle("Effect_Snow", pos, accessor);
        VfxUtil.sound("SFX_Ice_Build", pos, accessor);
    }

    public static void renderImpact(Vector3d pos, HexColors colors,
            ComponentAccessor<EntityStore> accessor) {
        VfxUtil.particle("Effect_Snow_Impact", pos, accessor);
        VfxUtil.particle("Effect_Snow", pos, accessor);
        VfxUtil.sound("SFX_Arrow_Frost_Hit", pos, accessor);
    }

    public static void renderMelt(Vector3d pos, HexColors colors,
            ComponentAccessor<EntityStore> accessor) {
        VfxUtil.particle("Effect_Snow", pos, accessor);
        VfxUtil.sound("SFX_Ice_Break", pos, accessor);
    }
}
