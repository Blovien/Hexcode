package com.riprod.hexcode.builtin.glyphs.effect.chaos;

import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;

import java.util.concurrent.ThreadLocalRandom;

import com.hypixel.hytale.logger.HytaleLogger;

public class ChaosGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Chaos";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar minValVar = glyph.readSlot("min", hexContext);
        HexVar maxValVar = glyph.readSlot("max", hexContext);

        if (minValVar != null && maxValVar != null) {
            int minVal = (int) Math.round(SpellVarUtil.resolveNumber(minValVar));
            int maxVal = (int) Math.round(SpellVarUtil.resolveNumber(maxValVar));
            int randomValue = ThreadLocalRandom.current().nextInt(minVal, maxVal + 1);
            glyph.writeSlot("result", new NumberVar(randomValue), hexContext);
            LOGGER.atInfo().log("chaos: range [%d, %d], result=%d", minVal, maxVal, randomValue);
        }

        Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }

    @Override
    public boolean canReadValue() {
        return true;
    }

    @Override
    public HexVar readValue(Glyph glyph, HexContext hexContext) {
        HexVar minValVar = glyph.readSlot("min", hexContext);
        HexVar maxValVar = glyph.readSlot("max", hexContext);

        if (minValVar == null || maxValVar == null) return new NumberVar(0);

        int minVal = (int) Math.round(SpellVarUtil.resolveNumber(minValVar));
        int maxVal = (int) Math.round(SpellVarUtil.resolveNumber(maxValVar));
        int randomValue = ThreadLocalRandom.current().nextInt(minVal, maxVal + 1);
        return new NumberVar(randomValue);
    }
}
