package com.riprod.hexcode.builtin.glyphs.warp;

import java.util.List;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.Glyph;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.utils.SpellVarUtil;
import com.riprod.hexcode.core.glyphs.variables.SpellVar;
import com.riprod.hexcode.utils.BlockUtils;

public class WarpGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Warp";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext, ExecutionContext executionContext) {
        boolean hasCoords = glyph.getNumbers().containsKey(2) && glyph.getNumbers().containsKey(3)
                && glyph.getNumbers().containsKey(4);

        Vector3d destination = null;

        if (!hasCoords) {
            List<SpellVar> targets = executionContext.getVariable(glyph.getNumber(2));
            // check if it is a position variable
            destination = SpellVarUtil.resolvePosition(targets, hexContext.accessor);

            if (destination == null) {
                LOGGER.atWarning().log("Warp glyph: could not resolve target position");
                Executor.continueExecution(hexContext, executionContext);
                return;
            }

        } else {
            try {

                destination = new Vector3d(glyph.getNumber(2), glyph.getNumber(3), glyph.getNumber(4));
            } catch (Exception e) {
                LOGGER.atWarning().log("Warp glyph: invalid coordinates: " + e.getMessage());
                Executor.continueExecution(hexContext, executionContext);
                return;
            }
        }

        int targetSlot = glyph.getNumber(1);

        List<SpellVar> targets = executionContext.getVariable(targetSlot);
        if (targets.isEmpty()) {
            LOGGER.atWarning().log("Warp glyph: no targets to warp");
            Executor.continueExecution(hexContext, executionContext);
            return;
        }

        World world = hexContext.accessor.getExternalData().getWorld();

        for (SpellVar target : targets) {
            LOGGER.atInfo().log("Warping target to " + destination);
            Vector3d departurePos = SpellVarUtil.resolvePosition(List.of(target), hexContext.accessor);
            BlockUtils.moveToDestination(target, destination, world, hexContext);
            if (departurePos != null) {
                WarpGlyphStyle.render(departurePos, destination, hexContext.accessor);
            }
        }

        Executor.continueExecution(hexContext, executionContext);
    }
}
