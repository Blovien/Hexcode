package com.riprod.hexcode.builtin.glyphs;

import com.riprod.hexcode.core.glyphs.component.GlyphHandler;

public class HealGlyph implements GlyphHandler {
    public static final String ID = "Heal";

    public HealGlyph() {
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