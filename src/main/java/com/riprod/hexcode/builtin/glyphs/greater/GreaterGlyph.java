package com.riprod.hexcode.builtin.glyphs.greater;

import java.util.List;

import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.HexCompareUtil;

public class GreaterGlyph implements GlyphHandler {
    @Override
public String getId() { return ID; }

public static final String ID = "Greater";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar a = glyph.readSlot(GreaterGlyphSlots.A, hexContext);
        HexVar b = glyph.readSlot(GreaterGlyphSlots.B, hexContext);
        boolean result = HexCompareUtil.isGreater(a, b);

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
