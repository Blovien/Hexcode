package com.riprod.hexcode.builtin.glyphs.effect.subtract;

import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.glyphs.component.Glyph;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.execution.component.HexContext;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.HexVar;
import com.riprod.hexcode.utils.HexMathUtil;

public class SubtractGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Subtract";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar result = glyph.getInput(0, hexContext);

        for (int i = 1; ; i++) {
            HexVar next = glyph.getInput(i, hexContext);
            if (next == null) break;
            result = HexMathUtil.subtract(result, next);
        }

        if (result != null) {
            hexContext.setVariable(glyph.getOutput(0, hexContext), result);
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
