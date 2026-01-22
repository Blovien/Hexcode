package com.riprod.hexcode.glyph;

/**
 * Immutable data class representing the visual shape properties of a glyph.
 *
 * Used for particle-based glyph rendering instead of 3D models.
 * Each glyph has a PNG texture (128x128, black/white shape on transparent background)
 * that is displayed as a tinted particle sprite.
 */
public final class GlyphShape {
    private final String textureId;
    private final int color;
    private final float scale;
    private final float glowIntensity;
    private final float rotationSpeed;

    /**
     * Create a new GlyphShape.
     *
     * @param textureId Path to the glyph PNG texture (e.g., "hexcode:glyphs/fire")
     * @param color RGB color as hex int (e.g., 0xFF6600 for orange)
     * @param scale Size multiplier (1.0 = default size)
     * @param glowIntensity Light intensity (0-255)
     * @param rotationSpeed Rotation speed in radians/sec (0 = static)
     */
    public GlyphShape(String textureId, int color, float scale, float glowIntensity, float rotationSpeed) {
        this.textureId = textureId;
        this.color = color;
        this.scale = scale;
        this.glowIntensity = glowIntensity;
        this.rotationSpeed = rotationSpeed;
    }

    /**
     * Create a GlyphShape from a Glyph's visual properties.
     *
     * @param glyph The glyph to extract shape data from
     * @return A new GlyphShape instance
     */
    public static GlyphShape fromGlyph(Glyph glyph) {
        GlyphVisual visual = glyph.getVisual();
        return new GlyphShape(
                visual.getTextureId(),
                visual.getColor(),
                visual.getParticleScale(),
                visual.getGlowIntensity(),
                visual.getRotationSpeed()
        );
    }

    /**
     * Create a default effect glyph shape with the given texture and color.
     */
    public static GlyphShape effect(String textureId, int color) {
        return new GlyphShape(textureId, color, 1.0f, 10.0f, 0.0f);
    }

    /**
     * Create a default modifier glyph shape with the given texture.
     */
    public static GlyphShape modifier(String textureId) {
        return new GlyphShape(textureId, GlyphVisual.COLOR_MODIFIER, 1.0f, 8.0f, 0.5f);
    }

    /**
     * Create a default select glyph shape with the given texture.
     */
    public static GlyphShape select(String textureId) {
        return new GlyphShape(textureId, GlyphVisual.COLOR_SELECT, 1.2f, 12.0f, 0.3f);
    }

    /**
     * @return Path to the glyph texture asset
     */
    public String getTextureId() {
        return textureId;
    }

    /**
     * @return RGB color as hex int
     */
    public int getColor() {
        return color;
    }

    /**
     * Extract red component (0-255) from color.
     */
    public int getRed() {
        return (color >> 16) & 0xFF;
    }

    /**
     * Extract green component (0-255) from color.
     */
    public int getGreen() {
        return (color >> 8) & 0xFF;
    }

    /**
     * Extract blue component (0-255) from color.
     */
    public int getBlue() {
        return color & 0xFF;
    }

    /**
     * @return Size multiplier
     */
    public float getScale() {
        return scale;
    }

    /**
     * @return Light glow intensity
     */
    public float getGlowIntensity() {
        return glowIntensity;
    }

    /**
     * @return Rotation speed in radians per second
     */
    public float getRotationSpeed() {
        return rotationSpeed;
    }

    @Override
    public String toString() {
        return String.format("GlyphShape{texture=%s, color=#%06X, scale=%.1f, glow=%.1f, rotation=%.2f}",
                textureId, color, scale, glowIntensity, rotationSpeed);
    }
}
