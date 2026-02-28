package com.riprod.hexcode.builtin.glyphs.effect.arc;

import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.execution.component.HexContext;
import com.riprod.hexcode.core.glyphs.component.Glyph;

import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.hypixel.hytale.logger.HytaleLogger;

public class ArcGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Arc";

    

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        LOGGER.atInfo().log("Casted Arc");
        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
