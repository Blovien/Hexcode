package com.riprod.hexcode.builtin.glyphs.chaos;

import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;

import java.util.concurrent.ThreadLocalRandom;

import com.hypixel.hytale.logger.HytaleLogger;

public class ChaosGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Override
public String getId() { return ID; };

public static final String ID = "Chaos";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar minValVar = glyph.readSlot(ChaosGlyphSlots.MIN, hexContext);
        HexVar maxValVar = glyph.readSlot(ChaosGlyphSlots.MAX, hexContext);

        if (minValVar != null && maxValVar != null) {
            int minVal = (int) Math.round(SpellVarUtil.resolveNumber(minValVar));
            int maxVal = (int) Math.round(SpellVarUtil.resolveNumber(maxValVar));
            int randomValue = ThreadLocalRandom.current().nextInt(minVal, maxVal + 1);
            glyph.writeOutput(new NumberVar(randomValue), hexContext);
            LOGGER.atInfo().log("chaos: range [%d, %d], result=%d", minVal, maxVal, randomValue);
        }

        HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }

    @Override
    public HexVar readValue(Glyph glyph, HexContext hexContext) {
        HexVar minValVar = glyph.readSlot(ChaosGlyphSlots.MIN, hexContext);
        HexVar maxValVar = glyph.readSlot(ChaosGlyphSlots.MAX, hexContext);

        if (minValVar == null || maxValVar == null) return new NumberVar(0);

        int minVal = (int) Math.round(SpellVarUtil.resolveNumber(minValVar));
        int maxVal = (int) Math.round(SpellVarUtil.resolveNumber(maxValVar));
        int randomValue = ThreadLocalRandom.current().nextInt(minVal, maxVal + 1);
        return new NumberVar(randomValue);
    }
}
