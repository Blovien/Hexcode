package com.riprod.hexcode.core.glyphs.component;
import com.riprod.hexcode.core.execution.component.HexContext;

public abstract interface GlyphHandler {
    public abstract void execute(Glyph glyph, HexContext hexContext);
}