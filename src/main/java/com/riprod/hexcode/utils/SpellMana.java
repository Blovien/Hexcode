package com.riprod.hexcode.utils;

import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphRegistry;
import com.riprod.hexcode.core.common.hexes.component.Hex;

public final class SpellMana {

    private SpellMana() {
    }

    public static float computeTotalMana(Hex hex) {
        if (hex == null) return 0f;
        float total = 0f;
        for (Glyph glyph : hex.getGlyphs()) {
            if (glyph == null) continue;
            GlyphHandler handler = GlyphRegistry.get(glyph.getGlyphId());
            if (handler == null) continue;
            GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
            total += handler.collectMana(glyph, asset);
        }
        return total;
    }
}
