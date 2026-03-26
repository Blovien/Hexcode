package com.riprod.hexcode.builtin.glyphs.effect.divide;

import java.util.concurrent.ThreadLocalRandom;

import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.values.HexValInterface;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.HexMathUtil;

public class DivideGlyph implements GlyphHandler, HexValInterface {
    public static final String ID = "Glyph_Divide";
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

    private HexVar compute(Glyph glyph, HexContext hexContext) {
        HexVar a = glyph.resolveInput("a", hexContext);
        HexVar b = glyph.resolveInput("b", hexContext);
        return HexMathUtil.divide(a, b);
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar result = compute(glyph, hexContext);

        if (result != null) {
            Integer outputSlot = glyph.resolveOutput("result", hexContext);
            if (outputSlot != null) hexContext.setVariable(outputSlot, result);
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }

    @Override
    public HexVar getValue(Glyph glyph, HexContext hexContext) {
        return compute(glyph, hexContext);
    }
}
