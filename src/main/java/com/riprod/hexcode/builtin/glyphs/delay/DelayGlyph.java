package com.riprod.hexcode.builtin.glyphs.delay;

import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.glyphs.variables.SpellVar;
import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.components.Glyph;


public class DelayGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Delay";
    private static final int TICKS_PER_SECOND = 20;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext, ExecutionContext executionContext) {
        int varIndex = glyph.getNumber(1);

        int tickDelay = TICKS_PER_SECOND * 2; // default 2 second

        if (executionContext.getVariable(varIndex).size() > 0) {
            SpellVar var = executionContext.getVariable(varIndex).get(0);
            if (var instanceof NumberVar numberVar) {
                tickDelay = (int) (numberVar.number * TICKS_PER_SECOND);
            } else {
                LOGGER.atWarning().log("Delay glyph expected a number variable at index " + varIndex + ", but got " + var.getClass().getSimpleName());
            }
        }

        Executor.delayContinuation(hexContext, executionContext, tickDelay);
    }
}
