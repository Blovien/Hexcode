package com.riprod.hexcode.builtin.glyphs.effect.anchor;

import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class AnchorGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Anchor";

    

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar inputVar = glyph.getInput(0, hexContext);
        int outputSlot = glyph.getOutputOrNumber(0, hexContext);

        if (inputVar != null) {
            hexContext.setVariable(outputSlot, inputVar);
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
