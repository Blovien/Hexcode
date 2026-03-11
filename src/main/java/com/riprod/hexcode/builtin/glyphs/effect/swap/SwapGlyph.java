package com.riprod.hexcode.builtin.glyphs.effect.swap;

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

public class SwapGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Swap";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar varsA = glyph.resolveInput("a", hexContext);
        HexVar varsB = glyph.resolveInput("b", hexContext);

        if (varsA == null || varsB == null) {
            LOGGER.atWarning().log("swap glyph: missing input variables");
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        World world = hexContext.getAccessor().getExternalData().getWorld();

        int pairCount = Math.min(varsA.size(), varsB.size());
        for (int i = 0; i < pairCount; i++) {
            Vector3d posA = SpellVarUtil.resolvePositionAt(varsA, i, hexContext.getAccessor());
            Vector3d posB = SpellVarUtil.resolvePositionAt(varsB, i, hexContext.getAccessor());
            if (posA != null && posB != null) {
                SwapGlyphStyle.render(posA, posB, hexContext.getAccessor());
            }
            BlockUtils.swapPair(varsA, varsB, i, world, hexContext);
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
