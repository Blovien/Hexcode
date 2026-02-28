package com.riprod.hexcode.builtin.glyphs.effect.halt;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.VfxUtil;

public class HaltGlyphStyle {

    private static final String ICE_BLAST_PARTICLE = "Ice_Blast";
    private static final String DUST_PARTICLE = "Dust_Sparkles";
    private static final String CRYSTALLIZE_SOUND = "SFX_Ice_Build";
    private static final Vector3f SPHERE_COLOR = new Vector3f(0.5f, 0.8f, 1.0f);
    private static final float SPHERE_SCALE = 0.3f;
    private static final float SPHERE_DURATION = 1.0f;

    private HaltGlyphStyle() {
    }

    public static void render(Vector3d targetPos, CommandBuffer<EntityStore> accessor) {
        VfxUtil.particle(ICE_BLAST_PARTICLE, targetPos, accessor);
        VfxUtil.particle(DUST_PARTICLE, targetPos, accessor);
        VfxUtil.sound(CRYSTALLIZE_SOUND, targetPos, accessor);

        World world = accessor.getExternalData().getWorld();
        DebugUtils.addSphere(world, targetPos, SPHERE_COLOR, SPHERE_SCALE, SPHERE_DURATION);
    }
}
