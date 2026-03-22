package com.riprod.hexcode.core.common.glyphs.component;

import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.utils.GlyphType;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;

public interface GlyphHandler {
    HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    void execute(Glyph glyph, HexContext hexContext);

    default boolean canExecute(Glyph glyph, HexContext hexContext) {
        if (glyph.getType() != GlyphType.Effect) return true;
        return resolveVolatility(glyph, hexContext)
            && resolveMana(glyph, hexContext);
    }

    default boolean resolveVolatility(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null) return true;
        boolean passed = tracker.rollAndIncrement(glyph);
        if (!passed) {
            LOGGER.atInfo().log("glyph %s fizzled: rolled %.3f against %.3f chance (cast #%d, type count %d)",
                    glyph.getGlyphId(), tracker.getLastRoll(), tracker.getLastChance(),
                    tracker.getCastCount(),
                    tracker.getGlyphTypeCount(glyph.getGlyphId()));
        }
        return passed;
    }

    default boolean resolveMana(Glyph glyph, HexContext hexContext) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return true;

        float baseCost = asset.getManaConsumption()
            * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;
        float finalCost = baseCost * castMultiplier;

        boolean consumed = hexContext.getRoot().tryConsumeMana(finalCost, hexContext.getAccessor());
        if (!consumed) {
            float currentMana = hexContext.getRoot().getCurrentMana(hexContext.getAccessor());
            LOGGER.atInfo().log("glyph %s insufficient mana: needs %.1f, has %.1f (base %d, efficiency %.2f, multiplier %.2f)",
                    glyph.getGlyphId(), finalCost, currentMana,
                    asset.getManaConsumption(), glyph.getEfficiency(), castMultiplier);
        }
        return consumed;
    }
}
