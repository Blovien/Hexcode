package com.riprod.hexcode.builtin.glyphs.warp;

import java.util.List;

import javax.annotation.Nullable;

import com.hypixel.hytale.builtin.hytalegenerator.fields.FastNoiseLite.Vector3;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.riprod.hexcode.builtin.utils.BlockUtils;
import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.Glyph;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.utils.SpellVarUtil;
import com.riprod.hexcode.core.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.glyphs.variables.SpellVar;

/**
 * First number = target
 * Second number = position
 * 3rd, 4th number present = x, y, z coordinates
 * 
 */
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
            BlockUtils.moveToDestination(target, destination, world, hexContext);
        }

        Executor.continueExecution(hexContext, executionContext);
    }
}
