package com.riprod.hexcode.builtin.glyphs.warp;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.VfxUtil;

public class WarpGlyphStyle {

    private static final String TELEPORT_PARTICLE = "Teleport";
    private static final String DEPARTURE_SOUND = "SFX_Portal_Neutral_Teleport_Local";
    private static final String ARRIVAL_SOUND = "SFX_Divine_Respawn";
    private static final float SPHERE_SCALE = 0.3f;
    private static final float SPHERE_DURATION = 1.0f;

    private WarpGlyphStyle() {
    }

    public static void render(Vector3d departure, Vector3d arrival, CommandBuffer<EntityStore> accessor) {
        World world = accessor.getExternalData().getWorld();

        VfxUtil.effect(TELEPORT_PARTICLE, DEPARTURE_SOUND, departure, accessor);
        DebugUtils.addSphere(world, departure, new Vector3f(0.2f, 0.9f, 0.9f), SPHERE_SCALE, SPHERE_DURATION);

        VfxUtil.effect(TELEPORT_PARTICLE, ARRIVAL_SOUND, arrival, accessor);
        DebugUtils.addSphere(world, arrival, new Vector3f(0.3f, 1.0f, 0.5f), SPHERE_SCALE, SPHERE_DURATION);
    }
}
