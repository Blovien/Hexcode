package com.riprod.hexcode.builtin.glyphs.effect.nullify;

import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.execution.component.HexContext;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.glyphs.component.Glyph;

public class NullifyGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Nullify";

    

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        LOGGER.atInfo().log("Casted Nullify");
        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
