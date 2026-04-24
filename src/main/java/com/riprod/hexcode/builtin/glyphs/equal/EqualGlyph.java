package com.riprod.hexcode.builtin.glyphs.equal;

import java.util.List;

import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.HexMathUtil;

public class EqualGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Override
public String getId() { return ID; };

public static final String ID = "Equal";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar a = glyph.readSlot(EqualGlyphSlots.A, hexContext);
        HexVar b = glyph.readSlot(EqualGlyphSlots.B, hexContext);

        if (b == null) {
            assign(glyph, hexContext, a);
            return;
        }

        branch(glyph, hexContext, a, b);
    }

    private void assign(Glyph glyph, HexContext hexContext, HexVar value) {
        if (value != null) {
            glyph.writeOutput(value, hexContext);
        }
        HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }

    private void branch(Glyph glyph, HexContext hexContext, HexVar a, HexVar b) {
        boolean result = HexMathUtil.isEqual(a, b);

        List<String> next = glyph.getNextLinks();
        if (result) {
            if (!next.isEmpty()) {
                HexExecuter.continueExecution(List.of(next.get(0)), hexContext);
            }
        } else {
            if (next.size() > 1) {
                HexExecuter.continueExecution(next.subList(1, next.size()), hexContext);
            }
        }
    }
}
