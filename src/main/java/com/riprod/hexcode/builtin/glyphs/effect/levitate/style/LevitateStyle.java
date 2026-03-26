package com.riprod.hexcode.builtin.glyphs.effect.levitate.style;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class LevitateStyle {

    private LevitateStyle() {
    }

    public static void renderActivation(Vector3d pos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Levitate_Activate", "SFX_Weapon_Charge_Swing", pos, accessor);
    }
}
