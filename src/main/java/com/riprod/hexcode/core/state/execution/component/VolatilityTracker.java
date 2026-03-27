package com.riprod.hexcode.core.state.execution.component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;

public class VolatilityTracker {
    private Map<String, Integer> glyphTypeCounts = new HashMap<>();
    private Integer castCount;
    private Float staffModifier;
    private Float powerModifier;
    private Float volatilityMultiplier;
    private Float manaCostMultiplier;
    private Float lastChance;
    private Float lastRoll;
    private volatile boolean fizzled;

    public VolatilityTracker(int castCount, float staffModifier,
            float powerModifier, float volatilityMultiplier, float manaCostMultiplier) {
        this.castCount = castCount;
        this.staffModifier = Math.max(staffModifier, 0.1f);
        this.powerModifier = powerModifier;
        this.volatilityMultiplier = volatilityMultiplier;
        this.manaCostMultiplier = manaCostMultiplier;
    }

    public VolatilityTracker() {
        this.castCount = 0;
        this.staffModifier = 1.0f;
        this.powerModifier = 0f;
        this.volatilityMultiplier = 1.0f;
        this.manaCostMultiplier = 1.0f;
    }

    public float computeSuccessChance(Glyph glyph) {
        float v = glyph.getVolatility();
        float g = 1.0f / (1 + glyphTypeCounts.getOrDefault(glyph.getGlyphId(), 0));
        float x = 1.0f;
        float c = castCount;
        float s = staffModifier;
        float m = powerModifier;

        float chance = (v * g) * (x + (c * (0.3f / s) * 0.25f)) + m;
        return Math.max(0f, Math.min(1f, chance * volatilityMultiplier));
    }

    public boolean rollAndIncrement(Glyph glyph) {
        lastChance = computeSuccessChance(glyph);
        lastRoll = ThreadLocalRandom.current().nextFloat();
        incrementGlyphType(glyph.getGlyphId());
        return lastRoll < lastChance;
    }

    public float getLastChance() {
        return lastChance;
    }

    public float getLastRoll() {
        return lastRoll;
    }

    public void incrementGlyphType(String glyphId) {
        glyphTypeCounts.merge(glyphId, 1, Integer::sum);
    }

    public int getGlyphTypeCount(String glyphId) {
        return glyphTypeCounts.getOrDefault(glyphId, 0);
    }

    public float getManaCostMultiplier() {
        return manaCostMultiplier;
    }

    public int getCastCount() {
        return castCount;
    }

    public float getStaffModifier() {
        return staffModifier;
    }

    public float getPowerModifier() {
        return powerModifier;
    }

    public float getVolatilityMultiplier() {
        return volatilityMultiplier;
    }

    public boolean isFizzled() {
        return fizzled;
    }

    public void setFizzled(boolean fizzled) {
        this.fizzled = fizzled;
    }

    public static final BuilderCodec<VolatilityTracker> CODEC = BuilderCodec
            .builder(VolatilityTracker.class, VolatilityTracker::new)
            .append(new KeyedCodec<>("CastCount", Codec.INTEGER),
                    (c, v) -> c.castCount = v,
                    (c) -> c.castCount)
            .add()
            .append(new KeyedCodec<>("StaffModifier", Codec.FLOAT),
                    (c, v) -> c.staffModifier = v,
                    (c) -> c.staffModifier)
            .add()
            .append(new KeyedCodec<>("PowerModifier", Codec.FLOAT),
                    (c, v) -> c.powerModifier = v,
                    (c) -> c.powerModifier)
            .add()
            .append(new KeyedCodec<>("VolatilityMultiplier", Codec.FLOAT),
                    (c, v) -> c.volatilityMultiplier = v,
                    (c) -> c.volatilityMultiplier)
            .add()
            .append(new KeyedCodec<>("ManaCostMultiplier", Codec.FLOAT),
                    (c, v) -> c.manaCostMultiplier = v,
                    (c) -> c.manaCostMultiplier)
            .add()
            .append(new KeyedCodec<>("GlyphTypeCounts", new MapCodec<>(Codec.INTEGER, HashMap::new, false)),
                    (c, v) -> c.glyphTypeCounts = v,
                    c -> c.glyphTypeCounts)
            .add()
            .build();
}
