package com.riprod.hexcode.builtin.glyphs.effect.rupture.style;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class RuptureStyle {

    private RuptureStyle() {
    }

    public static void renderSeismicBurst(Vector3d center, HexColors colors,
            ComponentAccessor<EntityStore> accessor) {
        VfxUtil.effect("Rupture_Burst", "SFX_Rupture_Cast", center, accessor);
    }

    public static void renderSpikeDamage(Vector3d pos,
            ComponentAccessor<EntityStore> accessor) {
        VfxUtil.particle("Rupture_Hit", pos, accessor);
    }

    public static void renderSpikeDespawn(Vector3d pos,
            ComponentAccessor<EntityStore> accessor) {
        VfxUtil.effect("Rupture_Crumble", "SFX_Rupture_End", pos, accessor);
    }
}
