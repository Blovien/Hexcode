package com.riprod.hexcode.builtin.glyphs.effect.arc.style;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class ArcStyle {

    private static final String ARC_PARTICLE = "Shock_Spawner";
    private static final String ARC_SOUND = "SFX_Staff_Fire_Shoot";
    private static final Vector3f DEFAULT_COLOR = new Vector3f(0.6f, 0.9f, 1.0f);
    private static final float BEAM_THICKNESS = 0.15f;
    private static final float BEAM_DURATION = 0.5f;
    private static final int PARTICLES_PER_ARC = 8;

    private ArcStyle() {
    }

    public static void renderArc(CommandBuffer<EntityStore> accessor, World world,
            Vector3d sourcePos, Vector3d targetPos, HexColors colors) {
        Vector3f color = resolveColor(colors);
        VfxUtil.line(accessor, world, sourcePos, targetPos, color, BEAM_THICKNESS, BEAM_DURATION, 0);
        VfxUtil.particleAlongPath(ARC_PARTICLE, sourcePos, targetPos, PARTICLES_PER_ARC, accessor);
        VfxUtil.sound(ARC_SOUND, sourcePos, accessor);
    }

    public static void renderHit(CommandBuffer<EntityStore> accessor, Vector3d position,
            HexColors colors) {
        VfxUtil.particle(ARC_PARTICLE, position, accessor);
        VfxUtil.sound(ARC_SOUND, position, accessor);
    }

    public static void renderFizzle(CommandBuffer<EntityStore> accessor, Vector3d position,
            HexColors colors) {
        VfxUtil.particle(ARC_PARTICLE, position, accessor);
    }

    private static Vector3f resolveColor(HexColors colors) {
        if (colors == null || colors.getPrimaryColor() == null) return DEFAULT_COLOR;
        return HexColors.toVector3f(colors.getPrimaryColor());
    }
}
