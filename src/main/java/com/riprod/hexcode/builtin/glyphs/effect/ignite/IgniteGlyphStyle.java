package com.riprod.hexcode.builtin.glyphs.effect.ignite;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.VfxUtil;

public class IgniteGlyphStyle {

    private IgniteGlyphStyle() {
    }

    public static void render(Vector3d pos, CommandBuffer<EntityStore> accessor) {
        VfxUtil.particle("Effect_Fire", pos, accessor);
        VfxUtil.sound("SFX_Staff_Flame_Fireball_Impact", pos, accessor);
    }
}
