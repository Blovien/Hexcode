package com.riprod.hexcode.builtin.glyphs.effect.erode;

import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.components.Glyph;


public class ErodeGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Erode";

    

    @Override
    public void execute(Glyph glyph, HexContext hexContext, ExecutionContext executionContext) {
        LOGGER.atInfo().log("Casted Erode");
        Executor.continueExecution(hexContext, executionContext);
    }
}
