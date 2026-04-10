package com.riprod.hexcode.builtin.glyphs.effect.delay;

import com.riprod.hexcode.builtin.glyphs.effect.delay.style.DelayStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;

public class DelayGlyph implements GlyphHandler {
    public static final String ID = "Glyph_Delay";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        float seconds = SpellVarUtil.resolveNumberOrDefault(glyph.readSlot("duration", hexContext), 1.0).floatValue();

        DelayStyle.render(hexContext);

        Executor.delayFromSlot(glyph, Glyph.NEXT_SLOT, hexContext, seconds);
    }
}
