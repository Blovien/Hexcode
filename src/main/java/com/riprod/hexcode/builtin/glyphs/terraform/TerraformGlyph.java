package com.riprod.hexcode.builtin.glyphs.terraform;

import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.api.event.GlyphFizzleEvent;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class TerraformGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "Terraform";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        LOGGER.atWarning().log("terraform: not yet implemented");
        HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.NOT_IMPLEMENTED,
                "Terraform not yet implemented");
    }
}
