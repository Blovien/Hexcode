package com.riprod.hexcode.builtin.glyphs;

import com.riprod.hexcode.core.glyphs.component.GlyphHandler;

public class FireGlyph implements GlyphHandler {
    public static final String ID = "Fire";

    public FireGlyph() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void execute() {
        // Implementation for the blink glyph's behavior
    }
}