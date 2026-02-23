package com.riprod.hexcode.builtin.glyphs.effect.combust;

import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.components.Glyph;

import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.hypixel.hytale.logger.HytaleLogger;

public class CombustGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Combust";

    

    @Override
    public void execute(Glyph glyph, HexContext hexContext, ExecutionContext executionContext) {
        LOGGER.atInfo().log("Casted Combust");
        Executor.continueExecution(hexContext, executionContext);
    }
}
