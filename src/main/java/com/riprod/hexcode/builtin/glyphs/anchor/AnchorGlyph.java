package com.riprod.hexcode.builtin.glyphs.anchor;

import java.util.List;

import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.Glyph;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.SpellVar;

public class AnchorGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Anchor";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext, ExecutionContext executionContext) {
        int inputSlot = glyph.getVariable(1);
        List<SpellVar> targets = executionContext.getVariable(inputSlot);
        int outputSlot = glyph.getNumbers().containsKey(1) ? glyph.getNumber(1) : 2;

        executionContext.setVariable(outputSlot, targets);
        Executor.continueExecution(hexContext, executionContext);
    }
}
