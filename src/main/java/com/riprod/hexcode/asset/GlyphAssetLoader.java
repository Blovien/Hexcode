package com.riprod.hexcode.asset;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Loads glyph asset definitions from JSON files.
 *
 * <p>This loader follows the Hytale plugin pattern for file access, using
 * the plugin's data directory as the base path. It supports loading from
 * multiple sources to enable plugin extensibility.
 *
 * <h2>Asset Locations</h2>
 * <ul>
 *   <li><b>Primary</b>: {@code {dataDirectory}/glyphs/} - Built-in glyphs</li>
 *   <li><b>External</b>: Registered via {@link #registerExternalAssetPath(String, Path)}</li>
 * </ul>
 *
 * <h2>Initialization Pattern</h2>
 * <p>Follows the BarterShopState pattern from Hytale's built-in plugins:
 * <pre>
 * // In plugin start()
 * GlyphAssetLoader.initialize(this.getDataDirectory());
 * Map&lt;String, GlyphAssetDefinition&gt; assets = GlyphAssetLoader.loadAll();
 * </pre>
 *
 * <h2>External Plugin Support</h2>
 * <pre>
 * // External plugin can register their asset path
 * GlyphAssetLoader.registerExternalAssetPath("myplugin", myPlugin.getDataDirectory().resolve("glyphs"));
 * </pre>
 *
 * @see GlyphAssetDefinition
 */
public class GlyphAssetLoader {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final String GLYPHS_SUBDIRECTORY = "glyphs";
    private static final String JSON_EXTENSION = ".json";

    // Static instance following BarterShopState pattern
    private static Path dataDirectory;
    private static final Map<String, GlyphAssetDefinition> loadedAssets = new ConcurrentHashMap<>();
    private static final Map<String, Path> externalAssetPaths = new ConcurrentHashMap<>();

    private GlyphAssetLoader() {
        // Static utility class
    }

    // ========== INITIALIZATION ==========

    /**
     * Initialize the asset loader with the plugin's data directory.
     *
     * <p>This must be called before any other methods. Typically called
     * from the plugin's start() method.
     *
     * @param pluginDataDirectory The plugin's data directory from {@code plugin.getDataDirectory()}
     */
    public static void initialize(@Nonnull Path pluginDataDirectory) {
        dataDirectory = pluginDataDirectory;
        LOGGER.atInfo().log("GlyphAssetLoader initialized with data directory: %s", dataDirectory);

        // Ensure the glyphs directory exists
        Path glyphsDir = getGlyphsDirectory();
        try {
            if (!Files.exists(glyphsDir)) {
                Files.createDirectories(glyphsDir);
                LOGGER.atInfo().log("Created glyphs directory: %s", glyphsDir);
            }
        } catch (IOException e) {
            LOGGER.atWarning().log("Failed to create glyphs directory: %s", e.getMessage());
        }
    }

    /**
     * Check if the loader has been initialized.
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
     * Ensure the loader has been initialized.
     *
     * @throws IllegalStateException if not initialized
     */
    private static void ensureInitialized() {
        if (dataDirectory == null) {
            throw new IllegalStateException("GlyphAssetLoader has not been initialized. Call initialize() first.");
        }
    }

    // ========== EXTERNAL PLUGIN SUPPORT ==========

    /**
     * Register an external asset path for a plugin.
     *
     * <p>This allows other plugins to register their glyph assets
     * to be loaded alongside the built-in glyphs.
     *
     * @param namespace The plugin's namespace (e.g., "myplugin")
     * @param assetPath Path to the plugin's glyph assets directory
     */
    public static void registerExternalAssetPath(@Nonnull String namespace, @Nonnull Path assetPath) {
        externalAssetPaths.put(namespace, assetPath);
        LOGGER.atInfo().log("Registered external asset path for '%s': %s", namespace, assetPath);
    }

    /**
     * Unregister an external asset path.
     *
     * @param namespace The plugin's namespace to unregister
     */
    public static void unregisterExternalAssetPath(@Nonnull String namespace) {
        externalAssetPaths.remove(namespace);
        LOGGER.atInfo().log("Unregistered external asset path for '%s'", namespace);
    }

    /**
     * Get all registered external asset paths.
     *
     * @return Unmodifiable map of namespace to path
     */
    @Nonnull
    public static Map<String, Path> getExternalAssetPaths() {
        return Collections.unmodifiableMap(externalAssetPaths);
    }

    // ========== ASSET LOADING ==========

    /**
     * Load a single glyph asset by its ID.
     *
     * @param glyphId The full glyph ID (e.g., "hexcode:fire")
     * @return The loaded asset definition, or null if loading fails
     */
    @Nullable
    public static GlyphAssetDefinition loadGlyphAsset(@Nonnull String glyphId) {
        ensureInitialized();

        // Check cache first
        GlyphAssetDefinition cached = loadedAssets.get(glyphId);
        if (cached != null) {
            return cached;
        }

        // Extract namespace and name from ID
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

        Path filePath = assetPath.resolve(name + JSON_EXTENSION);

        try {
            GlyphAssetDefinition asset = loadFromPath(filePath);
            if (asset != null) {
                if (!asset.getId().equals(glyphId)) {
                    LOGGER.atWarning().log("Asset ID mismatch: expected %s, got %s", glyphId, asset.getId());
                }
                loadedAssets.put(asset.getId(), asset);
                return asset;
            }
        } catch (Exception e) {
            LOGGER.atSevere().log("Failed to load glyph asset '%s': %s", glyphId, e.getMessage());
        }

        return null;
    }

    /**
     * Load all glyph assets from all registered paths.
     *
     * <p>Loads from:
     * <ol>
     *   <li>Primary glyphs directory</li>
     *   <li>All registered external paths</li>
     * </ol>
     *
     * @return Map of glyph ID to asset definition
     */
    @Nonnull
    public static Map<String, GlyphAssetDefinition> loadAll() {
        ensureInitialized();

        Map<String, GlyphAssetDefinition> assets = new HashMap<>();

        // Load from primary directory
        Path primaryDir = getGlyphsDirectory();
        loadFromDirectory(primaryDir, "hexcode", assets);

        // Load from all external paths
        for (Map.Entry<String, Path> entry : externalAssetPaths.entrySet()) {
            loadFromDirectory(entry.getValue(), entry.getKey(), assets);
        }

        LOGGER.atInfo().log("Loaded %d glyph assets total", assets.size());
        return assets;
    }

    /**
     * Load all assets from a specific directory.
     *
     * @param directory The directory to load from
     * @param namespace The namespace for these assets
     * @param assets Map to add loaded assets to
     */
    private static void loadFromDirectory(@Nonnull Path directory, @Nonnull String namespace,
                                          @Nonnull Map<String, GlyphAssetDefinition> assets) {
        if (!Files.exists(directory)) {
            LOGGER.atFine().log("Glyph asset directory does not exist: %s", directory);
            return;
        }

        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(path -> path.toString().endsWith(JSON_EXTENSION))
                    .forEach(path -> {
                        try {
                            GlyphAssetDefinition asset = loadFromPath(path);
                            if (asset != null) {
                                assets.put(asset.getId(), asset);
                                loadedAssets.put(asset.getId(), asset);
                                LOGGER.atInfo().log("Loaded glyph asset: %s", asset.getId());
                            }
                        } catch (Exception e) {
                            LOGGER.atWarning().log("Failed to load glyph from %s: %s",
                                    path.getFileName(), e.getMessage());
                        }
                    });
        } catch (IOException e) {
            LOGGER.atSevere().log("Failed to scan glyph asset directory %s: %s", directory, e.getMessage());
        }
    }

    /**
     * Reload all assets from disk.
     *
     * @return Map of reloaded glyph ID to asset definition
     */
    @Nonnull
    public static Map<String, GlyphAssetDefinition> reload() {
        LOGGER.atInfo().log("Reloading all glyph assets...");
        loadedAssets.clear();
        return loadAll();
    }

    // ========== ASYNC OPERATIONS ==========

    /**
     * Load all assets asynchronously.
     *
     * @return CompletableFuture that completes with the loaded assets
     */
    @Nonnull
    public static CompletableFuture<Map<String, GlyphAssetDefinition>> loadAllAsync() {
        return CompletableFuture.supplyAsync(GlyphAssetLoader::loadAll);
    }

    /**
     * Load a single asset asynchronously.
     *
     * @param glyphId The glyph ID to load
     * @return CompletableFuture that completes with the loaded asset (or null)
     */
    @Nonnull
    public static CompletableFuture<GlyphAssetDefinition> loadGlyphAssetAsync(@Nonnull String glyphId) {
        return CompletableFuture.supplyAsync(() -> loadGlyphAsset(glyphId));
    }

    // ========== CACHE ACCESS ==========

    /**
     * Get all currently loaded assets.
     *
     * @return Unmodifiable map of loaded assets
     */
    @Nonnull
    public static Map<String, GlyphAssetDefinition> getLoadedAssets() {
        return Collections.unmodifiableMap(loadedAssets);
    }

    /**
     * Get a loaded asset by ID.
     *
     * @param glyphId The glyph ID
     * @return The asset definition, or null if not loaded
     */
    @Nullable
    public static GlyphAssetDefinition getAsset(@Nonnull String glyphId) {
        return loadedAssets.get(glyphId);
    }

    /**
     * Check if an asset is loaded.
     *
     * @param glyphId The glyph ID
     * @return true if the asset is loaded
     */
    public static boolean hasAsset(@Nonnull String glyphId) {
        return loadedAssets.containsKey(glyphId);
    }

    // ========== FILE LOADING ==========

    /**
     * Load a glyph asset from a specific path.
     *
     * @param path Path to the JSON file
     * @return The loaded asset, or null on failure
     */
    @Nullable
    private static GlyphAssetDefinition loadFromPath(@Nonnull Path path) {
        if (!Files.exists(path)) {
            LOGGER.atFine().log("Asset file does not exist: %s", path);
            return null;
        }

        try (InputStream is = Files.newInputStream(path);
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            GlyphAssetDefinition asset = GlyphAssetDefinition.fromJson(json);

            // Validate the asset
            try {
                asset.validate();
            } catch (IllegalStateException e) {
                LOGGER.atSevere().log("Asset validation failed for %s: %s", path.getFileName(), e.getMessage());
                return null;
            }

            // Check for missing referenced files
            validateReferencedFiles(asset);

            return asset;

        } catch (IOException e) {
            LOGGER.atSevere().log("Failed to read asset file %s: %s", path.getFileName(), e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.atSevere().log("Failed to parse asset file %s: %s", path.getFileName(), e.getMessage());
            return null;
        }
    }

    /**
     * Load a glyph asset from an external plugin's asset path by full path.
     *
     * <p>This is used during GlyphRegistrationEvent for external plugins
     * that want to load assets from arbitrary locations.
     *
     * @param assetPath Full path to the asset file
     * @return The loaded asset, or null on failure
     */
    @Nullable
    public static GlyphAssetDefinition loadFromExternalPath(@Nonnull Path assetPath) {
        return loadFromPath(assetPath);
    }

    /**
     * Validate that referenced files (model, drawing template) exist.
     * Logs warnings but doesn't prevent loading.
     */
    private static void validateReferencedFiles(@Nonnull GlyphAssetDefinition asset) {
        String modelPath = asset.getModelPath();
        if (modelPath != null && !modelPath.isEmpty()) {
            LOGGER.atFine().log("Asset %s references model: %s", asset.getId(), modelPath);
        }

        String drawingPath = asset.getDrawingTemplatePath();
        if (drawingPath != null && !drawingPath.isEmpty()) {
            LOGGER.atFine().log("Asset %s references drawing template: %s", asset.getId(), drawingPath);
        }
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
     * Reset the loader state. Used for testing.
     */
    public static void reset() {
        loadedAssets.clear();
        externalAssetPaths.clear();
        dataDirectory = null;
    }

    /**
     * Get the data directory path. Used for testing.
     *
     * @return The current data directory, or null if not initialized
     */
    @Nullable
    public static Path getDataDirectory() {
        return dataDirectory;
    }
}
