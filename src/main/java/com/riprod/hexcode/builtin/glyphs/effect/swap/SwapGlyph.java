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

        HexVar varsA = glyph.readSlot("a", hexContext);
        HexVar varsB = glyph.readSlot("b", hexContext);
        if (varsA == null || varsB == null) {
            return true;
        }

        Vector3d posA = SpellVarUtil.resolvePosition(varsA, hexContext.getAccessor());
        Vector3d posB = SpellVarUtil.resolvePosition(varsB, hexContext.getAccessor());
        if (posA == null || posB == null) {
            return true;
        }

        double dx = posA.getX() - posB.getX();
        double dy = posA.getY() - posB.getY();
        double dz = posA.getZ() - posB.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz) * 2.0;

        float baseCost = asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;
        float finalCost = (float) (baseCost * castMultiplier * distance);

        return hexContext.getRoot().tryConsumeMana(finalCost, hexContext.getAccessor());
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar varsA = glyph.readSlot("a", hexContext);
        HexVar varsB = glyph.readSlot("b", hexContext);

        if (varsA == null || varsB == null) {
            LOGGER.atWarning().log("swap glyph: missing input variables");
            Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        World world = hexContext.getAccessor().getExternalData().getWorld();

        Vector3d posA = SpellVarUtil.resolvePosition(varsA, hexContext.getAccessor());
        Vector3d posB = SpellVarUtil.resolvePosition(varsB, hexContext.getAccessor());
        if (posA != null && posB != null) {
            SwapStyle.render(posA, posB, hexContext.getColors(), hexContext.getAccessor());
        }
        BlockUtils.swapPair(varsA, varsB, world, hexContext);

        Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
