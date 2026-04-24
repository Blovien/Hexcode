package com.riprod.hexcode.core.state.execution.component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
    private float magicPowerMultiplier;
    private UUID executionId;
    private Map<String, Integer> glyphUsageMap = new HashMap<>();

    public VolatilityTracker(float startingBudget, float volatilityMultiplier) {
        this(startingBudget, volatilityMultiplier, 1.0f);
    }

    public VolatilityTracker(float startingBudget, float volatilityMultiplier,
            float magicPowerMultiplier) {
        this.startingBudget = startingBudget;
        this.remainingBudget = startingBudget;
        this.volatilityMultiplier = volatilityMultiplier;
        this.magicPowerMultiplier = magicPowerMultiplier;
    }

    public VolatilityTracker() {
        this.startingBudget = 0f;
        this.remainingBudget = 0f;
        this.volatilityMultiplier = 1.0f;
        this.magicPowerMultiplier = 1.0f;
    }

    public static float computeGlyphCost(Glyph glyph) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null)
            return 0;
        float baseCost = asset.getVolatilityCost();
        // perfect draw (1.0) = 0.5x cost, worst draw (0.0) = 1.0x cost
        float qualityFactor = (1 - glyph.getVolatility()) * 0.5f + 0.5f;
        return baseCost * qualityFactor;
    }

    public boolean consumeVolatility(float cost) {
        float finalCost = cost * volatilityMultiplier;
        if (finalCost <= 0)
            return true;
        float drain = finalCost <= 1f
                ? finalCost
                : 1f + ThreadLocalRandom.current().nextFloat() * (finalCost - 1f);
        remainingBudget -= drain;
        if (remainingBudget <= 0) {
            remainingBudget = 0;
            return false;
        }
        return true;
    }

    public float getStartingBudget() {
        return startingBudget;
    }

    public void setStartingBudget(float startingBudget) {
        this.startingBudget = startingBudget;
    }

    public float getRemainingBudget() {
        return remainingBudget;
    }

    public void setBudget(float budget) {
        this.remainingBudget = Math.max(0f, budget);
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

    public int getGlyphUsage(String glyphId) {
        return glyphUsageMap.getOrDefault(glyphId, 0);
    }

    public float getGlyphUsageScaled(String glyphId) {
        int usage = getGlyphUsage(glyphId);
        float k = 5.0f;
        float usageScale = 1.0f + (usage / (usage + k));
        return usageScale;
    }

    public int incrementGlyphUsage(String glyphId) {
        int usage = getGlyphUsage(glyphId) + 1;
        glyphUsageMap.put(glyphId, usage);
        return usage;
    }

    public void setExecutionId(UUID executionId) {
        this.executionId = executionId;
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
            .append(new KeyedCodec<>("MagicPowerMultiplier", Codec.FLOAT),
                    (c, v) -> c.magicPowerMultiplier = v,
                    (c) -> c.magicPowerMultiplier)
            .add()
            .append(new KeyedCodec<>("ExecutionId", Codec.UUID_STRING),
                    (c, v) -> c.executionId = v,
                    (c) -> c.executionId)
            .add()
            .build();

    VolatilityTracker copy() {
        VolatilityTracker copy = new VolatilityTracker();
        copy.startingBudget = this.startingBudget;
        copy.remainingBudget = this.remainingBudget;
        copy.volatilityMultiplier = this.volatilityMultiplier;
        copy.magicPowerMultiplier = this.magicPowerMultiplier;
        copy.executionId = this.executionId;
        return copy;
    }
}
