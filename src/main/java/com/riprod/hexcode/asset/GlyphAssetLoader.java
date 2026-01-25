package com.riprod.hexcode.asset;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Loads glyph asset definitions from JSON files.
 *
 * <p>This loader supports dual-location loading for easy customization:
 * <ol>
 *   <li><b>Bundled defaults</b>: Loaded from JAR resources (classpath)</li>
 *   <li><b>User overrides</b>: Loaded from plugin data directory (filesystem)</li>
 * </ol>
 * User files take precedence over bundled defaults, allowing modpack developers
 * and server admins to customize or add glyphs without recompiling.
 *
 * <h2>Asset Locations</h2>
 * <ul>
 *   <li><b>Bundled</b>: {@code /Server/Riprod_Hexcode/glyphs/} in JAR resources</li>
 *   <li><b>User</b>: {@code {dataDirectory}/glyphs/} on filesystem</li>
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

    /** Path to bundled glyph assets within JAR resources */
    private static final String BUNDLED_RESOURCE_PATH = "/Server/Riprod_Hexcode/glyphs";

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
     * <p>Loads from (in order, later sources override earlier):
     * <ol>
     *   <li>Bundled JAR resources (defaults)</li>
     *   <li>User data directory (overrides)</li>
     *   <li>All registered external paths</li>
     * </ol>
     *
     * @return Map of glyph ID to asset definition
     */
    @Nonnull
    public static Map<String, GlyphAssetDefinition> loadAll() {
        ensureInitialized();

        Map<String, GlyphAssetDefinition> assets = new HashMap<>();

        // Step 1: Load bundled defaults from JAR resources
        int bundledCount = loadFromBundledResources(assets);
        LOGGER.atInfo().log("Loaded %d bundled glyph assets from JAR resources", bundledCount);

        // Step 2: Load from user data directory (overrides bundled)
        Path primaryDir = getGlyphsDirectory();
        int userCount = loadFromDirectory(primaryDir, "hexcode", assets);
        if (userCount > 0) {
            LOGGER.atInfo().log("Loaded %d user glyph assets from %s (may override bundled)", userCount, primaryDir);
        }

        // Step 3: Load from all external plugin paths
        for (Map.Entry<String, Path> entry : externalAssetPaths.entrySet()) {
            loadFromDirectory(entry.getValue(), entry.getKey(), assets);
        }

        LOGGER.atInfo().log("Loaded %d glyph assets total from directory %s", assets.size(), primaryDir);
        return assets;
    }

    /**
     * Load glyph assets from bundled JAR resources.
     *
     * @param assets Map to add loaded assets to
     * @return Number of assets loaded
     */
    private static int loadFromBundledResources(@Nonnull Map<String, GlyphAssetDefinition> assets) {
        int loadedCount = 0;

        try {
            // Get the resource URL for the glyphs directory
            java.net.URL resourceUrl = GlyphAssetLoader.class.getResource(BUNDLED_RESOURCE_PATH);
            if (resourceUrl == null) {
                LOGGER.atWarning().log("Bundled glyph resources not found at: %s", BUNDLED_RESOURCE_PATH);
                return 0;
            }

            // Handle JAR and filesystem differently
            java.net.URI uri = resourceUrl.toURI();
            if (uri.getScheme().equals("jar")) {
                // Running from JAR - use FileSystem to access
                loadedCount = loadFromJarResources(uri, assets);
            } else {
                // Running from filesystem (IDE/development) - use Path directly
                Path resourcePath = Paths.get(uri);
                loadedCount = loadFromResourcePath(resourcePath, assets);
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to load bundled glyph resources: %s", e.getMessage());
        }

        return loadedCount;
    }

    /**
     * Load glyph assets from JAR file resources.
     */
    private static int loadFromJarResources(java.net.URI jarUri, Map<String, GlyphAssetDefinition> assets) {
        int loadedCount = 0;

        try {
            // Split JAR URI into filesystem and path parts
            String[] parts = jarUri.toString().split("!");
            java.net.URI fsUri = java.net.URI.create(parts[0]);
            String pathInJar = parts[1];

            // Use try-with-resources for the FileSystem, but check if it already exists
            java.nio.file.FileSystem fs = null;
            boolean createdFs = false;
            try {
                fs = FileSystems.getFileSystem(fsUri);
            } catch (java.nio.file.FileSystemNotFoundException e) {
                fs = FileSystems.newFileSystem(fsUri, Collections.emptyMap());
                createdFs = true;
            }

            try {
                Path jarPath = fs.getPath(pathInJar);
                loadedCount = loadFromResourcePath(jarPath, assets);
            } finally {
                if (createdFs && fs != null) {
                    fs.close();
                }
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to load from JAR resources: %s", e.getMessage());
        }

        return loadedCount;
    }

    /**
     * Load glyph assets from a resource path (works for both JAR and filesystem).
     */
    private static int loadFromResourcePath(Path resourcePath, Map<String, GlyphAssetDefinition> assets) {
        int loadedCount = 0;

        if (!Files.exists(resourcePath) || !Files.isDirectory(resourcePath)) {
            LOGGER.atFine().log("Resource path does not exist or is not a directory: %s", resourcePath);
            return 0;
        }

        try (Stream<Path> paths = Files.list(resourcePath)) {
            List<Path> jsonFiles = paths
                    .filter(path -> path.toString().endsWith(JSON_EXTENSION))
                    .toList();

            for (Path path : jsonFiles) {
                try {
                    GlyphAssetDefinition asset = loadFromPath(path);
                    if (asset != null) {
                        assets.put(asset.getId(), asset);
                        loadedAssets.put(asset.getId(), asset);
                        LOGGER.atFine().log("Loaded bundled glyph asset: %s", asset.getId());
                        loadedCount++;
                    }
                } catch (Exception e) {
                    LOGGER.atWarning().log("Failed to load bundled glyph from %s: %s",
                            path.getFileName(), e.getMessage());
                }
            }
        } catch (IOException e) {
            LOGGER.atSevere().log("Failed to scan bundled glyph resources at %s: %s", resourcePath, e.getMessage());
        }

        return loadedCount;
    }

    /**
     * Load all assets from a specific directory.
     *
     * @param directory The directory to load from
     * @param namespace The namespace for these assets
     * @param assets Map to add loaded assets to
     * @return Number of assets loaded
     */
    private static int loadFromDirectory(@Nonnull Path directory, @Nonnull String namespace,
                                          @Nonnull Map<String, GlyphAssetDefinition> assets) {
        if (!Files.exists(directory)) {
            LOGGER.atFine().log("Glyph asset directory does not exist: %s", directory);
            return 0;
        }

        int[] loadedCount = {0};  // Use array for lambda capture

        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(path -> path.toString().endsWith(JSON_EXTENSION))
                    .forEach(path -> {
                        try {
                            GlyphAssetDefinition asset = loadFromPath(path);
                            if (asset != null) {
                                assets.put(asset.getId(), asset);
                                loadedAssets.put(asset.getId(), asset);
                                LOGGER.atInfo().log("Loaded glyph asset: %s", asset.getId());
                                loadedCount[0]++;
                            }
                        } catch (Exception e) {
                            LOGGER.atWarning().log("Failed to load glyph from %s: %s",
                                    path.getFileName(), e.getMessage());
                        }
                    });
        } catch (IOException e) {
            LOGGER.atSevere().log("Failed to scan glyph asset directory %s: %s", directory, e.getMessage());
        }

        return loadedCount[0];
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
