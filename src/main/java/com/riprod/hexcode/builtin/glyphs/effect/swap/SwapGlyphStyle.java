package com.riprod.hexcode.builtin.glyphs.effect.swap;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.VfxUtil;

public class SwapGlyphStyle {

    private SwapGlyphStyle() {
    }

    public static void render(Vector3d posA, Vector3d posB, ComponentAccessor<EntityStore> accessor) {
        VfxUtil.effect("Swap_Rift", "SFX_Portal_Neutral", posA, accessor);
        VfxUtil.effect("Swap_Rift", "SFX_Portal_Neutral", posB, accessor);
    }
}
