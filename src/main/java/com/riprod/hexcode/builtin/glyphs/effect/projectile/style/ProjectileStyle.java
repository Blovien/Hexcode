package com.riprod.hexcode.builtin.glyphs.effect.projectile.style;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class ProjectileStyle {

    private static final Vector3f DEFAULT_COLOR = new Vector3f(1.0f, 0.8f, 0.3f);
    private static final float TRAIL_THICKNESS = 0.08f;
    private static final float TRAIL_DURATION = 0.3f;
    private static final float HIT_LINE_THICKNESS = 0.12f;
    private static final float HIT_LINE_LENGTH = 0.6f;
    private static final float HIT_LINE_DURATION = 0.2f;

    private ProjectileStyle() {
    }

    public static void renderLaunch(Vector3d position, Vector3d direction, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Projectile_Launch", "SFX_Staff_Fire_Shoot", position, accessor);

        Vector3f color = resolveColor(colors);
        World world = accessor.getExternalData().getWorld();
        Vector3d trailEnd = new Vector3d(position).add(new Vector3d(direction).scale(2.0));
        VfxUtil.line(accessor, world, position, trailEnd, color, TRAIL_THICKNESS, TRAIL_DURATION, 0);
    }

    public static void renderEntityHit(Vector3d projectilePos, Vector3d hitPos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Projectile_Hit", "SFX_Staff_Flame_Fireball_Impact", hitPos, accessor);
        VfxUtil.particle("Area_Pulse", hitPos, accessor);

        Vector3f color = resolveColor(colors);
        World world = accessor.getExternalData().getWorld();
        Vector3d lineEnd = new Vector3d(hitPos).add(0, HIT_LINE_LENGTH, 0);
        VfxUtil.line(accessor, world, hitPos, lineEnd, color, HIT_LINE_THICKNESS, HIT_LINE_DURATION, 0);
    }

    public static void renderBlockHit(Vector3d hitPos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Projectile_Hit", "SFX_Fireball_Miss", hitPos, accessor);

        Vector3f color = resolveColor(colors);
        World world = accessor.getExternalData().getWorld();
        Vector3d lineEnd = new Vector3d(hitPos).add(0, HIT_LINE_LENGTH, 0);
        VfxUtil.line(accessor, world, hitPos, lineEnd, color, HIT_LINE_THICKNESS, HIT_LINE_DURATION, 0);
    }

    public static void renderMiss(Vector3d endPos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.particle("Projectile_Miss", endPos, accessor);
    }

    private static Vector3f resolveColor(HexColors colors) {
        if (colors == null || colors.getPrimaryColor() == null) return DEFAULT_COLOR;
        return HexColors.toVector3f(colors.getPrimaryColor());
    }
}
