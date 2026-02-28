package com.riprod.hexcode.builtin.glyphs.effect.propel;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.VfxUtil;

public class PropelGlyphStyle {

    private static final String LAUNCH_PARTICLE_1 = "MagicBlast";
    private static final String LAUNCH_PARTICLE_2 = "Rings";
    private static final String LAUNCH_SOUND = "SFX_Staff_Fire_Shoot";

    private static final String ENTITY_HIT_PARTICLE_2 = "Explosion_Small";
    private static final String ENTITY_HIT_SOUND = "SFX_Staff_Flame_Fireball_Impact";
    private static final Vector3f ENTITY_HIT_COLOR = new Vector3f(0.8f, 0.2f, 1.0f);
    private static final float ENTITY_HIT_SPHERE_SCALE = 0.4f;
    private static final float ENTITY_HIT_SPHERE_DURATION = 1.0f;

    private static final String BLOCK_HIT_PARTICLE = "Impact_Critical";
    private static final String BLOCK_HIT_SOUND = "SFX_Fireball_Miss";
    private static final Vector3f BLOCK_HIT_COLOR = new Vector3f(0.5f, 0.3f, 1.0f);
    private static final float BLOCK_HIT_SPHERE_SCALE = 0.3f;
    private static final float BLOCK_HIT_SPHERE_DURATION = 0.8f;

    private static final String MISS_PARTICLE = "Dust_Sparkles";

    private static final Vector3f LAUNCH_LINE_COLOR = new Vector3f(0.4f, 0.8f, 1.0f);
    private static final double LAUNCH_LINE_LENGTH = 0.5;
    private static final double LAUNCH_LINE_THICKNESS = 0.3;
    private static final float LAUNCH_LINE_DURATION = 0.3f;

    private static final double HIT_LINE_THICKNESS = 0.45;
    private static final float HIT_LINE_DURATION = 0.4f;

    private PropelGlyphStyle() {
    }

    public static void renderLaunch(Vector3d position, Vector3d direction,
            ComponentAccessor<EntityStore> accessor) {
        VfxUtil.effect(LAUNCH_PARTICLE_1, LAUNCH_SOUND, position, accessor);
        VfxUtil.particle(LAUNCH_PARTICLE_2, position, accessor);

        World world = accessor.getExternalData().getWorld();
        Vector3d lineEnd = new Vector3d(position).addScaled(direction, LAUNCH_LINE_LENGTH);
        VfxUtil.line(world, position, lineEnd, LAUNCH_LINE_COLOR, LAUNCH_LINE_THICKNESS,
                LAUNCH_LINE_DURATION, true);
    }

    public static void renderEntityHit(Vector3d projectilePos, Vector3d hitPos,
            ComponentAccessor<EntityStore> accessor) {
        VfxUtil.sound(ENTITY_HIT_SOUND, hitPos, accessor);
        VfxUtil.particle(ENTITY_HIT_PARTICLE_2, hitPos, accessor);

        World world = accessor.getExternalData().getWorld();
        DebugUtils.addSphere(world, hitPos, ENTITY_HIT_COLOR, ENTITY_HIT_SPHERE_SCALE,
                ENTITY_HIT_SPHERE_DURATION);
        VfxUtil.line(world, projectilePos, hitPos, ENTITY_HIT_COLOR, HIT_LINE_THICKNESS,
                HIT_LINE_DURATION, true);
    }

    public static void renderBlockHit(Vector3d hitPos, ComponentAccessor<EntityStore> accessor) {
        VfxUtil.effect(BLOCK_HIT_PARTICLE, BLOCK_HIT_SOUND, hitPos, accessor);

        World world = accessor.getExternalData().getWorld();
        DebugUtils.addSphere(world, hitPos, BLOCK_HIT_COLOR, BLOCK_HIT_SPHERE_SCALE,
                BLOCK_HIT_SPHERE_DURATION);
    }

    public static void renderMiss(Vector3d endPos, ComponentAccessor<EntityStore> accessor) {
        VfxUtil.particle(MISS_PARTICLE, endPos, accessor);
    }
}
