package com.riprod.hexcode.builtin.glyphs.effect.warp;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.utils.BlockUtils;
import com.riprod.hexcode.utils.SpellVarUtil;

public class WarpGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Warp";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.resolveInput("target", hexContext);
        HexVar destInput = glyph.resolveInput("destination", hexContext);

        if (destInput == null || destInput.size() == 0) {
            LOGGER.atWarning().log("warp glyph: no destination provided");
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        Vector3d destination = SpellVarUtil.resolvePosition(destInput, hexContext.getAccessor());

        if (destination == null) {
            LOGGER.atWarning().log("warp glyph: could not resolve destination");
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        if (targets == null || targets.size() == 0) {
            LOGGER.atWarning().log("warp glyph: no targets to warp");
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        World world = hexContext.getAccessor().getExternalData().getWorld();

        for (int i = 0; i < targets.size(); i++) {
            Vector3d departurePos = SpellVarUtil.resolvePositionAt(targets, i, hexContext.getAccessor());
            BlockUtils.moveToDestination(targets, i, destination, world, hexContext);
            if (departurePos != null) {
                WarpGlyphStyle.render(departurePos, destination, hexContext.getAccessor());
            }
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
