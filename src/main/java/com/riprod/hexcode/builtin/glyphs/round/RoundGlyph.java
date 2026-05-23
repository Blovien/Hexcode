package com.riprod.hexcode.builtin.glyphs.round;

import org.joml.Vector3d;
import org.joml.Vector3f;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.common.glyphs.variables.RotationVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class RoundGlyph implements GlyphHandler {
    public static final String ID = "Round";

    @Override
    public String getId() {
        return ID;
    }

    private static double op(double v) {
        return (double) Math.round(v);
    }

    private HexVar compute(Glyph glyph, HexContext hexContext) {
        HexVar a = glyph.readSlot(RoundGlyphSlots.A, hexContext);
        if (a == null) return null;
        return switch (a) {
            case NumberVar n -> {
                Double v = n.getValue();
                yield new NumberVar(v == null ? 0.0 : op(v));
            }
            case PositionVar p -> {
                Vector3d v = p.getValue();
                yield v == null
                        ? p
                        : new PositionVar(new Vector3d(op(v.x), op(v.y), op(v.z)), p.isAbsolute());
            }
            case RotationVar r -> {
                Vector3f v = r.getValue();
                yield v == null
                        ? r
                        : new RotationVar(new Vector3f((float) op(v.x), (float) op(v.y), (float) op(v.z)));
            }
            default -> a;
        };
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
