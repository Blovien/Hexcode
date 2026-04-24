package com.riprod.hexcode.builtin.glyphs.variable;

import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;

public class VariableValue implements GlyphHandler {

    @Override
    public String getId() {
        return ID;
    };

    public static final String ID = "Variable";

    // single source of truth for "what is my dereference target".
    // if the slot input resolves to a number, that number is the key.
    // otherwise the key is this Variable instance's own glyph UUID.
    private static String computeKey(Glyph glyph, HexContext hexContext) {
        HexVar slotInput = glyph.readSlot(VariableValueSlots.SLOT, hexContext);
        Double n = SpellVarUtil.resolveNumber(slotInput);
        if (n != null)
            return String.valueOf(n.intValue());
        return glyph.getId();
    }

    @Override
    public HexVar readValue(Glyph glyph, HexContext hexContext) {
        return hexContext.getVariable(computeKey(glyph, hexContext));
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
