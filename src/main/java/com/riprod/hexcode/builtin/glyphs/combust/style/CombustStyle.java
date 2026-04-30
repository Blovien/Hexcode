package com.riprod.hexcode.builtin.glyphs.combust.style;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.hexes.registry.HexStyleAsset;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.VfxUtil;

public class CombustStyle {

    private static final String GLYPH_ID = "Combust";

    private CombustStyle() {
    }

    private static GlyphAsset asset() {
        return GlyphAsset.getAssetMap().getAsset(GLYPH_ID);
    }

    public static void renderExplosion(ComponentAccessor<EntityStore> accessor, Vector3d center,
            double radius, HexContext ctx) {
        HexStyleAsset overrides = ctx != null ? ctx.getStyle() : null;
        VfxUtil.spawnPrimary(overrides, asset(), center, accessor);
        VfxUtil.spawnSecondary(overrides, asset(), center, accessor);
    }

    public static void renderLava(ComponentAccessor<EntityStore> accessor, Vector3d position,
            HexContext ctx) {
        HexStyleAsset overrides = ctx != null ? ctx.getStyle() : null;
        VfxUtil.spawnTertiary(overrides, asset(), position, accessor);
    }
}
