package com.riprod.hexcode.builtin.glyphs.effect.output;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.Color;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class OutputGlyph implements GlyphHandler {

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar colorInput = glyph.readSlot("color", hexContext);
        applyColor(colorInput, hexContext);
        // end-cap at craft time: Next is empty, continueFromSlot returns immediately.
        // Wave 3 drag-snap mutates Next to inject a child hex; this same call forwards into it.
        Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }

    private void applyColor(HexVar input, HexContext hexContext) {
        Color newColor = resolveAsColor(input);
        if (newColor == null) return;

        HexColors colors = hexContext.getColors();
        if (colors == null) {
            colors = new HexColors();
            hexContext.setColors(colors);
        }
        colors.setPrimaryColor(newColor);
    }

    private Color resolveAsColor(HexVar input) {
        if (input instanceof NumberVar numVar) {
            int v = clamp255((int) numVar.getValue());
            return makeColor(v, v, v);
        }
        if (input instanceof PositionVar posVar && posVar.getValue() != null) {
            Vector3d p = posVar.getValue();
            return makeColor(clamp255((int) p.x), clamp255((int) p.y), clamp255((int) p.z));
        }
        return null;
    }

    private static Color makeColor(int r, int g, int b) {
        return new Color((byte) r, (byte) g, (byte) b);
    }

    private static int clamp255(int v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return v;
    }
}
