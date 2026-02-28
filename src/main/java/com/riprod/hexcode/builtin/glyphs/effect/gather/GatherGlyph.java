package com.riprod.hexcode.builtin.glyphs.effect.gather;

import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.execution.component.HexContext;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.glyphs.component.Glyph;

public class GatherGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Gather";

    

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        LOGGER.atInfo().log("Casted Gather");
        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
