package com.riprod.hexcode.builtin.glyphs.effect.delay;

import com.riprod.hexcode.builtin.glyphs.effect.delay.style.DelayStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;

public class DelayGlyph implements GlyphHandler {
    public static final String ID = "Glyph_Delay";
    private static final int TICKS_PER_SECOND = 20;

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        double seconds = SpellVarUtil.resolveNumberOrDefault(glyph.resolveInput("duration", hexContext), 1.0);
        int tickDelay = (int) Math.round(seconds * TICKS_PER_SECOND);

        DelayStyle.render(hexContext);

        Executor.delayContinuation(glyph.getNext(), hexContext, tickDelay);
    }
}
