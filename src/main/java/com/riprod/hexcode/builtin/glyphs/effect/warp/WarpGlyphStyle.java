package com.riprod.hexcode.builtin.glyphs.effect.warp;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.VfxUtil;

public class WarpGlyphStyle {

    private WarpGlyphStyle() {
    }

    public static void render(Vector3d departure, Vector3d arrival, CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Warp_Departure", "SFX_Portal_Neutral_Teleport_Local", departure, accessor);
        VfxUtil.effect("Warp_Arrival", "SFX_Divine_Respawn", arrival, accessor);
    }
}
