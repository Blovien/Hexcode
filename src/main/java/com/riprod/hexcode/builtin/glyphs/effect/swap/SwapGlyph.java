package com.riprod.hexcode.builtin.glyphs.effect.swap;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.riprod.hexcode.builtin.glyphs.effect.swap.style.SwapStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.BlockUtils;
import com.riprod.hexcode.utils.SpellVarUtil;

public class SwapGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Swap";

    @Override
    public boolean resolveMana(Glyph glyph, HexContext hexContext) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return true;

        HexVar varsA = glyph.resolveInput("a", hexContext);
        HexVar varsB = glyph.resolveInput("b", hexContext);
        int pairCount = (varsA != null && varsB != null)
                ? Math.min(varsA.size(), varsB.size()) : 1;

        float baseCost = asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;
        float finalCost = baseCost * castMultiplier * Math.max(1, pairCount);

        return hexContext.getRoot().tryConsumeMana(finalCost, hexContext.getAccessor());
    }

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
                SwapStyle.render(posA, posB, hexContext.getColors(), hexContext.getAccessor());
            }
            BlockUtils.swapPair(varsA, varsB, i, world, hexContext);
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
