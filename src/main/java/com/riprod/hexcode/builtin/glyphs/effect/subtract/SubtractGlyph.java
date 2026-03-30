package com.riprod.hexcode.builtin.glyphs.effect.subtract;

import java.util.concurrent.ThreadLocalRandom;

import com.hypixel.hytale.math.vector.Vector3d;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.values.HexValInterface;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.HexMathUtil;
import com.riprod.hexcode.utils.SpellVarUtil;

public class SubtractGlyph implements GlyphHandler, HexValInterface {
    public static final String ID = "Glyph_Subtract";
    private static final float INITIAL_PASS_CHANCE = 0.995f;
    private static final float DECAY_PER_USE = 0.007f; // slower decay, ~10 uses -> ~0.92 chance

    @Override
    public boolean canExecute(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null)
            return true;

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

    private HexVar compute(Glyph glyph, HexContext hexContext) {
        HexVar a = glyph.resolveInput("a", hexContext);
        HexVar b = glyph.resolveInput("b", hexContext);
        if (a != null && b == null)
            return HexMathUtil.negate(a);
        if (a == null && b != null)
            return HexMathUtil.negate(b);

        if (a instanceof EntityVar && !(b instanceof EntityVar)) {
            Vector3d aPos = SpellVarUtil.resolveAsPosition(a, hexContext.getAccessor());
            a = new PositionVar(aPos, true);
        } else if (b instanceof EntityVar && !(a instanceof EntityVar)) {
            Vector3d bPos = SpellVarUtil.resolveAsPosition(b, hexContext.getAccessor());
            b = new PositionVar(bPos, true);
        }

        return HexMathUtil.subtract(a, b);
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar result = compute(glyph, hexContext);

        if (result != null) {
            Integer outputSlot = glyph.resolveOutput("result", hexContext);
            if (outputSlot != null)
                hexContext.setVariable(outputSlot, result);
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }

    @Override
    public HexVar getValue(Glyph glyph, HexContext hexContext) {
        return compute(glyph, hexContext);
    }
}
