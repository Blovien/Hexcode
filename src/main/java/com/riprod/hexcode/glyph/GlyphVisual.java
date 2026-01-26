package com.riprod.hexcode.glyph;

/**
 * Visual properties for a glyph including color, model, and lighting.
 *
 * All glyphs use blockymodel-based rendering. Each glyph specifies:
 * - A blockymodel path for the 3D representation
 * - A color for tinting and dynamic lighting
 * - A glow intensity for the dynamic light radius
 * - An optional rotation speed for spinning effects
 * - A scale factor (1.0 = default, higher = larger, lower = smaller)
 * - Position offsets relative to the parent (x=0.5, y=0.5, z=0.0 = center of parent)
 */
public class GlyphVisual {
    private final int color;
    private final String modelId;
    private final float glowIntensity;
    private final float rotationSpeed;
    private float scale;
    private float offsetX;
    private float offsetY;
    private float offsetZ;

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
     * @param scale Scale factor (1.0 = default)
     * @param offsetX X offset relative to parent (0.5 = center)
     * @param offsetY Y offset relative to parent (0.5 = center)
     * @param offsetZ Z offset relative to parent (0.0 = center)
     */
    public GlyphVisual(int color, String modelId, float glowIntensity, float rotationSpeed, float scale, float offsetX, float offsetY, float offsetZ) {
        this.color = color;
        this.modelId = modelId;
        this.glowIntensity = glowIntensity;
        this.rotationSpeed = rotationSpeed;
        this.scale = scale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
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
     * @return Scale factor (1.0 = default)
     */
    public float getScale() {
        return scale;
    }

    /**
     * @return X offset relative to parent (0.5 = center)
     */
    public float getOffsetX() {
        return offsetX;
    }

    /**
     * @return Y offset relative to parent (0.5 = center)
     */
    public float getOffsetY() {
        return offsetY;
    }

    /**
     * @return Z offset relative to parent (0.0 = center)
     */
    public float getOffsetZ() {
        return offsetZ;
    }

    /**
     * Set the scale factor.
     * @param scale Scale factor (1.0 = default)
     */
    public void setScale(float scale) {
        this.scale = scale;
    }

    /**
     * Set the X offset relative to parent.
     * @param offsetX X offset (0.5 = center)
     */
    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }

    /**
     * Set the Y offset relative to parent.
     * @param offsetY Y offset (0.5 = center)
     */
    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
    }

    /**
     * Set the Z offset relative to parent.
     * @param offsetZ Z offset (0.0 = center)
     */
    public void setOffsetZ(float offsetZ) {
        this.offsetZ = offsetZ;
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
                0.05f,
                0.0f,  // Effects don't rotate by default
                1.0f,  // Default scale
                0.5f,  // Center X
                0.5f,  // Center Y
                0.0f   // Center Z
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
                0.05f,
                0.5f,  // Modifiers rotate slowly
                1.0f,  // Default scale
                0.5f,  // Center X
                0.5f,  // Center Y
                0.0f   // Center Z
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
                0.05f,
                0.3f,  // Selects rotate slowly
                1.0f,  // Default scale
                0.5f,  // Center X
                0.5f,  // Center Y
                0.0f   // Center Z
        );
    }
}
