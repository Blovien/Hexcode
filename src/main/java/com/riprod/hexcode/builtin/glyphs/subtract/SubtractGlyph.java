package com.riprod.hexcode.builtin.glyphs.subtract;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.HexMathUtil;
import com.riprod.hexcode.utils.SpellVarUtil;

public class SubtractGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    @Override
public String getId() { return ID; };

public static final String ID = "Subtract";

    private HexVar compute(Glyph glyph, HexContext hexContext) {
        HexVar a = glyph.readSlot(SubtractGlyphSlots.A, hexContext);
        HexVar b = glyph.readSlot(SubtractGlyphSlots.B, hexContext);
        if (a != null && b == null)
            return HexMathUtil.negate(a);
        if (a == null && b != null)
            return HexMathUtil.negate(b);

        if (a instanceof EntityVar && !(b instanceof EntityVar)) {
            Vector3d aPos = SpellVarUtil.resolveAsPosition(a, hexContext.getAccessor());
            a = new PositionVar(aPos, true);
        } else if (b instanceof EntityVar && !(a instanceof EntityVar)) {
            Vector3d bPos = SpellVarUtil.resolveAsPosition(b, hexContext.getAccessor());
            b = new PositionVar(bPos, true);
        }

        return HexMathUtil.subtract(a, b);
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar result = compute(glyph, hexContext);

        if (result != null) {
            glyph.writeOutput(result, hexContext);
        }

        HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }

    @Override
    public HexVar readValue(Glyph glyph, HexContext hexContext) {
        return compute(glyph, hexContext);
    }
}
