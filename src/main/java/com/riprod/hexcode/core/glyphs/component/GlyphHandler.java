package com.riprod.hexcode.core.glyphs.component;

import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.Glyph;
import com.riprod.hexcode.components.HexContext;

public abstract interface GlyphHandler {
    public abstract void execute(Glyph glyph, HexContext hexContext, ExecutionContext executionContext);
}