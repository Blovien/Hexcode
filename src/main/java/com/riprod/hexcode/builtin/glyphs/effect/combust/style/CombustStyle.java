package com.riprod.hexcode.builtin.glyphs.effect.combust.style;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class CombustStyle {

    private static final String FIRE_PARTICLE = "Effect_Fire";
    private static final String EXPLOSION_PARTICLE = "Explosion_Big";
    private static final String EXPLOSION_SOUND = "SFX_Fireball_Death";
    private static final String FIRE_SOUND = "SFX_Staff_Flame_Fireball_Impact";
    private static final Vector3f DEFAULT_COLOR = new Vector3f(1.0f, 0.5f, 0.15f);

    private static final String EXPLOSION_BIG = "Gust_Big";

    private CombustStyle() {
    }

    public static void renderExplosion(CommandBuffer<EntityStore> accessor, Vector3d center,
            double radius, HexColors colors) {
        VfxUtil.particle(EXPLOSION_PARTICLE, center, accessor);
        VfxUtil.particle(EXPLOSION_PARTICLE, center, accessor);
        VfxUtil.particle(EXPLOSION_BIG, center, accessor);
        VfxUtil.particle(FIRE_PARTICLE, center, accessor);
        VfxUtil.sound(EXPLOSION_SOUND, center, accessor);
        VfxUtil.sound(FIRE_SOUND, center, accessor);
    }

    public static void renderLava(CommandBuffer<EntityStore> accessor, Vector3d position,
            HexColors colors) {
        VfxUtil.particle(FIRE_PARTICLE, position, accessor);
    }

    private static Vector3f resolveColor(HexColors colors) {
        if (colors == null || colors.getPrimaryColor() == null) return DEFAULT_COLOR;
        return HexColors.toVector3f(colors.getPrimaryColor());
    }
}
