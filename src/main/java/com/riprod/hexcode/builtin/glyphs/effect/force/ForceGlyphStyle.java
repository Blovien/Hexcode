package com.riprod.hexcode.builtin.glyphs.effect.force;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.VfxUtil;

public class ForceGlyphStyle {

    private ForceGlyphStyle() {
    }

    public static void render(Vector3d targetPos, Vector3d forceDir, CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Force_Impact", "SFX_Weapon_Charge_Swing", targetPos, accessor);
    }
}
