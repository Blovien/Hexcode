package com.riprod.hexcode.builtin.glyphs.effect.greater;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.HexMathUtil;

public class GreaterGlyph implements GlyphHandler {
    public static final String ID = "Glyph_Greater";
    private static final float DEGRADATION_RATE = 0.25f;

    @Override
    public boolean canExecute(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null) return true;

        int useCount = tracker.getGlyphTypeCount(glyph.getGlyphId());
        float g = 1.0f / (1 + useCount * DEGRADATION_RATE);
        float chance = g * tracker.getVolatilityMultiplier();
        chance = Math.max(0f, Math.min(1f, chance));

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
        boolean result = HexMathUtil.isGreater(a, b);

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
