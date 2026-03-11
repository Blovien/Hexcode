package com.riprod.hexcode.core.common.glyphs.values;

import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public abstract interface HexValInterface {
    abstract public HexVar getValue(Glyph glyph, HexContext hexContext);
}
