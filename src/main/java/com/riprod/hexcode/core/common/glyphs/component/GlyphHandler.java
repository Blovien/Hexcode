package com.riprod.hexcode.core.common.glyphs.component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.riprod.hexcode.api.event.GlyphFizzleEvent;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphConfig;
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
        if (asset == null)
            return 0f;
        return asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);
    }

    default boolean consumeVolatility(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null)
            return true;
        int repeatCount = tracker.getGlyphUsage(glyph.getId());
        float cost = VolatilityTracker.computeGlyphCost(glyph, repeatCount);
        if (cost <= 0)
            return true;
        boolean consumed = tracker.consumeVolatility(cost);
        if (!consumed) {
            HytaleServer.get().getEventBus().dispatchFor(GlyphFizzleEvent.class)
                    .dispatch(new GlyphFizzleEvent(
                            glyph, GlyphFizzleEvent.Reason.VOLATILITY_DEPLETED, hexContext));
        }
        return consumed;
    }


    @Nullable
    default <T extends GlyphConfig> T getConfig(@Nonnull Class<T> type) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(getId());
        if (asset == null)
            return null;
        GlyphConfig config = asset.getConfig();
        return type.isInstance(config) ? type.cast(config) : null;
    }

    default float computeAreaScale(double magnitude, GlyphAsset asset) {
        if (asset == null)
            return 1.0f;
        VolatilityAsset.AreaTax tax = asset.getVolatility().getAreaTax();
        if (tax == null || tax.getDefaultMagnitude() <= 0.0f)
            return 1.0f;
        double ratio = magnitude / tax.getDefaultMagnitude();
        if (ratio <= 1.0)
            return 1.0f;
        return (float) Math.pow(ratio, tax.getExponent());
    }

    default ConfigBinding<? extends GlyphConfig> getConfigBinding() {
        return ConfigBinding.of(GlyphConfig.Default.class, GlyphConfig.Default.CODEC);
    }

    record ConfigBinding<T extends GlyphConfig>(Class<T> type, BuilderCodec<T> codec) {
        public static <T extends GlyphConfig> ConfigBinding<T> of(
                @Nonnull Class<T> type, @Nonnull BuilderCodec<T> codec) {
            return new ConfigBinding<>(type, codec);
        }
    }
}
