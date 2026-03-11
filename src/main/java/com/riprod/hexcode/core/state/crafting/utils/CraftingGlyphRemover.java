package com.riprod.hexcode.core.state.crafting.utils;

import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.hexes.component.Hex;

public class CraftingGlyphRemover {
    public static void clearInputOutputConnections(Glyph glyph, Hex hex) {
        glyph.getInputs().clear();
        glyph.getOutputs().clear();
        String id = glyph.getId();
        for (Glyph other : hex.getGlyphs()) {
            if (other == glyph) continue;
            other.getInputs().values().removeIf(v -> v.equals(id));
            other.getOutputs().values().removeIf(v -> v.equals(id));
        }
    }
}
