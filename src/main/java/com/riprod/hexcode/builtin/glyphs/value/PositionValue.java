package com.riprod.hexcode.builtin.glyphs.value;

import com.hypixel.hytale.math.vector.Vector3d;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.values.HexValInterface;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;

public class PositionValue implements HexValInterface {

    @Override
    public HexVar getValue(Glyph glyph, HexContext hexContext) {
        Double x = SpellVarUtil.resolveNumber(glyph.resolveInput("x", hexContext));
        Double y = SpellVarUtil.resolveNumber(glyph.resolveInput("y", hexContext));
        Double z = SpellVarUtil.resolveNumber(glyph.resolveInput("z", hexContext));
        if (x == null) x = 0.0;
        if (y == null) y = 0.0;
        if (z == null) z = 0.0;
        return new PositionVar(new Vector3d(x, y, z));
    }
}
