package com.riprod.hexcode.core.state.execution.component;

import java.util.concurrent.ThreadLocalRandom;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;

public class VolatilityTracker {
    private float startingBudget;
    private float remainingBudget;
    private float volatilityMultiplier;
    private float manaCostMultiplier;
    private float magicPowerMultiplier;
    private volatile boolean fizzled;
    private volatile boolean failed;

    public VolatilityTracker(float startingBudget, float volatilityMultiplier, float manaCostMultiplier) {
        this(startingBudget, volatilityMultiplier, manaCostMultiplier, 1.0f);
    }

    public VolatilityTracker(float startingBudget, float volatilityMultiplier,
            float manaCostMultiplier, float magicPowerMultiplier) {
        this.startingBudget = startingBudget;
        this.remainingBudget = startingBudget;
        this.volatilityMultiplier = volatilityMultiplier;
        this.manaCostMultiplier = manaCostMultiplier;
        this.magicPowerMultiplier = magicPowerMultiplier;
    }

    public VolatilityTracker() {
        this.startingBudget = 0f;
        this.remainingBudget = 0f;
        this.volatilityMultiplier = 1.0f;
        this.manaCostMultiplier = 1.0f;
        this.magicPowerMultiplier = 1.0f;
    }

    public static float computeGlyphCost(Glyph glyph) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return 0;
        float baseCost = asset.getVolatilityCost();
        // perfect draw (1.0) = 0.5x cost, worst draw (0.0) = 1.0x cost
        float qualityFactor = (1 - glyph.getVolatility()) * 0.5f + 0.5f;
        return baseCost * qualityFactor;
    }

    public boolean consumeVolatility(float cost) {
        if (fizzled) return false;
        float finalCost = cost * volatilityMultiplier;
        if (finalCost <= 0) return true;
        float drain = finalCost <= 1f
                ? finalCost
                : 1f + ThreadLocalRandom.current().nextFloat() * (finalCost - 1f);
        remainingBudget -= drain;
        if (remainingBudget <= 0) {
            remainingBudget = 0;
            fizzled = true;
            return false;
        }
        return true;
    }

    public float getStartingBudget() {
        return startingBudget;
    }

    public float getRemainingBudget() {
        return remainingBudget;
    }

    public float getManaCostMultiplier() {
        return manaCostMultiplier;
    }

    public float getVolatilityMultiplier() {
        return volatilityMultiplier;
    }

    public void setVolatilityMultiplier(float volatilityMultiplier) {
        this.volatilityMultiplier = volatilityMultiplier;
    }

    public float getMagicPowerMultiplier() {
        return magicPowerMultiplier;
    }

    public void setMagicPowerMultiplier(float magicPowerMultiplier) {
        this.magicPowerMultiplier = magicPowerMultiplier;
    }

    public boolean isFizzled() {
        return fizzled;
    }

    public void setFizzled(boolean fizzled) {
        this.fizzled = fizzled;
    }

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public static final BuilderCodec<VolatilityTracker> CODEC = BuilderCodec
            .builder(VolatilityTracker.class, VolatilityTracker::new)
            .append(new KeyedCodec<>("StartingBudget", Codec.FLOAT),
                    (c, v) -> c.startingBudget = v,
                    (c) -> c.startingBudget)
            .add()
            .append(new KeyedCodec<>("RemainingBudget", Codec.FLOAT),
                    (c, v) -> c.remainingBudget = v,
                    (c) -> c.remainingBudget)
            .add()
            .append(new KeyedCodec<>("VolatilityMultiplier", Codec.FLOAT),
                    (c, v) -> c.volatilityMultiplier = v,
                    (c) -> c.volatilityMultiplier)
            .add()
            .append(new KeyedCodec<>("ManaCostMultiplier", Codec.FLOAT),
                    (c, v) -> c.manaCostMultiplier = v,
                    (c) -> c.manaCostMultiplier)
            .add()
            .append(new KeyedCodec<>("MagicPowerMultiplier", Codec.FLOAT),
                    (c, v) -> c.magicPowerMultiplier = v,
                    (c) -> c.magicPowerMultiplier)
            .add()
            .build();
}
