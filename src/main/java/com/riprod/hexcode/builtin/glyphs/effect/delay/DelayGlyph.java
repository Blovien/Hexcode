package com.riprod.hexcode.builtin.glyphs.effect.delay;

import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.execution.component.HexContext;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.utils.SpellVarUtil;
import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.glyphs.component.Glyph;

public class DelayGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Delay";
    private static final int TICKS_PER_SECOND = 20;

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        Double delayLength = SpellVarUtil.resolveNumberOrDefault(glyph.getInput(0, hexContext), 1.0d);

        int tickDelay = TICKS_PER_SECOND * 2 * (int) Math.round(delayLength); // default 2 second

        Executor.delayContinuation(glyph.getNext(), hexContext, tickDelay);
    }
}
