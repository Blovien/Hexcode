package com.riprod.hexcode.builtin.glyphs.effect.greater;

import java.util.List;

import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.HexMathUtil;

public class GreaterGlyph implements GlyphHandler {
    public static final String ID = "Glyph_Greater";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar a = glyph.resolveInput("a", hexContext);
        HexVar b = glyph.resolveInput("b", hexContext);
        boolean result = HexMathUtil.isGreater(a, b);

        List<String> next = glyph.getNext();
        if (result) {
            if (!next.isEmpty()) {
                Executor.continueExecution(List.of(next.get(0)), hexContext);
            }
        } else {
            if (next.size() > 1) {
                Executor.continueExecution(next.subList(1, next.size()), hexContext);
            }
        }
    }
}
