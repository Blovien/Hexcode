package com.riprod.hexcode.builtin.glyphs.effect.phase;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class PhaseStyle {

    private PhaseStyle() {
    }

    public static void renderPhaseOut(Vector3d blockCenter, HexColors colors,
            ComponentAccessor<EntityStore> accessor) {
        VfxUtil.effect("Phase_Out", "SFX_Phase_Out", blockCenter, accessor);
    }

    public static void renderPhaseIn(Vector3d blockCenter, HexColors colors,
            ComponentAccessor<EntityStore> accessor) {
        VfxUtil.effect("Phase_In", "SFX_Phase_In", blockCenter, accessor);
    }

    public static void renderCrush(Vector3d blockCenter, HexColors colors,
            ComponentAccessor<EntityStore> accessor) {
        VfxUtil.effect("Phase_Crush", "SFX_Phase_Crush", blockCenter, accessor);
    }
}
