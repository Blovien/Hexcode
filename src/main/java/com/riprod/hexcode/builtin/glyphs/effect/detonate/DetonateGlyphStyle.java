package com.riprod.hexcode.builtin.glyphs.effect.detonate;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.VfxUtil;

public class DetonateGlyphStyle {

    private DetonateGlyphStyle() {
    }

    public static void render(Vector3d center, double radius, CommandBuffer<EntityStore> accessor) {
        String particle;
        if (radius <= 3.0) {
            particle = "Detonate_Small";
        } else if (radius <= 7.0) {
            particle = "Detonate_Medium";
        } else {
            particle = "Detonate_Big";
        }

        VfxUtil.particle(particle, center, accessor);
        VfxUtil.sound("SFX_Staff_Flame_Fireball_Impact", center, accessor);
        VfxUtil.sound("SFX_Fireball_Death", center, accessor);
    }
}
