package com.riprod.hexcode.builtin.glyphs.effect.divide;

import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.HexMathUtil;

public class DivideGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Divide";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar result = glyph.getInput(0, hexContext);

        for (int i = 1; ; i++) {
            HexVar next = glyph.getInput(i, hexContext);
            if (next == null) break;
            result = HexMathUtil.divide(result, next);
        }

        if (result != null) {
            hexContext.setVariable(glyph.getOutput(0, hexContext), result);
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
