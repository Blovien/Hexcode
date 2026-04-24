package com.riprod.hexcode.builtin.glyphs.scale.style;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class ScaleStyle {

    private ScaleStyle() {
    }

    public static void renderApply(Vector3d pos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Scale_Apply", "SFX_Scale_Cast", pos, accessor);
    }

    public static void renderRestore(Vector3d pos, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Scale_Restore", "SFX_Scale_End", pos, accessor);
    }
}
