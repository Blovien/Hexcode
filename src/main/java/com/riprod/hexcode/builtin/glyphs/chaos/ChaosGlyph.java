package com.riprod.hexcode.builtin.glyphs.chaos;

import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.components.Glyph;

import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.glyphs.variables.SpellVar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.hypixel.hytale.logger.HytaleLogger;

public class ChaosGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Chaos";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext, ExecutionContext executionContext) {
        int varIndex = glyph.getVariable(1);
        int minVal = glyph.getNumber(1);
        List<SpellVar> variable = executionContext.getVariable(varIndex);
        Boolean hasMaxVal = glyph.getNumbers().containsKey(2);

        if (hasMaxVal) {
            int maxVal = glyph.getNumber(2);
            int randomValue = ThreadLocalRandom.current().nextInt(minVal, maxVal + 1);
            executionContext.setVariable(varIndex, List.of(new NumberVar(randomValue)));
            LOGGER.atInfo().log("Casted Chaos with range [" + minVal + ", " + maxVal + "], set variable " + varIndex + " to " + randomValue);
        } else {
            if (minVal > 0 && variable.size() >= minVal) {
                List<SpellVar> shuffled = new ArrayList<>(variable);
                Collections.shuffle(shuffled);
                executionContext.setVariable(varIndex, shuffled.subList(0, minVal));
                LOGGER.atInfo().log("Casted Chaos selecting " + minVal + " random items from variable " + varIndex);
            } else {
                LOGGER.atInfo().log("Casted Chaos item-select mode skipped for variable " + varIndex + " (requested " + minVal + ", available " + variable.size() + ")");
            }
        }

        Executor.continueExecution(hexContext, executionContext);
    }
}
