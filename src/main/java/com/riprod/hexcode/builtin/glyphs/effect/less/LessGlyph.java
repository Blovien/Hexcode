package com.riprod.hexcode.builtin.glyphs.effect.less;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.HexMathUtil;

public class LessGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Less";
    private static final float INITIAL_PASS_CHANCE = 0.995f;
    private static final float DECAY_PER_USE = 0.007f; // slower decay, ~10 uses -> ~0.92 chance

    @Override
    public boolean canExecute(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null) return true;

        int useCount = tracker.getGlyphTypeCount(glyph.getGlyphId());
        float chance = INITIAL_PASS_CHANCE - useCount * DECAY_PER_USE;
        chance = Math.max(0.01f, Math.min(1f, chance));

        chance *= tracker.getVolatilityMultiplier();
        chance = Math.max(0.01f, Math.min(1f, chance));

        float roll = ThreadLocalRandom.current().nextFloat();
        tracker.incrementGlyphType(glyph.getGlyphId());

        if (roll >= chance) {
            LOGGER.atInfo().log("glyph %s fizzled: rolled %.3f against %.3f chance (use #%d)",
                    glyph.getGlyphId(), roll, chance, useCount + 1);
        }
        return roll < chance;
    }


    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar a = glyph.resolveInput("a", hexContext);
        HexVar b = glyph.resolveInput("b", hexContext);
        boolean result = HexMathUtil.isLess(a, b);

        List<String> next = glyph.getNext();
        if (result) {
            if (!next.isEmpty()) {
                Executor.continueExecution(List.of(next.get(0)), hexContext);
            }
        } else {
            if (next.size() > 1) {
                Executor.continueExecution(next.subList(1, next.size()), hexContext);
            }
        }
    }
}
