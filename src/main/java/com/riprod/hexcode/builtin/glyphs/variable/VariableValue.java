package com.riprod.hexcode.builtin.glyphs.variable;

import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class VariableValue implements GlyphHandler {

    @Override
    public String getId() {
        return ID;
    };

    public static final String ID = "Variable";

    @Override
    public HexVar readValue(Glyph glyph, HexContext hexContext) {
        return hexContext.getVariable(glyph.getId());
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar var = glyph.readSlot(VariableValueSlots.TARGET, hexContext);

        hexContext.setVariable(Glyph.DEFAULT_SLOT, var);
        hexContext.setVariable(glyph.getId(), var);
        HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
