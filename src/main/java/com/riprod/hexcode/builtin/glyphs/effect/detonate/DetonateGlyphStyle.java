package com.riprod.hexcode.builtin.glyphs.effect.detonate;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.VfxUtil;

public class DetonateGlyphStyle {

    private DetonateGlyphStyle() {
    }

    public static void render(Vector3d center, double radius, CommandBuffer<EntityStore> accessor) {
        String particle;
        if (radius <= 3.0) {
            particle = "Explosion_Small";
        } else if (radius <= 7.0) {
            particle = "Explosion_Medium";
        } else {
            particle = "Explosion_Big";
        }

        VfxUtil.particle(particle, center, accessor);
        VfxUtil.sound("SFX_Staff_Flame_Fireball_Impact", center, accessor);
        VfxUtil.sound("SFX_Fireball_Death", center, accessor);

        World world = accessor.getExternalData().getWorld();
        Vector3f sphereColor = new Vector3f(1.0f, 0.3f, 0.1f);
        DebugUtils.addSphere(world, center, sphereColor, (float) (radius * 0.1), 1.5f);
    }
}
