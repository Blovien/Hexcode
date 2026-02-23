package com.riprod.hexcode.builtin.glyphs.effect.swap;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.Glyph;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.variables.HexVar;
import com.riprod.hexcode.utils.BlockUtils;
import com.riprod.hexcode.utils.SpellVarUtil;

public class SwapGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Swap";

    @Override
    public void execute(Glyph glyph, HexContext hexContext, ExecutionContext executionContext) {
        HexVar varsA = glyph.getInput(0, executionContext, hexContext);
        HexVar varsB = glyph.getInput(1, executionContext, hexContext);

        World world = hexContext.accessor.getExternalData().getWorld();

        int pairCount = Math.min(varsA.size(), varsB.size());
        for (int i = 0; i < pairCount; i++) {
            Vector3d posA = SpellVarUtil.resolvePositionAt(varsA, i, hexContext.accessor);
            Vector3d posB = SpellVarUtil.resolvePositionAt(varsB, i, hexContext.accessor);
            if (posA != null && posB != null) {
                SwapGlyphStyle.render(posA, posB, hexContext.accessor);
            }
            BlockUtils.swapPair(varsA, varsB, i, world, hexContext);
        }

        Executor.continueExecution(hexContext, executionContext);
    }
}
