package com.riprod.hexcode.builtin.glyphs.warp.style;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class WarpStyle {

    private static final Vector3f DEFAULT_COLOR = new Vector3f(0.5f, 0.3f, 0.9f);

    private WarpStyle() {
    }

    public static void render(Vector3d departure, Vector3d arrival, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Warp_Departure", "SFX_Portal_Neutral_Teleport_Local", departure, accessor);
        VfxUtil.effect("Warp_Arrival", "SFX_Divine_Respawn", arrival, accessor);
    }

    public static Vector3f resolveColor(HexColors colors) {
        if (colors == null || colors.getPrimaryColor() == null) return DEFAULT_COLOR;
        return HexColors.toVector3f(colors.getPrimaryColor());
    }
}
