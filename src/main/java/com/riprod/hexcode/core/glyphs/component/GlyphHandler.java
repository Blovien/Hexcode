package com.riprod.hexcode.core.glyphs.component;

import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.HexContext;

public abstract interface GlyphHandler {
    public abstract String getId();

    public abstract void execute(HexContext hexContext, ExecutionContext executionContext);
}