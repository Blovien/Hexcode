package com.riprod.hexcode.builtin.glyphs.value;

import com.hypixel.hytale.math.vector.Vector3d;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;

public class PositionValue implements GlyphHandler {

    private HexVar compute(Glyph glyph, HexContext hexContext) {
        HexVar xVar = glyph.readSlot("x", hexContext);
        HexVar yVar = glyph.readSlot("y", hexContext);
        HexVar zVar = glyph.readSlot("z", hexContext);

        int count = (xVar != null ? 1 : 0) + (yVar != null ? 1 : 0) + (zVar != null ? 1 : 0);

        if (count == 1) {
            HexVar single = xVar != null ? xVar : (yVar != null ? yVar : zVar);
            if (SpellVarUtil.isVectorVar(single)) {
                Vector3d pos = SpellVarUtil.resolveAsPosition(single, hexContext.getAccessor());
                if (pos == null) return null;
                return new PositionVar(pos, single instanceof EntityVar);
            }
        }

        var accessor = hexContext.getAccessor();
        return new PositionVar(new Vector3d(
                SpellVarUtil.resolvePositionAxis(xVar, 0, accessor),
                SpellVarUtil.resolvePositionAxis(yVar, 1, accessor),
                SpellVarUtil.resolvePositionAxis(zVar, 2, accessor)));
    }

    @Override
    public boolean canReadValue() {
        return true;
    }

    @Override
    public HexVar readValue(Glyph glyph, HexContext hexContext) {
        return compute(glyph, hexContext);
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar result = compute(glyph, hexContext);

        if (result != null) {
            glyph.writeSlot("result", result, hexContext);
        }

        Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
