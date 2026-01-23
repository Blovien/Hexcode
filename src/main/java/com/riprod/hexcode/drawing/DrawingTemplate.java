package com.riprod.hexcode.drawing;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a drawing template for glyph recognition.
 *
 * <p>Drawing templates are loaded from PNG images where:
 * <ul>
 *   <li>White pixels (RGB > 128) represent the shape</li>
 *   <li>Black/transparent pixels represent the background</li>
 * </ul>
 *
 * <p>The template is used to compare player-drawn glyphs against the expected shape.
 * Accuracy is determined by how well the player's drawing matches the template.
 *
 * <h2>File Location</h2>
 * <p>Drawing templates are stored alongside glyph definition JSON files:
 * {@code {dataDirectory}/glyphs/{name}.png}
 *
 * <h2>Initialization Pattern</h2>
 * <pre>
 * // In plugin start() - after GlyphAssetLoader.initialize()
 * DrawingTemplate.initialize(this.getDataDirectory());
 * </pre>
 *
 * <h2>PNG Format Requirements</h2>
 * <ul>
 *   <li>Black & white PNG (white = shape, black/transparent = background)</li>
 *   <li>Recommended size: 128x128 pixels</li>
 *   <li>Shape should be centered and fill ~80% of canvas</li>
 * </ul>
 */
public class DrawingTemplate {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String GLYPHS_SUBDIRECTORY = "glyphs";
    private static final String PNG_EXTENSION = ".png";

    /**
     * Brightness threshold for determining if a pixel is part of the shape.
     * Pixels with brightness above this value are considered shape pixels.
     */
    private static final int BRIGHTNESS_THRESHOLD = 128;

    /**
     * Cache of loaded templates by glyph ID.
     */
    private static final Map<String, DrawingTemplate> TEMPLATE_CACHE = new ConcurrentHashMap<>();

    /**
     * External asset paths registered by plugins (namespace -> path).
     */
    private static final Map<String, Path> externalAssetPaths = new ConcurrentHashMap<>();

    /**
     * Plugin data directory for asset loading.
     */
    private static Path dataDirectory;

    private final String glyphId;
    private final int width;
    private final int height;
    private final boolean[][] shapeData;
    private final float variability;

    // Cached normalized points for faster comparison
    private List<Point2D> normalizedPoints;

    /**
     * Create a new drawing template.
     *
     * @param glyphId The glyph ID this template belongs to
     * @param width Template width in pixels
     * @param height Template height in pixels
     * @param shapeData 2D boolean array (true = shape, false = background)
     * @param variability Drawing tolerance from 0.0-1.0
     */
    public DrawingTemplate(String glyphId, int width, int height, boolean[][] shapeData, float variability) {
        this.glyphId = glyphId;
        this.width = width;
        this.height = height;
        this.shapeData = shapeData;
        this.variability = variability;
    }

    // ========== INITIALIZATION ==========

    /**
     * Initialize the drawing template system with the plugin's data directory.
     *
     * <p>This must be called before loading templates. Typically called
     * from the plugin's start() method after GlyphAssetLoader.initialize().
     *
     * @param pluginDataDirectory The plugin's data directory from {@code plugin.getDataDirectory()}
     */
    public static void initialize(@Nonnull Path pluginDataDirectory) {
        dataDirectory = pluginDataDirectory;
        LOGGER.atInfo().log("DrawingTemplate system initialized with data directory: %s", dataDirectory);
    }

    /**
     * Check if the system has been initialized.
     *
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return dataDirectory != null;
    }

    /**
     * Get the base glyphs directory.
     *
     * @return Path to the glyphs directory
     */
    @Nonnull
    private static Path getGlyphsDirectory() {
        ensureInitialized();
        return dataDirectory.resolve(GLYPHS_SUBDIRECTORY);
    }

    /**
     * Ensure the system has been initialized.
     *
     * @throws IllegalStateException if not initialized
     */
    private static void ensureInitialized() {
        if (dataDirectory == null) {
            throw new IllegalStateException("DrawingTemplate has not been initialized. Call initialize() first.");
        }
    }

    // ========== EXTERNAL PLUGIN SUPPORT ==========

    /**
     * Register an external asset path for a plugin namespace.
     *
     * <p>This allows other plugins to load drawing templates from their own directories.
     *
     * @param namespace The plugin's namespace (e.g., "myplugin")
     * @param assetPath Path to the plugin's glyph assets directory (containing PNGs)
     */
    public static void registerExternalAssetPath(@Nonnull String namespace, @Nonnull Path assetPath) {
        externalAssetPaths.put(namespace, assetPath);
        LOGGER.atInfo().log("Registered external drawing template path for '%s': %s", namespace, assetPath);
    }

    // ========== GETTERS ==========

    /**
     * @return The glyph ID this template belongs to
     */
    public String getGlyphId() {
        return glyphId;
    }

    /**
     * @return Template width in pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return Template height in pixels
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return Drawing tolerance from 0.0-1.0
     */
    public float getVariability() {
        return variability;
    }

    /**
     * Check if a pixel is part of the shape.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return true if the pixel is part of the shape
     */
    public boolean isShapeAt(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return false;
        }
        return shapeData[y][x];
    }

    /**
     * Get the raw shape data array.
     *
     * @return 2D boolean array where true = shape pixel
     */
    public boolean[][] getShapeData() {
        // Return a copy to prevent modification
        boolean[][] copy = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            System.arraycopy(shapeData[y], 0, copy[y], 0, width);
        }
        return copy;
    }

    /**
     * Get a list of all (x, y) coordinates that are part of the shape.
     *
     * @return List of shape points in pixel coordinates
     */
    @Nonnull
    public List<Point2D> getShapePoints() {
        List<Point2D> points = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (shapeData[y][x]) {
                    points.add(new Point2D(x, y));
                }
            }
        }
        return points;
    }

    /**
     * Get shape points normalized to 0.0-1.0 range.
     * Results are cached for performance.
     *
     * @return List of normalized points
     */
    @Nonnull
    public List<Point2D> getNormalizedPoints() {
        if (normalizedPoints == null) {
            List<Point2D> points = getShapePoints();
            normalizedPoints = new ArrayList<>(points.size());

            for (Point2D point : points) {
                float normalizedX = (float) point.x / (width - 1);
                float normalizedY = (float) point.y / (height - 1);
                normalizedPoints.add(new Point2D(normalizedX, normalizedY));
            }
        }
        return Collections.unmodifiableList(normalizedPoints);
    }

    /**
     * Get the total number of shape pixels.
     *
     * @return Count of shape pixels
     */
    public int getShapePixelCount() {
        int count = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (shapeData[y][x]) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Calculate the shape density (ratio of shape pixels to total pixels).
     *
     * @return Shape density from 0.0-1.0
     */
    public float getShapeDensity() {
        return (float) getShapePixelCount() / (width * height);
    }

    // ========== LOADING ==========

    /**
     * Load a drawing template for a glyph by its ID.
     *
     * <p>Looks for a PNG file in the appropriate directory based on the glyph's namespace.
     *
     * @param glyphId The glyph ID (e.g., "hexcode:fire")
     * @param variability Drawing tolerance
     * @return The loaded template, or null on failure
     */
    @Nullable
    public static DrawingTemplate loadForGlyph(@Nonnull String glyphId, float variability) {
        ensureInitialized();

        // Check cache first
        DrawingTemplate cached = TEMPLATE_CACHE.get(glyphId);
        if (cached != null) {
            return cached;
        }

        // Parse namespace and name
        String[] parts = parseGlyphId(glyphId);
        String namespace = parts[0];
        String name = parts[1];

        // Determine which path to use
        Path assetPath;
        if ("hexcode".equals(namespace)) {
            assetPath = getGlyphsDirectory();
        } else {
            assetPath = externalAssetPaths.get(namespace);
            if (assetPath == null) {
                LOGGER.atWarning().log("No asset path registered for namespace: %s", namespace);
                return null;
            }
        }

        Path pngPath = assetPath.resolve(name + PNG_EXTENSION);
        return loadFromPath(pngPath, glyphId, variability);
    }

    /**
     * Load a drawing template from a specific PNG file path.
     *
     * @param path Full path to the PNG file
     * @param glyphId The glyph ID for this template
     * @param variability Drawing tolerance
     * @return The loaded template, or null on failure
     */
    @Nullable
    public static DrawingTemplate loadFromPath(@Nonnull Path path, @Nonnull String glyphId, float variability) {
        // Check cache first
        DrawingTemplate cached = TEMPLATE_CACHE.get(glyphId);
        if (cached != null) {
            return cached;
        }

        if (!Files.exists(path)) {
            LOGGER.atFine().log("Drawing template not found: %s", path);
            return null;
        }

        try (InputStream is = Files.newInputStream(path)) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                LOGGER.atSevere().log("Failed to read image: %s", path);
                return null;
            }

            int width = image.getWidth();
            int height = image.getHeight();
            boolean[][] shapeData = new boolean[height][width];

            // Convert image to boolean array
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = image.getRGB(x, y);
                    int alpha = (rgb >> 24) & 0xFF;
                    int red = (rgb >> 16) & 0xFF;
                    int green = (rgb >> 8) & 0xFF;
                    int blue = rgb & 0xFF;

                    // Calculate brightness
                    int brightness = (red + green + blue) / 3;

                    // Pixel is part of shape if it's bright and not transparent
                    shapeData[y][x] = alpha > 128 && brightness > BRIGHTNESS_THRESHOLD;
                }
            }

            DrawingTemplate template = new DrawingTemplate(glyphId, width, height, shapeData, variability);
            TEMPLATE_CACHE.put(glyphId, template);
            LOGGER.atInfo().log("Loaded drawing template for %s (%dx%d, %d shape pixels)",
                    glyphId, width, height, template.getShapePixelCount());
            return template;

        } catch (IOException e) {
            LOGGER.atSevere().log("Failed to load drawing template %s: %s", path, e.getMessage());
            return null;
        }
    }

    /**
     * Load a drawing template from a PNG file (legacy method).
     *
     * @param path Path to the PNG file (relative path)
     * @param glyphId The glyph ID for this template
     * @param variability Drawing tolerance
     * @return The loaded template, or null on failure
     * @deprecated Use {@link #loadForGlyph(String, float)} instead
     */
    @Deprecated
    @Nullable
    public static DrawingTemplate loadFromPng(String path, String glyphId, float variability) {
        ensureInitialized();
        Path fullPath = getGlyphsDirectory().resolve(path);
        return loadFromPath(fullPath, glyphId, variability);
    }

    // ========== CACHE MANAGEMENT ==========

    /**
     * Get a cached template by glyph ID.
     *
     * @param glyphId The glyph ID
     * @return The cached template, or null if not loaded
     */
    @Nullable
    public static DrawingTemplate getCached(@Nonnull String glyphId) {
        return TEMPLATE_CACHE.get(glyphId);
    }

    /**
     * Check if a template is cached.
     *
     * @param glyphId The glyph ID
     * @return true if the template is cached
     */
    public static boolean hasCached(@Nonnull String glyphId) {
        return TEMPLATE_CACHE.containsKey(glyphId);
    }

    /**
     * Clear the template cache.
     */
    public static void clearCache() {
        TEMPLATE_CACHE.clear();
        LOGGER.atInfo().log("Cleared drawing template cache");
    }

    /**
     * Get the number of cached templates.
     *
     * @return Cache size
     */
    public static int getCacheSize() {
        return TEMPLATE_CACHE.size();
    }

    // ========== UTILITY ==========

    /**
     * Parse a glyph ID into namespace and name.
     *
     * @param glyphId The glyph ID (e.g., "hexcode:fire")
     * @return Array of [namespace, name]
     */
    @Nonnull
    private static String[] parseGlyphId(@Nonnull String glyphId) {
        int colonIndex = glyphId.indexOf(':');
        if (colonIndex < 0) {
            return new String[]{"hexcode", glyphId};
        }
        return new String[]{glyphId.substring(0, colonIndex), glyphId.substring(colonIndex + 1)};
    }

    /**
     * Reset the system state. Used for testing.
     */
    public static void reset() {
        TEMPLATE_CACHE.clear();
        externalAssetPaths.clear();
        dataDirectory = null;
    }

    /**
     * Simple 2D point class for coordinates.
     */
    public static class Point2D {
        public final double x;
        public final double y;

        public Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }

        /**
         * Calculate the distance to another point.
         */
        public double distanceTo(Point2D other) {
            double dx = other.x - x;
            double dy = other.y - y;
            return Math.sqrt(dx * dx + dy * dy);
        }

        /**
         * Calculate the squared distance to another point (faster than distanceTo).
         */
        public double distanceSquaredTo(Point2D other) {
            double dx = other.x - x;
            double dy = other.y - y;
            return dx * dx + dy * dy;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Point2D point2D = (Point2D) o;
            return Double.compare(point2D.x, x) == 0 && Double.compare(point2D.y, y) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return String.format("(%.2f, %.2f)", x, y);
        }
    }

    @Override
    public String toString() {
        return String.format("DrawingTemplate{glyphId='%s', size=%dx%d, pixels=%d, variability=%.2f}",
                glyphId, width, height, getShapePixelCount(), variability);
    }
}
