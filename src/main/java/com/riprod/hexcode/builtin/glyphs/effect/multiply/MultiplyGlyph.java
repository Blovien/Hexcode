package com.riprod.hexcode.builtin.glyphs.effect.multiply;

import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.HexMathUtil;

public class MultiplyGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Multiply";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar a = glyph.resolveInput("a", hexContext);
        HexVar b = glyph.resolveInput("b", hexContext);

        HexVar result = HexMathUtil.multiply(a, b);

        if (result != null) {
            Integer outputSlot = glyph.resolveOutput("result", hexContext);
            if (outputSlot != null) hexContext.setVariable(outputSlot, result);
            else hexContext.setVariable(1, result); // default to slot 1 if no output specified
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
