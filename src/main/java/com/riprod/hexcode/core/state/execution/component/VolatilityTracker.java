package com.riprod.hexcode.core.state.execution.component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import com.riprod.hexcode.core.common.glyphs.component.Glyph;

public class VolatilityTracker {
    private final Map<String, Integer> glyphTypeCounts = new HashMap<>();
    private final int castCount;
    private final float staffModifier;
    private final float powerModifier;
    private final float volatilityMultiplier;
    private final float manaCostMultiplier;
    private float lastChance;
    private float lastRoll;

    public VolatilityTracker(int castCount, float staffModifier,
            float powerModifier, float volatilityMultiplier, float manaCostMultiplier) {
        this.castCount = castCount;
        this.staffModifier = Math.max(staffModifier, 0.1f);
        this.powerModifier = powerModifier;
        this.volatilityMultiplier = volatilityMultiplier;
        this.manaCostMultiplier = manaCostMultiplier;
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
}
