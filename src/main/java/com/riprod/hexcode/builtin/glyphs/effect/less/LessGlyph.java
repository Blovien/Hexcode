package com.riprod.hexcode.builtin.glyphs.effect.less;

import java.util.List;

import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.HexMathUtil;

public class LessGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Less";


    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar a = glyph.readSlot(LessGlyphSlots.A, hexContext);
        HexVar b = glyph.readSlot(LessGlyphSlots.B, hexContext);
        boolean result = HexMathUtil.isLess(a, b);

        List<String> next = glyph.getNextLinks();
        if (result) {
            if (!next.isEmpty()) {
                Executor.continueExecution(List.of(next.get(0)), hexContext);
            }
        } else {
            if (next.size() > 1) {
                Executor.continueExecution(next.subList(1, next.size()), hexContext);
            }
        }
    }
}
