package com.riprod.hexcode.builtin.glyphs.value;

import com.hypixel.hytale.math.vector.Vector3d;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.values.HexValInterface;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;

public class PositionValue implements GlyphHandler, HexValInterface {

    private HexVar compute(Glyph glyph, HexContext hexContext) {
        HexVar xVar = glyph.resolveInput("x", hexContext);
        HexVar yVar = glyph.resolveInput("y", hexContext);
        HexVar zVar = glyph.resolveInput("z", hexContext);

        int count = (xVar != null ? 1 : 0) + (yVar != null ? 1 : 0) + (zVar != null ? 1 : 0);

        if (count == 1) {
            HexVar single = xVar != null ? xVar : (yVar != null ? yVar : zVar);
            if (SpellVarUtil.isVectorVar(single)) {
                Vector3d pos = SpellVarUtil.resolveAsPosition(single, hexContext.getAccessor());
                return pos != null ? new PositionVar(pos) : null;
            }
        }

        var accessor = hexContext.getAccessor();
        return new PositionVar(new Vector3d(
                SpellVarUtil.resolvePositionAxis(xVar, 0, accessor),
                SpellVarUtil.resolvePositionAxis(yVar, 1, accessor),
                SpellVarUtil.resolvePositionAxis(zVar, 2, accessor)));
    }

    @Override
    public HexVar getValue(Glyph glyph, HexContext hexContext) {
        return compute(glyph, hexContext);
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar result = compute(glyph, hexContext);

        if (result != null) {
            Integer outputSlot = glyph.resolveOutput("result", hexContext);
            if (outputSlot != null) hexContext.setVariable(outputSlot, result);
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
