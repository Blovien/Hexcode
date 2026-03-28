package com.riprod.hexcode.builtin.glyphs.effect.warp;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.riprod.hexcode.builtin.glyphs.effect.warp.style.WarpStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.BlockUtils;
import com.riprod.hexcode.utils.SpellVarUtil;

public class WarpGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Warp";

    @Override
    public boolean resolveMana(Glyph glyph, HexContext hexContext) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return true;

        HexVar targets = glyph.resolveInput("target", hexContext);
        HexVar destInput = glyph.resolveInput("destination", hexContext);
        if (targets == null || destInput == null) {
            return true;
        }

        Vector3d destination = SpellVarUtil.resolvePosition(destInput, hexContext.getAccessor());
        if (destination == null) {
            return true;
        }

        Vector3d departurePos = SpellVarUtil.resolvePosition(targets, hexContext.getAccessor());
        if (departurePos == null) {
            return true;
        }

        double dx = destination.getX() - departurePos.getX();
        double dy = destination.getY() - departurePos.getY();
        double dz = destination.getZ() - departurePos.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        float baseCost = asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;
        float finalCost = (float) (baseCost * castMultiplier * distance);
        return hexContext.getRoot().tryConsumeMana(finalCost, hexContext.getAccessor());
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.resolveInput("target", hexContext);
        HexVar destInput = glyph.resolveInput("destination", hexContext);

        if (destInput == null) {
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

        if (targets == null) {
            LOGGER.atWarning().log("warp glyph: no targets to warp");
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        World world = hexContext.getAccessor().getExternalData().getWorld();

        Vector3d departurePos = SpellVarUtil.resolvePosition(targets, hexContext.getAccessor());
        BlockUtils.moveToDestination(targets, destination, world, hexContext);
        if (departurePos != null) {
            WarpStyle.render(departurePos, destination, hexContext.getColors(), hexContext.getAccessor());
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
