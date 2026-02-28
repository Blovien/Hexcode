package com.riprod.hexcode.builtin.glyphs.effect.position;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.execution.component.HexContext;
import com.riprod.hexcode.core.glyphs.component.Glyph;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.HexVar;
import com.riprod.hexcode.core.glyphs.variables.PositionVar;
import com.riprod.hexcode.utils.SpellVarUtil;

public class PositionGlyph implements GlyphHandler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar varsA = glyph.getInput(0, hexContext);
        HexVar varsB = glyph.getInput(1, hexContext);
        HexVar varsC = glyph.getInput(2, hexContext);
        int outputSlot = glyph.getOutputOrNumber(0, hexContext);

        Double x = SpellVarUtil.resolveNumber(varsA);
        Double y = SpellVarUtil.resolveNumber(varsB);
        Double z = SpellVarUtil.resolveNumber(varsC);

        PositionVar positionVar = new PositionVar(new Vector3d(x, y, z));

        hexContext.setVariable(outputSlot, positionVar);

        LOGGER.atInfo().log("Casted Position");
        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
