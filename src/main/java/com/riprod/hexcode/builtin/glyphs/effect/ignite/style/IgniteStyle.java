package com.riprod.hexcode.builtin.glyphs.effect.ignite.style;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class IgniteStyle {

    private IgniteStyle() {
    }

    public static void render(Vector3d pos, HexColors colors,
            ComponentAccessor<EntityStore> accessor) {
        VfxUtil.particle("Ignite_Fire", pos, accessor);
        VfxUtil.sound("SFX_Staff_Flame_Fireball_Impact", pos, accessor);
    }
}
