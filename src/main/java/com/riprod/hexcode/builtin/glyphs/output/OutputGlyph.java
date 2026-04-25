package com.riprod.hexcode.builtin.glyphs.output;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.Color;
import com.riprod.hexcode.api.event.GlyphFizzleEvent;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.NumberVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class OutputGlyph implements GlyphHandler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "Output";

    @Override
    public String getId() {
        return ID;
    };

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        // color is best-effort cosmetic; forwarding is the structural contract.
        // splice/mixin usage (Interfere/Resonate) wires Output without a color input,
        // so we must never let a missing/invalid color abort the chain.
        HexVar colorInput = glyph.readSlot(OutputGlyphSlots.COLOR, hexContext);
        applyColor(colorInput, hexContext);
        HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
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
            int v = clamp255(numVar.getValue().intValue());
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
