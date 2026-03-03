package com.riprod.hexcode.core.common.glyphs.component;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public abstract interface GlyphHandler {
    public abstract void execute(Glyph glyph, HexContext hexContext);
}