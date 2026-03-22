package com.riprod.hexcode.builtin.glyphs.effect.add;

import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.values.HexValInterface;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.HexMathUtil;

public class AddGlyph implements GlyphHandler, HexValInterface {
    public static final String ID = "Glyph_Add";

    private HexVar compute(Glyph glyph, HexContext hexContext) {
        HexVar a = glyph.resolveInput("a", hexContext);
        HexVar b = glyph.resolveInput("b", hexContext);
        return HexMathUtil.add(a, b);
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar result = compute(glyph, hexContext);

        if (result != null) {
            Integer outputSlot = glyph.resolveOutput("result", hexContext);
            if (outputSlot != null) hexContext.setVariable(outputSlot, result);
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }

    @Override
    public HexVar getValue(Glyph glyph, HexContext hexContext) {
        return compute(glyph, hexContext);
    }
}
