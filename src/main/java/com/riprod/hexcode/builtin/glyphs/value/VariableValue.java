package com.riprod.hexcode.builtin.glyphs.value;

import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;

public class VariableValue implements GlyphHandler {

    @Override
    public boolean canResolveValue() {
        return true;
    }

    @Override
    public HexVar resolveValue(Glyph glyph, HexContext hexContext) {
        Double slot = SpellVarUtil.resolveNumber(glyph.resolveSlot("slot", hexContext));
        if (slot == null) return null;
        return hexContext.getVariable(slot.intValue());
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
