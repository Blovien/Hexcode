package com.riprod.hexcode.builtin.glyphs.effect.force;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.VfxUtil;

public class ForceGlyphStyle {

    private static final String IMPACT_PARTICLE = "Shock_Spawner";
    private static final String FLASH_PARTICLE = "Impact_Critical";
    private static final String IMPACT_SOUND = "SFX_Weapon_Charge_Swing";
    private static final Vector3f SPHERE_COLOR = new Vector3f(1.0f, 0.6f, 0.1f);
    private static final float SPHERE_SCALE = 0.25f;
    private static final float SPHERE_DURATION = 0.8f;

    private ForceGlyphStyle() {
    }

    public static void render(Vector3d targetPos, Vector3d forceDir, CommandBuffer<EntityStore> accessor) {
        VfxUtil.particle(IMPACT_PARTICLE, targetPos, accessor);
        VfxUtil.particle(FLASH_PARTICLE, targetPos, accessor);
        VfxUtil.sound(IMPACT_SOUND, targetPos, accessor);

        World world = accessor.getExternalData().getWorld();
        DebugUtils.addSphere(world, targetPos, SPHERE_COLOR, SPHERE_SCALE, SPHERE_DURATION);
    }
}
