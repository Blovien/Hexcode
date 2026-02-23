package com.riprod.hexcode.builtin.glyphs.effect.multiply;

import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.Glyph;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.HexVar;
import com.riprod.hexcode.utils.HexMathUtil;

public class MultiplyGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Multiply";

    @Override
    public void execute(Glyph glyph, HexContext hexContext, ExecutionContext executionContext) {
        HexVar result = glyph.getInput(0, executionContext, hexContext);

        for (int i = 1; ; i++) {
            HexVar next = glyph.getInput(i, executionContext, hexContext);
            if (next == null) break;
            result = HexMathUtil.multiply(result, next);
        }

        if (result != null) {
            executionContext.setVariable(glyph.getOutput(0), result);
        }

        Executor.continueExecution(hexContext, executionContext);
    }
}
