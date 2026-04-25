package com.riprod.hexcode.core.common.glyphs.component;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.riprod.hexcode.api.event.GlyphFizzleEvent;
import com.riprod.hexcode.api.event.HexCastEvent;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.registry.VolatilityAsset;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;

public interface GlyphHandler {
    HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    void execute(Glyph glyph, HexContext hexContext);

    String getId();

    default HexVar readValue(Glyph glyph, HexContext hexContext) {
        return hexContext.getVariable(glyph.getId());
    }

    /**
     * Per-glyph mana cost for upfront consumption. Called before any glyph
     * executes, so no slot variables are resolved yet. Default uses the
     * asset's ManaConsumption scaled by the glyph's efficiency. Handlers
     * with dynamic mana needs override.
     */
    default float collectMana(Glyph glyph, GlyphAsset asset) {
        if (asset == null) return 0f;
        return asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);
    }

    default boolean consumeVolatility(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null) return true;
        int repeatCount = tracker.getGlyphUsage(glyph.getId());
        float cost = VolatilityTracker.computeGlyphCost(glyph, repeatCount);
        if (cost <= 0) return true;
        boolean consumed = tracker.consumeVolatility(cost);
        if (!consumed) {
            HytaleServer.get().getEventBus().dispatchFor(GlyphFizzleEvent.class)
                .dispatch(new GlyphFizzleEvent(
                    glyph, GlyphFizzleEvent.Reason.VOLATILITY_DEPLETED, hexContext));
        }
        return consumed;
    }

    default boolean consumeResources(Glyph glyph, HexContext hexContext) {
        return consumeVolatility(glyph, hexContext);
    }

    // computes the area-tax multiplier for a glyph based on its actual magnitude
    // vs. the asset-declared default. ratio <= 1.0 returns 1.0 (no discount for
    // smaller-than-default casts).
    default float computeAreaScale(double magnitude, GlyphAsset asset) {
        if (asset == null) return 1.0f;
        VolatilityAsset.AreaTax tax = asset.getVolatility().getAreaTax();
        if (tax == null || tax.getDefaultMagnitude() <= 0.0f) return 1.0f;
        double ratio = magnitude / tax.getDefaultMagnitude();
        if (ratio <= 1.0) return 1.0f;
        return (float) Math.pow(ratio, tax.getExponent());
    }
}
