package com.riprod.hexcode.builtin.glyphs.gust;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class GustGlyphStyle {

    private GustGlyphStyle() {
    }

    public static void render(Vector3d center, double radius, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        String particle;
        if (radius <= 3.0) {
            particle = "Gust_Small";
        } else if (radius <= 7.0) {
            particle = "Gust_Medium";
        } else {
            particle = "Gust_Big";
        }

        VfxUtil.particle(particle, center, accessor);
        VfxUtil.sound("SFX_Staff_Flame_Fireball_Impact", center, accessor);
        VfxUtil.sound("SFX_Fireball_Death", center, accessor);
    }
}
