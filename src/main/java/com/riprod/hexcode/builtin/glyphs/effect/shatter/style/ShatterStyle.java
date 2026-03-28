package com.riprod.hexcode.builtin.glyphs.effect.shatter.style;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class ShatterStyle {

    private static final Vector3f DEFAULT_COLOR = new Vector3f(0.6f, 0.9f, 1.0f);
    private static final float LINE_THICKNESS = 0.06f;
    private static final float LINE_DURATION = 0.25f;
    private static final float HIT_LINE_THICKNESS = 0.1f;
    private static final float HIT_LINE_DURATION = 0.3f;

    private ShatterStyle() {
    }

    public static void renderLaunch(Vector3d position, Vector3d direction, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Hexcode_Ice_Effect", "SFX_Staff_Fire_Shoot", position, accessor);

        Vector3f color = resolveColor(colors);
        World world = accessor.getExternalData().getWorld();
        Vector3d trailEnd = new Vector3d(position).add(new Vector3d(direction).scale(1.5));
        VfxUtil.line(accessor, world, position, trailEnd, color, LINE_THICKNESS, LINE_DURATION, 0);
    }

    public static void renderShardHit(Vector3d hitPos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Hexcode_Ice_Effect", "SFX_Fireball_Miss", hitPos, accessor);

        Vector3f color = resolveColor(colors);
        World world = accessor.getExternalData().getWorld();
        Vector3d lineEnd = new Vector3d(hitPos).add(0, 0.5, 0);
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
