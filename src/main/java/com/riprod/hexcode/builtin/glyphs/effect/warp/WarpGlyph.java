package com.riprod.hexcode.builtin.glyphs.effect.warp;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.Glyph;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.HexVar;
import com.riprod.hexcode.core.glyphs.variables.PositionVar;
import com.riprod.hexcode.utils.BlockUtils;
import com.riprod.hexcode.utils.SpellVarUtil;

public class WarpGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Warp";

    @Override
    public void execute(Glyph glyph, HexContext hexContext, ExecutionContext executionContext) {
        HexVar targets = glyph.getInput(0, executionContext, hexContext);
        HexVar destInput = glyph.getInput(1, executionContext, hexContext);

        Vector3d destination = null;

        if (destInput != null) {
            destination = SpellVarUtil.resolvePosition(destInput, hexContext.accessor);
        }

        if (destination == null) {
            HexVar xInput = glyph.getInput(2, executionContext, hexContext);
            HexVar yInput = glyph.getInput(3, executionContext, hexContext);
            HexVar zInput = glyph.getInput(4, executionContext, hexContext);
            Double x = SpellVarUtil.resolveNumber(xInput);
            Double y = SpellVarUtil.resolveNumber(yInput);
            Double z = SpellVarUtil.resolveNumber(zInput);

            if (x != null && y != null && z != null) {
                destination = new Vector3d(x, y, z);
            }
        }

        if (destination == null) {
            LOGGER.atWarning().log("warp glyph: could not resolve destination");
            Executor.continueExecution(hexContext, executionContext);
            return;
        }

        if (targets == null || targets.size() == 0) {
            LOGGER.atWarning().log("warp glyph: no targets to warp");
            Executor.continueExecution(hexContext, executionContext);
            return;
        }

        World world = hexContext.accessor.getExternalData().getWorld();

        for (int i = 0; i < targets.size(); i++) {
            Vector3d departurePos = SpellVarUtil.resolvePositionAt(targets, i, hexContext.accessor);
            BlockUtils.moveToDestination(targets, i, destination, world, hexContext);
            if (departurePos != null) {
                WarpGlyphStyle.render(departurePos, destination, hexContext.accessor);
            }
        }

        Executor.continueExecution(hexContext, executionContext);
    }
}
