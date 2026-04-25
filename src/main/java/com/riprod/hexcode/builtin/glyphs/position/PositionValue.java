package com.riprod.hexcode.builtin.glyphs.position;

import com.hypixel.hytale.math.vector.Vector3d;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.common.glyphs.variables.RotationVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.HexDirectionUtil;
import com.riprod.hexcode.utils.HexVarUtil;

public class PositionValue implements GlyphHandler {
    public static final String ID = "Position";

    @Override
    public String getId() {
        return ID;
    };

    private HexVar compute(Glyph glyph, HexContext hexContext) {
        HexVar xVar = glyph.readSlot(PositionValueSlots.X, hexContext);
        HexVar yVar = glyph.readSlot(PositionValueSlots.Y, hexContext);
        HexVar zVar = glyph.readSlot(PositionValueSlots.Z, hexContext);

        var accessor = hexContext.getAccessor();
        return new PositionVar(new Vector3d(
                HexVarUtil.positionAxis(xVar, 0, accessor),
                HexVarUtil.positionAxis(yVar, 1, accessor),
                HexVarUtil.positionAxis(zVar, 2, accessor)));
    }

    @Override
    public HexVar readValue(Glyph glyph, HexContext hexContext) {
        return compute(glyph, hexContext);
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar result = compute(glyph, hexContext);

        if (result != null) {
            glyph.writeOutput(result, hexContext);
        }

        HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
