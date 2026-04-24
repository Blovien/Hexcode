package com.riprod.hexcode.core.common.glyphs.component;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.riprod.hexcode.api.event.GlyphFizzleEvent;
import com.riprod.hexcode.api.event.HexCastEvent;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;

public interface GlyphHandler {
    HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    void execute(Glyph glyph, HexContext hexContext);

    String getId();

    default HexVar readValue(Glyph glyph, HexContext hexContext) {
        return null;
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
        float cost = VolatilityTracker.computeGlyphCost(glyph);
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
}
