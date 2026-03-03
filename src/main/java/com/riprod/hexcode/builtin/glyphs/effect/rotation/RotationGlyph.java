package com.riprod.hexcode.builtin.glyphs.effect.rotation;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3f;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.RotationVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;

public class RotationGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar varsA = glyph.getInput(0, hexContext);
        HexVar varsB = glyph.getInput(1, hexContext);
        HexVar varsC = glyph.getInput(2, hexContext);
        int outputSlot = glyph.getOutputOrNumber(0, hexContext);

        Double xVal = SpellVarUtil.resolveNumber(varsA);
        Float x = xVal != null ? xVal.floatValue() : null;
        Double yVal = SpellVarUtil.resolveNumber(varsB);
        Float y = yVal != null ? yVal.floatValue() : null;
        Double zVal = SpellVarUtil.resolveNumber(varsC);
        Float z = zVal != null ? zVal.floatValue() : null;

        if (x != null && y != null && z != null) {
            RotationVar rotationVar = new RotationVar(new Vector3f(x, y, z));
            hexContext.setVariable(outputSlot, rotationVar);
        }

        LOGGER.atInfo().log("Casted Rotation");
        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
