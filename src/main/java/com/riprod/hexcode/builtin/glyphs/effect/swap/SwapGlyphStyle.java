package com.riprod.hexcode.builtin.glyphs.effect.swap;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.VfxUtil;

public class SwapGlyphStyle {

    private static final String TELEPORT_PARTICLE = "Teleport";
    private static final String RINGS_PARTICLE = "Rings";
    private static final String SWAP_SOUND = "SFX_Portal_Neutral";
    private static final Vector3f LINE_COLOR = new Vector3f(0.6f, 0.2f, 1.0f);
    private static final double LINE_THICKNESS = 0.45;
    private static final float LINE_DURATION = 0.8f;
    private static final Vector3f SPHERE_COLOR = new Vector3f(0.7f, 0.3f, 1.0f);
    private static final float SPHERE_SCALE = 0.35f;
    private static final float SPHERE_DURATION = 1.2f;

    private SwapGlyphStyle() {
    }

    public static void render(Vector3d posA, Vector3d posB, ComponentAccessor<EntityStore> accessor) {
        VfxUtil.effect(TELEPORT_PARTICLE, SWAP_SOUND, posA, accessor);
        VfxUtil.effect(TELEPORT_PARTICLE, SWAP_SOUND, posB, accessor);

        VfxUtil.particle(RINGS_PARTICLE, posA, accessor);
        VfxUtil.particle(RINGS_PARTICLE, posB, accessor);

        World world = accessor.getExternalData().getWorld();
        DebugUtils.addSphere(world, posA, SPHERE_COLOR, SPHERE_SCALE, SPHERE_DURATION);
        DebugUtils.addSphere(world, posB, SPHERE_COLOR, SPHERE_SCALE, SPHERE_DURATION);

        VfxUtil.line(accessor, world, posA, posB, LINE_COLOR, LINE_THICKNESS, LINE_DURATION, true);
    }
}
