package com.riprod.hexcode.builtin.glyphs.effect.halt;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.VfxUtil;

public class HaltGlyphStyle {

    private HaltGlyphStyle() {
    }

    public static void render(Vector3d targetPos, CommandBuffer<EntityStore> accessor) {
        VfxUtil.effect("Halt_Crystallize", "SFX_Ice_Build", targetPos, accessor);
    }
}
