package com.riprod.hexcode.builtin.glyphs.effect.smelt;

import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.hypixel.hytale.logger.HytaleLogger;

public class SmeltGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Smelt";

    

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        LOGGER.atInfo().log("Casted Smelt");
        Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
