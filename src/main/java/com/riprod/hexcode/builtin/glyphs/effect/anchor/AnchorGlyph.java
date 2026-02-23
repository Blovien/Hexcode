package com.riprod.hexcode.builtin.glyphs.effect.anchor;

import java.util.List;

import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.Glyph;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.HexVar;

public class AnchorGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Anchor";

    

    @Override
    public void execute(Glyph glyph, HexContext hexContext, ExecutionContext executionContext) {
        HexVar inputVar = glyph.getInput(0, executionContext, hexContext);
        int outputSlot = glyph.getOutputOrNumber(0);

        executionContext.setVariable(outputSlot, inputVar);
        Executor.continueExecution(hexContext, executionContext);
    }
}
