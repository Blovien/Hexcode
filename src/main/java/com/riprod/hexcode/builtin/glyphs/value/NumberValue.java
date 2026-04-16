package com.riprod.hexcode.builtin.glyphs.value;

import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class NumberValue implements GlyphHandler {

    private int number;

    public NumberValue() {
    }

    public NumberValue(int number) {
        this.number = number;
    }

    @Override
    public HexVar readValue(Glyph glyph, HexContext hexContext) {
        return new NumberVar(this.number);
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
