package com.riprod.hexcode.builtin.glyphs.effect.terraform;

import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.hypixel.hytale.logger.HytaleLogger;

public class TerraformGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Terraform";

    

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        LOGGER.atInfo().log("Casted Terraform");
        Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
