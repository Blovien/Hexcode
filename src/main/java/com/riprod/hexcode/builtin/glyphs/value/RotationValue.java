package com.riprod.hexcode.builtin.glyphs.value;

import com.hypixel.hytale.math.vector.Vector3f;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.values.HexValInterface;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.RotationVar;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.SpellVarUtil;

public class RotationValue implements HexValInterface {

    @Override
    public HexVar getValue(Glyph glyph, HexContext hexContext) {
        Double x = SpellVarUtil.resolveNumber(glyph.resolveInput("x", hexContext));
        Double y = SpellVarUtil.resolveNumber(glyph.resolveInput("y", hexContext));
        Double z = SpellVarUtil.resolveNumber(glyph.resolveInput("z", hexContext));
        if (x == null || y == null || z == null) return null;
        return new RotationVar(new Vector3f(x.floatValue(), y.floatValue(), z.floatValue()));
    }
}
