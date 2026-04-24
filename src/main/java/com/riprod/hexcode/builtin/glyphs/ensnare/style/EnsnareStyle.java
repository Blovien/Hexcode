package com.riprod.hexcode.builtin.glyphs.ensnare.style;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.utils.VfxUtil;

public class EnsnareStyle {

    private EnsnareStyle() {
    }

    public static void renderSeismicBurst(Vector3d center, HexColors colors,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Ensnare_Burst", "SFX_Ensnare_Cast", center, accessor);
    }

    public static void renderSpikeDamage(Vector3d pos,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.particle("Ensnare_Hit", pos, accessor);
    }

    public static void renderSpikeDespawn(Vector3d pos,
            CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Ensnare_Crumble", "SFX_Ensnare_End", pos, accessor);
    }
}
