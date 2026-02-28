package com.riprod.hexcode.builtin.glyphs.effect.excavate;

import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.execution.component.HexContext;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.glyphs.component.Glyph;

public class ExcavateGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Excavate";

    

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        LOGGER.atInfo().log("Casted Excavate");
        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
