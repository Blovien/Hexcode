package com.riprod.hexcode.builtin.glyphs.effect.chaos;

import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.values.HexValInterface;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;

import java.util.concurrent.ThreadLocalRandom;

import com.hypixel.hytale.logger.HytaleLogger;

public class ChaosGlyph implements GlyphHandler, HexValInterface {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Chaos";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        Integer outVarIndex = glyph.resolveOutput("result", hexContext);
        HexVar minValVar = glyph.resolveInput("min", hexContext);
        HexVar maxValVar = glyph.resolveInput("max", hexContext);

        if (outVarIndex != null && minValVar != null && maxValVar != null) {
            int minVal = (int) Math.round(SpellVarUtil.resolveNumber(minValVar));
            int maxVal = (int) Math.round(SpellVarUtil.resolveNumber(maxValVar));
            int randomValue = ThreadLocalRandom.current().nextInt(minVal, maxVal + 1);
            hexContext.setVariable(outVarIndex, new NumberVar(randomValue));
            LOGGER.atInfo().log("chaos: range [%d, %d], result=%d -> slot %d",
                    minVal, maxVal, randomValue, outVarIndex);
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }

    @Override
    public HexVar getValue(Glyph glyph, HexContext hexContext) {
        HexVar minValVar = glyph.resolveInput("min", hexContext);
        HexVar maxValVar = glyph.resolveInput("max", hexContext);

        if (minValVar != null && maxValVar != null) {
            int minVal = (int) Math.round(SpellVarUtil.resolveNumber(minValVar));
            int maxVal = (int) Math.round(SpellVarUtil.resolveNumber(maxValVar));
            int randomValue = ThreadLocalRandom.current().nextInt(minVal, maxVal + 1);
            return new NumberVar(randomValue);
        }

        return new NumberVar(0);
    }
}
