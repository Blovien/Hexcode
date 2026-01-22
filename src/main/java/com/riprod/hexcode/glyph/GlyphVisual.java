package com.riprod.hexcode.glyph;

/**
 * Visual properties for a glyph including color, shape, and particle effects.
 *
 * Supports two rendering modes:
 * 1. Model-based: Uses modelId to load a 3D blockymodel (deprecated)
 * 2. Particle-based: Uses textureId to display a 2D sprite particle (preferred)
 *
 * When textureId is set, particle-based rendering takes precedence over model rendering.
 */
public class GlyphVisual {
    private final int color;
    private final String modelId;
    private final String particleId;
    private final float glowIntensity;

    // Particle-based rendering fields
    private final String textureId;
    private final float particleScale;
    private final float rotationSpeed;

    /**
     * Legacy constructor for model-based rendering.
     * @deprecated Use the full constructor with texture fields instead.
     */
    public GlyphVisual(int color, String modelId, String particleId, float glowIntensity) {
        this(color, modelId, particleId, glowIntensity, null, 1.0f, 0.0f);
    }

    /**
     * Full constructor with particle-based rendering support.
     *
     * @param color RGB color as hex int
     * @param modelId Model asset ID (null when using particle rendering)
     * @param particleId Particle system ID for ambient effects
     * @param glowIntensity Light intensity for dynamic lighting
     * @param textureId Path to glyph PNG texture (e.g., "hexcode:glyphs/fire")
     * @param particleScale Size multiplier for the particle sprite
     * @param rotationSpeed Rotation speed in radians/sec (0 = static)
     */
    public GlyphVisual(int color, String modelId, String particleId, float glowIntensity,
                       String textureId, float particleScale, float rotationSpeed) {
        this.color = color;
        this.modelId = modelId;
        this.particleId = particleId;
        this.glowIntensity = glowIntensity;
        this.textureId = textureId;
        this.particleScale = particleScale;
        this.rotationSpeed = rotationSpeed;
    }

    public int getColor() {
        return color;
    }

    /**
     * @return Model asset ID, or null if using particle-based rendering
     */
    public String getModelId() {
        return modelId;
    }

    public String getParticleId() {
        return particleId;
    }

    public float getGlowIntensity() {
        return glowIntensity;
    }

    /**
     * @return Path to the glyph texture for particle-based rendering, or null if using model
     */
    public String getTextureId() {
        return textureId;
    }

    /**
     * @return Size multiplier for particle sprite (default 1.0)
     */
    public float getParticleScale() {
        return particleScale;
    }

    /**
     * @return Rotation speed in radians/sec (0 = static)
     */
    public float getRotationSpeed() {
        return rotationSpeed;
    }

    /**
     * @return true if this visual uses particle-based rendering (has texture)
     */
    public boolean usesParticleRendering() {
        return textureId != null && !textureId.isEmpty();
    }

    // Common color constants
    public static final int COLOR_FIRE = 0xFF6600;      // Orange
    public static final int COLOR_ICE = 0x00FFFF;       // Cyan
    public static final int COLOR_LIGHTNING = 0xFFFF00; // Yellow
    public static final int COLOR_EARTH = 0x8B4513;     // Brown
    public static final int COLOR_VOID = 0x4B0082;      // Indigo
    public static final int COLOR_LIGHT = 0xFFFFAA;     // Pale yellow
    public static final int COLOR_UTILITY = 0x00FF00;   // Green
    public static final int COLOR_HEAL = 0x00FF88;      // Green-cyan
    public static final int COLOR_SHIELD = 0x88CCFF;    // Light blue
    public static final int COLOR_BLINK = 0xAA00FF;     // Purple
    public static final int COLOR_PUSH = 0xCCCCCC;      // Light gray
    public static final int COLOR_MODIFIER = 0xFFD700;  // Gold
    public static final int COLOR_SELECT = 0xC0C0C0;    // Silver

    // Texture path constants
    public static final String TEXTURE_BASE = "hexcode:glyphs/";

    /**
     * Create an effect glyph visual with particle-based rendering.
     *
     * @param color The effect color
     * @param textureName Texture name (without prefix, e.g., "fire")
     * @return New GlyphVisual configured for particle rendering
     */
    public static GlyphVisual effect(int color, String textureName) {
        return new GlyphVisual(
                color,
                null, // No model - using particle rendering
                "hexcode:glyph_idle",
                10.0f,
                TEXTURE_BASE + textureName,
                1.0f,
                0.0f
        );
    }

    /**
     * Legacy effect factory - uses default texture based on color.
     * @deprecated Use effect(int color, String textureName) instead.
     */
    public static GlyphVisual effect(int color) {
        // Map color to texture name for backward compatibility
        String textureName = getDefaultTextureForColor(color);
        return effect(color, textureName);
    }

    /**
     * Create a modifier glyph visual with particle-based rendering.
     *
     * @param textureName Texture name (without prefix, e.g., "power")
     * @return New GlyphVisual configured for particle rendering
     */
    public static GlyphVisual modifier(String textureName) {
        return new GlyphVisual(
                COLOR_MODIFIER,
                null, // No model - using particle rendering
                "hexcode:glyph_modifier",
                8.0f,
                TEXTURE_BASE + textureName,
                1.0f,
                0.5f // Modifiers rotate slowly
        );
    }

    /**
     * Legacy modifier factory - uses default texture.
     * @deprecated Use modifier(String textureName) instead.
     */
    public static GlyphVisual modifier() {
        return modifier("modifier_default");
    }

    /**
     * Create a select glyph visual with particle-based rendering.
     *
     * @param textureName Texture name (without prefix, e.g., "beam")
     * @return New GlyphVisual configured for particle rendering
     */
    public static GlyphVisual select(String textureName) {
        return new GlyphVisual(
                COLOR_SELECT,
                null, // No model - using particle rendering
                "hexcode:glyph_select",
                12.0f,
                TEXTURE_BASE + textureName,
                1.2f, // Selects are slightly larger
                0.3f  // Slow rotation
        );
    }

    /**
     * Legacy select factory - uses default texture.
     * @deprecated Use select(String textureName) instead.
     */
    public static GlyphVisual select() {
        return select("select_default");
    }

    /**
     * Map legacy color constants to default texture names.
     */
    private static String getDefaultTextureForColor(int color) {
        if (color == COLOR_FIRE) return "fire";
        if (color == COLOR_ICE) return "ice";
        if (color == COLOR_LIGHTNING) return "lightning";
        if (color == COLOR_EARTH) return "earth";
        if (color == COLOR_VOID) return "void";
        if (color == COLOR_LIGHT) return "light";
        if (color == COLOR_HEAL) return "heal";
        if (color == COLOR_SHIELD) return "shield";
        if (color == COLOR_BLINK) return "blink";
        if (color == COLOR_PUSH) return "push";
        if (color == COLOR_UTILITY) return "utility";
        return "effect_default";
    }
}
