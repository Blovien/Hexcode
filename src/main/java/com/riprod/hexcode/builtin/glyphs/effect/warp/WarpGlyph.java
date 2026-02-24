package com.riprod.hexcode.builtin.glyphs.effect.warp;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.riprod.hexcode.core.glyphs.component.Glyph;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.execution.component.HexContext;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.HexVar;
import com.riprod.hexcode.core.glyphs.variables.PositionVar;
import com.riprod.hexcode.utils.BlockUtils;
import com.riprod.hexcode.utils.SpellVarUtil;

public class WarpGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Warp";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.getInput(0, hexContext);
        HexVar destInput = glyph.getInput(1, hexContext);

        Vector3d destination = null;

        if (destInput != null) {
            destination = SpellVarUtil.resolvePosition(destInput, hexContext.getAccessor());
        }

        if (destination == null) {
            HexVar xInput = glyph.getInput(2, hexContext);
            HexVar yInput = glyph.getInput(3, hexContext);
            HexVar zInput = glyph.getInput(4, hexContext);
            Double x = SpellVarUtil.resolveNumber(xInput);
            Double y = SpellVarUtil.resolveNumber(yInput);
            Double z = SpellVarUtil.resolveNumber(zInput);

            if (x != null && y != null && z != null) {
                destination = new Vector3d(x, y, z);
            }
        }

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
