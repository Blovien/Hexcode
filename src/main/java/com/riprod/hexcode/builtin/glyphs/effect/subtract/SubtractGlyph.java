package com.riprod.hexcode.builtin.glyphs.effect.subtract;

import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.HexMathUtil;

public class SubtractGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Subtract";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar a = glyph.resolveInput("a", hexContext);
        HexVar b = glyph.resolveInput("b", hexContext);

        HexVar result = HexMathUtil.subtract(a, b);

        if (result != null) {
            Integer outputSlot = glyph.resolveOutput("result", hexContext);
            if (outputSlot != null) hexContext.setVariable(outputSlot, result);
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
