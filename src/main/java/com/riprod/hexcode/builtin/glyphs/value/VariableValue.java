package com.riprod.hexcode.builtin.glyphs.value;

import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.values.HexValInterface;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;

public class VariableValue implements HexValInterface {

    @Override
    public HexVar getValue(Glyph glyph, HexContext hexContext) {
        Double slot = SpellVarUtil.resolveNumber(glyph.resolveInput("slot", hexContext));
        if (slot == null) return null;
        return hexContext.getVariable(slot.intValue());
    }
}
