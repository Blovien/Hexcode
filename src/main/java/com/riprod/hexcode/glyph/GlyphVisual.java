package com.riprod.hexcode.glyph;

/**
 * Visual properties for a glyph including color, shape, and particle effects.
 */
public class GlyphVisual {
    private final int color;
    private final String modelId;
    private final String particleId;
    private final float glowIntensity;

    public GlyphVisual(int color, String modelId, String particleId, float glowIntensity) {
        this.color = color;
        this.modelId = modelId;
        this.particleId = particleId;
        this.glowIntensity = glowIntensity;
    }

    public int getColor() {
        return color;
    }

    public String getModelId() {
        return modelId;
    }

    public String getParticleId() {
        return particleId;
    }

    public float getGlowIntensity() {
        return glowIntensity;
    }

    // Common color constants
    public static final int COLOR_FIRE = 0xFF6600;      // Orange
    public static final int COLOR_ICE = 0x00FFFF;       // Cyan
    public static final int COLOR_LIGHTNING = 0xFFFF00; // Yellow
    public static final int COLOR_EARTH = 0x8B4513;     // Brown
    public static final int COLOR_VOID = 0x4B0082;      // Indigo
    public static final int COLOR_UTILITY = 0x00FF00;   // Green
    public static final int COLOR_MODIFIER = 0xFFD700;  // Gold
    public static final int COLOR_SELECT = 0xC0C0C0;    // Silver

    public static GlyphVisual effect(int color) {
        return new GlyphVisual(color, "hexcode:glyph_frame", "hexcode:glyph_idle", 10.0f);
    }

    public static GlyphVisual modifier() {
        return new GlyphVisual(COLOR_MODIFIER, "hexcode:glyph_frame", "hexcode:glyph_modifier", 8.0f);
    }

    public static GlyphVisual select() {
        return new GlyphVisual(COLOR_SELECT, "hexcode:glyph_frame", "hexcode:glyph_select", 12.0f);
    }
}
