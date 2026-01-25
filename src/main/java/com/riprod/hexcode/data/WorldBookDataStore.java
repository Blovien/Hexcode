package com.riprod.hexcode.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Per-world, per-player book data storage manager.
 *
 * <p>This replaces the old HexBookDataStore system with proper per-world isolation.
 * Data is stored in the world's save directory following this structure:
 * <pre>
 * {world_save_path}/hexcode/
 * └── {player_uuid}/
 *     ├── hex_book.json
 *     ├── fire_hex_book.json
 *     └── ancient_hex_book.json
 * </pre>
 *
 * <p>Key features:
 * <ul>
 *   <li>Per-world data isolation - each world has its own book data</li>
 *   <li>Per-player storage - each player has their own directory</li>
 *   <li>Per-book-type files - different book types stored separately</li>
 *   <li>Automatic caching with dirty tracking for efficient saves</li>
 *   <li>Thread-safe operations using ConcurrentHashMap</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * // Get book data for a player in a world
 * HexBookData data = WorldBookDataStore.get().getBookData(world, playerUuid, BookType.HEX_BOOK);
 *
 * // Modify and save
 * data.recordGlyphDrawing("hexcode:fire", 0.95f, 2.3f);
 * WorldBookDataStore.get().saveBookData(world, playerUuid, BookType.HEX_BOOK, data);
 * </pre>
 *
 * @see BookType
 * @see HexBookData
 */
public class WorldBookDataStore {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String HEXCODE_DIR = "hexcode";
    private static final int DATA_VERSION = 1;

    private static WorldBookDataStore instance;

    /**
     * Cache structure: worldName -> playerUUID -> bookType -> CachedBookData
     */
    private final Map<String, Map<UUID, Map<BookType, CachedBookData>>> cache = new ConcurrentHashMap<>();

    /**
     * Wrapper for cached book data with dirty tracking.
     */
    private static class CachedBookData {
        final HexBookData data;
        final AtomicBoolean dirty;
        final long loadedAt;

        CachedBookData(@Nonnull HexBookData data) {
            this.data = data;
            this.dirty = new AtomicBoolean(false);
            this.loadedAt = System.currentTimeMillis();
        }
    }

    private WorldBookDataStore() {}

    /**
     * Initialize the WorldBookDataStore singleton.
     * Should be called during plugin setup.
     */
    public static void initialize() {
        if (instance == null) {
            instance = new WorldBookDataStore();
            LOGGER.at(Level.INFO).log("WorldBookDataStore initialized");
        }
    }

    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static WorldBookDataStore get() {
        if (instance == null) {
            instance = new WorldBookDataStore();
        }
        return instance;
    }

    /**
     * Shutdown and save all dirty data.
     */
    public static void shutdown() {
        if (instance != null) {
            instance.saveAllDirty();
            instance.cache.clear();
            instance = null;
            LOGGER.at(Level.INFO).log("WorldBookDataStore shutdown complete");
        }
    }

    // ==================== PATH RESOLUTION ====================

    /**
     * Get the hexcode data directory for a world.
     *
     * @param world The world
     * @return Path to {world_save_path}/hexcode/
     */
    @Nonnull
    private Path getHexcodeDir(@Nonnull World world) {
        return world.getSavePath().resolve(HEXCODE_DIR);
    }

    /**
     * Get the player's data directory in a world.
     *
     * @param world The world
     * @param playerUuid The player's UUID
     * @return Path to {world_save_path}/hexcode/{player_uuid}/
     */
    @Nonnull
    private Path getPlayerDir(@Nonnull World world, @Nonnull UUID playerUuid) {
        return getHexcodeDir(world).resolve(playerUuid.toString());
    }

    /**
     * Get the path to a specific book data file.
     *
     * @param world The world
     * @param playerUuid The player's UUID
     * @param bookType The book type
     * @return Path to {world_save_path}/hexcode/{player_uuid}/{book_type}.json
     */
    @Nonnull
    private Path getBookFilePath(@Nonnull World world, @Nonnull UUID playerUuid, @Nonnull BookType bookType) {
        return getPlayerDir(world, playerUuid).resolve(bookType.getFileName());
    }

    // ==================== DATA ACCESS ====================

    /**
     * Get book data for a player in a specific world.
     * Creates new empty data if none exists.
     *
     * @param world The world
     * @param playerUuid The player's UUID
     * @param bookType The book type
     * @return The HexBookData (existing or new empty)
     */
    @Nonnull
    public HexBookData getBookData(@Nonnull World world, @Nonnull UUID playerUuid, @Nonnull BookType bookType) {
        String worldName = world.getName();

        // Check cache first
        CachedBookData cached = getCachedData(worldName, playerUuid, bookType);
        if (cached != null) {
            return cached.data;
        }

        // Load from disk
        HexBookData data = loadFromDisk(world, playerUuid, bookType);

        // Cache it
        putInCache(worldName, playerUuid, bookType, new CachedBookData(data));

        return data;
    }

    /**
     * Get book data using world name (for contexts where World object isn't available).
     * Note: This only works for cached data - use getBookData(World, ...) for disk access.
     *
     * @param worldName The world name
     * @param playerUuid The player's UUID
     * @param bookType The book type
     * @return The cached HexBookData, or null if not cached
     */
    @Nullable
    public HexBookData getCachedBookData(@Nonnull String worldName, @Nonnull UUID playerUuid, @Nonnull BookType bookType) {
        CachedBookData cached = getCachedData(worldName, playerUuid, bookType);
        return cached != null ? cached.data : null;
    }

    /**
     * Save book data for a player in a specific world.
     *
     * @param world The world
     * @param playerUuid The player's UUID
     * @param bookType The book type
     * @param data The data to save
     */
    public void saveBookData(@Nonnull World world, @Nonnull UUID playerUuid,
                             @Nonnull BookType bookType, @Nonnull HexBookData data) {
        String worldName = world.getName();

        // Update cache
        CachedBookData cached = getCachedData(worldName, playerUuid, bookType);
        if (cached != null && cached.data == data) {
            cached.dirty.set(true);
        } else {
            CachedBookData newCached = new CachedBookData(data);
            newCached.dirty.set(true);
            putInCache(worldName, playerUuid, bookType, newCached);
        }

        // Write to disk
        saveToDisk(world, playerUuid, bookType, data);
    }

    /**
     * Mark book data as dirty (needs saving).
     * Call this after modifying cached data.
     *
     * @param worldName The world name
     * @param playerUuid The player's UUID
     * @param bookType The book type
     */
    public void markDirty(@Nonnull String worldName, @Nonnull UUID playerUuid, @Nonnull BookType bookType) {
        CachedBookData cached = getCachedData(worldName, playerUuid, bookType);
        if (cached != null) {
            cached.dirty.set(true);
        }
    }

    /**
     * Save all dirty data for a specific world.
     * Should be called when a world is unloading.
     *
     * @param world The world
     * @return CompletableFuture that completes when all saves are done
     */
    @Nonnull
    public CompletableFuture<Void> saveWorld(@Nonnull World world) {
        String worldName = world.getName();
        Map<UUID, Map<BookType, CachedBookData>> worldCache = cache.get(worldName);

        if (worldCache == null || worldCache.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        int savedCount = 0;
        for (Map.Entry<UUID, Map<BookType, CachedBookData>> playerEntry : worldCache.entrySet()) {
            UUID playerUuid = playerEntry.getKey();
            for (Map.Entry<BookType, CachedBookData> bookEntry : playerEntry.getValue().entrySet()) {
                BookType bookType = bookEntry.getKey();
                CachedBookData cached = bookEntry.getValue();

                if (cached.dirty.getAndSet(false)) {
                    saveToDisk(world, playerUuid, bookType, cached.data);
                    savedCount++;
                }
            }
        }

        if (savedCount > 0) {
            LOGGER.at(Level.INFO).log("Saved %d book data files for world '%s'", savedCount, worldName);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Clear cache for a specific world.
     * Should be called when a world is unloaded (after saving).
     *
     * @param world The world
     */
    public void onWorldUnload(@Nonnull World world) {
        String worldName = world.getName();
        Map<UUID, Map<BookType, CachedBookData>> removed = cache.remove(worldName);
        if (removed != null) {
            int playerCount = removed.size();
            int bookCount = removed.values().stream().mapToInt(Map::size).sum();
            LOGGER.at(Level.INFO).log("Cleared cache for world '%s': %d players, %d books",
                    worldName, playerCount, bookCount);
        }
    }

    // ==================== DISK I/O ====================

    /**
     * Load book data from disk.
     */
    @Nonnull
    private HexBookData loadFromDisk(@Nonnull World world, @Nonnull UUID playerUuid, @Nonnull BookType bookType) {
        Path filePath = getBookFilePath(world, playerUuid, bookType);

        if (!Files.exists(filePath)) {
            LOGGER.at(Level.FINE).log("No saved book data at %s, creating new", filePath);
            return new HexBookData();
        }

        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            // Check version for future migration support
            int version = json.has("version") ? json.get("version").getAsInt() : 1;
            if (version != DATA_VERSION) {
                LOGGER.at(Level.WARNING).log("Book data version mismatch at %s: expected %d, got %d",
                        filePath, DATA_VERSION, version);
                // Future: Add migration logic here
            }

            // Parse the data section
            if (json.has("data") && json.get("data").isJsonObject()) {
                return HexBookData.fromJson(json.getAsJsonObject("data"));
            } else {
                // Legacy format - try parsing root as data
                return HexBookData.fromJson(json);
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("Failed to load book data from %s", filePath);
            return new HexBookData();
        }
    }

    /**
     * Save book data to disk.
     */
    private void saveToDisk(@Nonnull World world, @Nonnull UUID playerUuid,
                            @Nonnull BookType bookType, @Nonnull HexBookData data) {
        Path filePath = getBookFilePath(world, playerUuid, bookType);

        try {
            // Ensure parent directories exist
            Path parentDir = filePath.getParent();
            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // Build JSON with metadata
            JsonObject root = new JsonObject();
            root.addProperty("version", DATA_VERSION);
            root.addProperty("bookType", bookType.getFileId());
            root.addProperty("playerUuid", playerUuid.toString());
            root.addProperty("worldName", world.getName());
            root.addProperty("savedAt", System.currentTimeMillis());
            root.add("data", data.toJson());

            // Write to file
            try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }

            LOGGER.at(Level.FINE).log("Saved book data to %s", filePath);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).withCause(e).log("Failed to save book data to %s", filePath);
        }
    }

    /**
     * Save all dirty data across all worlds.
     */
    private void saveAllDirty() {
        int totalSaved = 0;

        for (Map.Entry<String, Map<UUID, Map<BookType, CachedBookData>>> worldEntry : cache.entrySet()) {
            String worldName = worldEntry.getKey();

            for (Map.Entry<UUID, Map<BookType, CachedBookData>> playerEntry : worldEntry.getValue().entrySet()) {
                for (Map.Entry<BookType, CachedBookData> bookEntry : playerEntry.getValue().entrySet()) {
                    CachedBookData cached = bookEntry.getValue();

                    if (cached.dirty.get()) {
                        // Note: We can't save without the World object for the path
                        // This is called during shutdown, so we log a warning
                        LOGGER.at(Level.WARNING).log(
                                "Dirty book data not saved during shutdown: world=%s, player=%s, book=%s",
                                worldName, playerEntry.getKey(), bookEntry.getKey().getFileId());
                        totalSaved++;
                    }
                }
            }
        }

        if (totalSaved > 0) {
            LOGGER.at(Level.WARNING).log("%d dirty book data entries may have been lost", totalSaved);
        }
    }

    // ==================== CACHE HELPERS ====================

    @Nullable
    private CachedBookData getCachedData(@Nonnull String worldName, @Nonnull UUID playerUuid, @Nonnull BookType bookType) {
        Map<UUID, Map<BookType, CachedBookData>> worldCache = cache.get(worldName);
        if (worldCache == null) return null;

        Map<BookType, CachedBookData> playerCache = worldCache.get(playerUuid);
        if (playerCache == null) return null;

        return playerCache.get(bookType);
    }

    private void putInCache(@Nonnull String worldName, @Nonnull UUID playerUuid,
                            @Nonnull BookType bookType, @Nonnull CachedBookData data) {
        cache.computeIfAbsent(worldName, k -> new ConcurrentHashMap<>())
             .computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
             .put(bookType, data);
    }

    // ==================== STATISTICS ====================

    /**
     * Get the number of cached worlds.
     */
    public int getCachedWorldCount() {
        return cache.size();
    }

    /**
     * Get the total number of cached book entries.
     */
    public int getTotalCachedBooks() {
        return cache.values().stream()
                .flatMap(worldCache -> worldCache.values().stream())
                .mapToInt(Map::size)
                .sum();
    }
}
