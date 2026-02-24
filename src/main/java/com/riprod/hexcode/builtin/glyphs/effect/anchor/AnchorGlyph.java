package com.riprod.hexcode.builtin.glyphs.effect.anchor;

import java.util.List;

import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.glyphs.component.Glyph;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.execution.component.HexContext;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.HexVar;

public class AnchorGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Anchor";

    

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar inputVar = glyph.getInput(0, hexContext);
        int outputSlot = glyph.getOutputOrNumber(0, hexContext);

        hexContext.setVariable(outputSlot, inputVar);
        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
