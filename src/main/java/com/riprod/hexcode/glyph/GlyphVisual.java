package com.riprod.hexcode.glyph;

/**
 * Visual properties for a glyph including color, model, and lighting.
 *
 * All glyphs use blockymodel-based rendering. Each glyph specifies:
 * - A blockymodel path for the 3D representation
 * - A color for tinting and dynamic lighting
 * - A glow intensity for the dynamic light radius
 * - An optional rotation speed for spinning effects
 */
public class GlyphVisual {
    private final int color;
    private final String modelId;
    private final float glowIntensity;
    private final float rotationSpeed;

    // Default model paths
    private static final String DEFAULT_EFFECT_MODEL = "Effect_default";
    private static final String DEFAULT_MODIFIER_MODEL = "Modifier_default";
    private static final String DEFAULT_SELECT_MODEL = "Select_default";

    /**
     * Create a GlyphVisual with all properties.
     *
     * @param color RGB color as hex int (e.g., 0xFF6600 for orange)
     * @param modelId Path to the blockymodel asset
     * @param glowIntensity Light intensity/radius for dynamic lighting
     * @param rotationSpeed Rotation speed in radians/sec (0 = static)
     */
    public GlyphVisual(int color, String modelId, float glowIntensity, float rotationSpeed) {
        this.color = color;
        this.modelId = modelId;
        this.glowIntensity = glowIntensity;
        this.rotationSpeed = rotationSpeed;
    }

    /**
     * Create a GlyphVisual with default rotation (static).
     *
     * @param color RGB color as hex int
     * @param modelId Path to the blockymodel asset
     * @param glowIntensity Light intensity for dynamic lighting
     */
    public GlyphVisual(int color, String modelId, float glowIntensity) {
        this(color, modelId, glowIntensity, 0.0f);
    }

    /**
     * Legacy constructor for backward compatibility.
     * @deprecated Use the new constructors instead.
     */
    @Deprecated
    public GlyphVisual(int color, String modelId, String particleId, float glowIntensity) {
        this(color, modelId, glowIntensity, 0.0f);
    }

    /**
     * Legacy constructor for backward compatibility.
     * @deprecated Use the new constructors instead.
     */
    @Deprecated
    public GlyphVisual(int color, String modelId, String particleId, float glowIntensity,
                       String textureId, float particleScale, float rotationSpeed) {
        this(color, modelId, glowIntensity, rotationSpeed);
    }

    public int getColor() {
        return color;
    }

    /**
     * @return The blockymodel asset path
     */
    public String getModelId() {
        return modelId;
    }

    public float getGlowIntensity() {
        return glowIntensity;
    }

    /**
     * @return Rotation speed in radians/sec (0 = static)
     */
    public float getRotationSpeed() {
        return rotationSpeed;
    }

    /**
     * Get the red component of the color.
     * @return Red value (0-255)
     */
    public int getRed() {
        return (color >> 16) & 0xFF;
    }

    /**
     * Get the green component of the color.
     * @return Green value (0-255)
     */
    public int getGreen() {
        return (color >> 8) & 0xFF;
    }

    /**
     * Get the blue component of the color.
     * @return Blue value (0-255)
     */
    public int getBlue() {
        return color & 0xFF;
    }

    /**
     * @deprecated Particle rendering is no longer used. Always returns false.
     */
    @Deprecated
    public boolean usesParticleRendering() {
        return false;
    }

    /**
     * @deprecated Use getModelId() instead.
     */
    @Deprecated
    public String getParticleId() {
        return null;
    }

    /**
     * @deprecated Particle rendering is no longer used. Returns 1.0.
     */
    @Deprecated
    public float getParticleScale() {
        return 1.0f;
    }

    /**
     * @deprecated Use getModelId() instead.
     */
    @Deprecated
    public String getTextureId() {
        return null;
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

    // Model ID prefix (empty since we use asset IDs directly)
    public static final String MODEL_BASE = "";

    /**
     * Create an effect glyph visual with a blockymodel.
     *
     * @param color The effect color for tinting and glow
     * @param modelName Model name (without prefix, e.g., "fire")
     * @return New GlyphVisual configured for blockymodel rendering
     */
    public static GlyphVisual effect(int color, String modelName) {
        return new GlyphVisual(
                color,
                MODEL_BASE + modelName,
                0.5f,
                0.0f  // Effects don't rotate by default
        );
    }

    /**
     * Create a modifier glyph visual with a blockymodel.
     *
     * @param modelName Model name (without prefix, e.g., "power")
     * @return New GlyphVisual configured for blockymodel rendering
     */
    public static GlyphVisual modifier(String modelName) {
        return new GlyphVisual(
                COLOR_MODIFIER,
                MODEL_BASE + modelName,
                0.5f,
                0.5f  // Modifiers rotate slowly
        );
    }

    /**
     * Create a select glyph visual with a blockymodel.
     *
     * @param modelName Model name (without prefix, e.g., "beam")
     * @return New GlyphVisual configured for blockymodel rendering
     */
    public static GlyphVisual select(String modelName) {
        return new GlyphVisual(
                COLOR_SELECT,
                MODEL_BASE + modelName,
                12.0f,
                0.3f  // Selects rotate slowly
        );
    }

    /**
     * Create a select glyph visual with default model.
     *
     * @return New GlyphVisual with default select model
     */
    public static GlyphVisual select() {
        return new GlyphVisual(
                COLOR_SELECT,
                DEFAULT_SELECT_MODEL,
                12.0f,
                0.3f
        );
    }

    /**
     * Get the default model path for a glyph role.
     *
     * @param role The glyph role
     * @return The default model path
     */
    public static String getDefaultModelForRole(GlyphRole role) {
        if (role == null) {
            return DEFAULT_EFFECT_MODEL;
        }
        switch (role) {
            case EFFECT:
                return DEFAULT_EFFECT_MODEL;
            case MODIFIER:
                return DEFAULT_MODIFIER_MODEL;
            case SELECT:
                return DEFAULT_SELECT_MODEL;
            default:
                return DEFAULT_EFFECT_MODEL;
        }
    }
}
