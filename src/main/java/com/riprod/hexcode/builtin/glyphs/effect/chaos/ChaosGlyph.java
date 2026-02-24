package com.riprod.hexcode.builtin.glyphs.effect.chaos;

import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.execution.component.HexContext;
import com.riprod.hexcode.core.glyphs.component.Glyph;

import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.NumberVar;
import com.riprod.hexcode.utils.SpellVarUtil;
import com.riprod.hexcode.core.glyphs.variables.HexVar;

import java.util.concurrent.ThreadLocalRandom;

import com.hypixel.hytale.logger.HytaleLogger;

public class ChaosGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Chaos";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        int outVarIndex = glyph.getOutput(0, hexContext);
        HexVar variable = hexContext.getVariable(outVarIndex);
        HexVar minValVar = glyph.getInput(1, hexContext);
        HexVar maxValVar = glyph.getInput(2, hexContext);
        int minVal = (int) Math.round(SpellVarUtil.resolveNumber(minValVar));

        if (maxValVar != null) {
            int maxVal = (int) Math.round(SpellVarUtil.resolveNumber(maxValVar));
            int randomValue = ThreadLocalRandom.current().nextInt(minVal, (int) maxVal + 1);
            hexContext.setVariable(outVarIndex, new NumberVar(randomValue));
            LOGGER.atInfo().log("Casted Chaos with range [" + minVal + ", " + maxVal + "], set variable " + outVarIndex
                    + " to " + randomValue);
        } else {
            if (minVal > 0 && variable.size() >= minVal) {
                variable.shuffleAndTrim(minVal);
                LOGGER.atInfo().log("Casted Chaos selecting " + minVal + " random items from variable " + outVarIndex);
            } else {
                LOGGER.atInfo().log("Casted Chaos item-select mode skipped for variable " + outVarIndex + " (requested "
                        + minVal + ", available " + variable.size() + ")");
            }
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
