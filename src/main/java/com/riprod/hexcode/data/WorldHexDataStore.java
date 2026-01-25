package com.riprod.hexcode.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.World;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.hex.HexSerializer;

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
 * Per-world, UUID-based book data storage manager.
 *
 * <p>This is the primary storage system for HexBookData, using book UUIDs as keys.
 * Each book has a unique UUID that persists with the item, allowing data to be
 * retrieved regardless of who holds the book.
 *
 * <h2>Storage Structure</h2>
 * <pre>
 * {world_save_path}/hexcode/books/
 * ├── {book-uuid}.json           (primary storage)
 * └── fallback/
 *     └── {player-uuid}/
 *         └── {book-hash}.json   (fallback for old books)
 * </pre>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>UUID-based storage - book data follows the book, not the player</li>
 *   <li>Per-world isolation - each world has its own book data</li>
 *   <li>Automatic caching with dirty flag for efficient saves</li>
 *   <li>Thread-safe operations using ConcurrentHashMap</li>
 *   <li>Fallback support for legacy player-based storage</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>
 * // Load book by UUID
 * HexBookData data = WorldHexDataStore.get().loadBook(world, bookUuid);
 *
 * // Modify data
 * data.recordGlyphDrawing("hexcode:fire", 0.95f, 2.3f);
 *
 * // Save (uses book's UUID)
 * WorldHexDataStore.get().saveBook(world, data);
 * </pre>
 *
 * @see HexBookData
 * @see WorldBookDataStore
 */
public class WorldHexDataStore {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String HEXCODE_DIR = "hexcode";
    private static final String BOOKS_DIR = "books";
    private static final String FALLBACK_DIR = "fallback";
    private static final int DATA_VERSION = 2;

    private static WorldHexDataStore instance;

    /**
     * Cache structure: worldName -> bookUUID -> CachedBookData
     */
    private final Map<String, Map<UUID, CachedBookData>> cache = new ConcurrentHashMap<>();

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

    private WorldHexDataStore() {}

    /**
     * Initialize the WorldHexDataStore singleton.
     * Should be called during plugin setup.
     */
    public static void initialize() {
        if (instance == null) {
            instance = new WorldHexDataStore();
            LOGGER.at(Level.INFO).log("WorldHexDataStore initialized (UUID-based storage)");
        }
    }

    /**
     * Get the singleton instance.
     */
    @Nonnull
    public static WorldHexDataStore get() {
        if (instance == null) {
            instance = new WorldHexDataStore();
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
            LOGGER.at(Level.INFO).log("WorldHexDataStore shutdown complete");
        }
    }

    // ==================== PATH RESOLUTION ====================

    /**
     * Get the books directory for a world.
     *
     * @param world The world
     * @return Path to {world_save_path}/hexcode/books/
     */
    @Nonnull
    private Path getBooksDir(@Nonnull World world) {
        return world.getSavePath().resolve(HEXCODE_DIR).resolve(BOOKS_DIR);
    }

    /**
     * Get the path to a specific book data file by UUID.
     *
     * @param world The world
     * @param bookId The book's UUID
     * @return Path to {world_save_path}/hexcode/books/{book-uuid}.json
     */
    @Nonnull
    private Path getBookFilePath(@Nonnull World world, @Nonnull UUID bookId) {
        return getBooksDir(world).resolve(bookId.toString() + ".json");
    }

    /**
     * Get the fallback directory for legacy player-based storage.
     *
     * @param world The world
     * @param playerId The player's UUID
     * @return Path to {world_save_path}/hexcode/books/fallback/{player-uuid}/
     */
    @Nonnull
    private Path getFallbackDir(@Nonnull World world, @Nonnull UUID playerId) {
        return getBooksDir(world).resolve(FALLBACK_DIR).resolve(playerId.toString());
    }

    /**
     * Get the fallback path for a book without UUID.
     *
     * @param world The world
     * @param playerId The player's UUID
     * @param bookHash A hash derived from book properties
     * @return Path to the fallback file
     */
    @Nonnull
    private Path getFallbackFilePath(@Nonnull World world, @Nonnull UUID playerId, @Nonnull String bookHash) {
        return getFallbackDir(world, playerId).resolve(bookHash + ".json");
    }

    /**
     * Get the path to a book's queued hex file.
     *
     * @param world The world
     * @param bookId The book's UUID
     * @return Path to {world_save_path}/hexcode/books/{book-uuid}/queued.json
     */
    @Nonnull
    private Path getQueuedHexPath(@Nonnull World world, @Nonnull UUID bookId) {
        return getBooksDir(world).resolve(bookId.toString()).resolve("queued.json");
    }

    // ==================== QUEUED HEX OPERATIONS ====================

    /**
     * Get the queued hex for a book.
     *
     * <p>Queued hexes are stored in world files at:
     * {@code {world}/hexcode/books/{book-uuid}/queued.json}
     *
     * <p>This is the recommended approach for frequently-changing data like
     * queued spells, as it avoids the complexity of updating immutable ItemStacks.
     *
     * @param world The world
     * @param bookUuid The book's UUID
     * @return The queued Hex, or null if not set or empty
     */
    @Nullable
    public Hex getQueuedHex(@Nonnull World world, @Nonnull UUID bookUuid) {
        Path path = getQueuedHexPath(world, bookUuid);

        if (!Files.exists(path)) {
            return null;
        }

        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            if (json == null || json.isEmpty() || "{}".equals(json.trim())) {
                return null;
            }

            Hex hex = HexSerializer.deserialize(json);
            if (hex != null && hex.isEmpty()) {
                return null;
            }

            LOGGER.at(Level.FINE).log("Loaded queued hex for book %s", bookUuid);
            return hex;
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).withCause(e).log("Failed to read queued hex from %s", path);
            return null;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("Failed to deserialize queued hex for book %s", bookUuid);
            return null;
        }
    }

    /**
     * Set the queued hex for a book.
     *
     * <p>Queued hexes are stored in world files at:
     * {@code {world}/hexcode/books/{book-uuid}/queued.json}
     *
     * <p>If hex is null or empty, the queued hex file is deleted.
     *
     * @param world The world
     * @param bookUuid The book's UUID
     * @param hex The hex to queue, or null to clear
     */
    public void setQueuedHex(@Nonnull World world, @Nonnull UUID bookUuid, @Nullable Hex hex) {
        Path path = getQueuedHexPath(world, bookUuid);

        try {
            if (hex == null || hex.isEmpty()) {
                // Clear queued hex by deleting file
                if (Files.exists(path)) {
                    Files.delete(path);
                    LOGGER.at(Level.FINE).log("Cleared queued hex for book %s", bookUuid);
                }
                return;
            }

            // Ensure parent directories exist
            Path parentDir = path.getParent();
            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // Serialize and write
            String json = HexSerializer.serializePretty(hex);
            if (json == null) {
                LOGGER.at(Level.WARNING).log("Failed to serialize hex for book %s", bookUuid);
                return;
            }

            Files.writeString(path, json, StandardCharsets.UTF_8);
            LOGGER.at(Level.FINE).log("Saved queued hex for book %s to %s", bookUuid, path);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).withCause(e).log("Failed to write queued hex to %s", path);
        }
    }

    /**
     * Check if a book has a queued hex.
     *
     * @param world The world
     * @param bookUuid The book's UUID
     * @return true if a queued hex exists
     */
    public boolean hasQueuedHex(@Nonnull World world, @Nonnull UUID bookUuid) {
        Path path = getQueuedHexPath(world, bookUuid);
        return Files.exists(path);
    }

    /**
     * Clear the queued hex for a book.
     *
     * @param world The world
     * @param bookUuid The book's UUID
     */
    public void clearQueuedHex(@Nonnull World world, @Nonnull UUID bookUuid) {
        setQueuedHex(world, bookUuid, null);
    }

    // ==================== BOOK LOADING ====================

    /**
     * Load book data by UUID.
     * Creates new empty data if book doesn't exist.
     *
     * @param world The world
     * @param bookId The book's UUID
     * @return The HexBookData (existing or new empty with the given UUID)
     */
    @Nonnull
    public HexBookData loadBook(@Nonnull World world, @Nonnull UUID bookId) {
        String worldName = world.getName();

        // Check cache first
        CachedBookData cached = getCachedData(worldName, bookId);
        if (cached != null) {
            return cached.data;
        }

        // Load from disk
        HexBookData data = loadFromDisk(world, bookId);

        // Ensure the book ID matches
        if (!data.getBookId().equals(bookId)) {
            data.setBookId(bookId);
        }

        // Cache it
        putInCache(worldName, bookId, new CachedBookData(data));

        return data;
    }

    /**
     * Load book data by UUID, returning null if not found.
     *
     * @param world The world
     * @param bookId The book's UUID
     * @return The HexBookData, or null if not found
     */
    @Nullable
    public HexBookData loadBookIfExists(@Nonnull World world, @Nonnull UUID bookId) {
        String worldName = world.getName();

        // Check cache first
        CachedBookData cached = getCachedData(worldName, bookId);
        if (cached != null) {
            return cached.data;
        }

        // Check if file exists
        Path filePath = getBookFilePath(world, bookId);
        if (!Files.exists(filePath)) {
            return null;
        }

        return loadBook(world, bookId);
    }

    /**
     * Load book data using fallback storage (for migration).
     *
     * @param world The world
     * @param playerId The player's UUID
     * @param bookHash The book hash
     * @return The HexBookData, or null if not found
     */
    @Nullable
    public HexBookData loadFallback(@Nonnull World world, @Nonnull UUID playerId, @Nonnull String bookHash) {
        Path fallbackPath = getFallbackFilePath(world, playerId, bookHash);

        if (!Files.exists(fallbackPath)) {
            return null;
        }

        try (Reader reader = Files.newBufferedReader(fallbackPath, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return parseBookJson(json);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("Failed to load fallback book data from %s", fallbackPath);
            return null;
        }
    }

    // ==================== BOOK SAVING ====================

    /**
     * Save book data to world storage.
     * Uses the book's UUID as the storage key.
     *
     * @param world The world
     * @param data The book data to save
     */
    public void saveBook(@Nonnull World world, @Nonnull HexBookData data) {
        UUID bookId = data.getBookId();
        String worldName = world.getName();

        // Mark as modified
        data.markModified();

        // Update cache
        CachedBookData cached = getCachedData(worldName, bookId);
        if (cached != null && cached.data == data) {
            cached.dirty.set(true);
        } else {
            CachedBookData newCached = new CachedBookData(data);
            newCached.dirty.set(true);
            putInCache(worldName, bookId, newCached);
        }

        // Write to disk
        saveToDisk(world, data);
    }

    /**
     * Mark book data as dirty (needs saving).
     *
     * @param worldName The world name
     * @param bookId The book's UUID
     */
    public void markDirty(@Nonnull String worldName, @Nonnull UUID bookId) {
        CachedBookData cached = getCachedData(worldName, bookId);
        if (cached != null) {
            cached.dirty.set(true);
            cached.data.markModified();
        }
    }

    /**
     * Save all dirty books for a specific world.
     *
     * @param world The world
     * @return CompletableFuture that completes when all saves are done
     */
    @Nonnull
    public CompletableFuture<Void> saveWorld(@Nonnull World world) {
        String worldName = world.getName();
        Map<UUID, CachedBookData> worldCache = cache.get(worldName);

        if (worldCache == null || worldCache.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        int savedCount = 0;
        for (Map.Entry<UUID, CachedBookData> entry : worldCache.entrySet()) {
            CachedBookData cached = entry.getValue();

            if (cached.dirty.getAndSet(false)) {
                saveToDisk(world, cached.data);
                savedCount++;
            }
        }

        if (savedCount > 0) {
            LOGGER.at(Level.INFO).log("Saved %d book data files for world '%s'", savedCount, worldName);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Clear cache for a specific world.
     *
     * @param world The world
     */
    public void onWorldUnload(@Nonnull World world) {
        String worldName = world.getName();
        Map<UUID, CachedBookData> removed = cache.remove(worldName);
        if (removed != null && !removed.isEmpty()) {
            LOGGER.at(Level.INFO).log("Cleared cache for world '%s': %d books", worldName, removed.size());
        }
    }

    // ==================== MIGRATION ====================

    /**
     * Migrate a book from old format (item metadata) to new UUID-based storage.
     *
     * @param world The world
     * @param data The book data to migrate (must have valid UUID set)
     * @param playerId The player who owned the book (for fallback tracking)
     * @return true if migration successful
     */
    public boolean migrateBook(@Nonnull World world, @Nonnull HexBookData data, @Nonnull UUID playerId) {
        // Ensure book has an owner set
        if (data.getOwnerId() == null) {
            data.setOwnerId(playerId);
        }

        // Save to new UUID-based location
        saveBook(world, data);

        LOGGER.at(Level.INFO).log("Migrated book %s (owner: %s) to UUID-based storage",
                data.getBookId(), playerId);

        return true;
    }

    /**
     * Check if a book exists in storage.
     *
     * @param world The world
     * @param bookId The book's UUID
     * @return true if the book exists
     */
    public boolean bookExists(@Nonnull World world, @Nonnull UUID bookId) {
        // Check cache
        String worldName = world.getName();
        if (getCachedData(worldName, bookId) != null) {
            return true;
        }

        // Check disk
        return Files.exists(getBookFilePath(world, bookId));
    }

    // ==================== DISK I/O ====================

    /**
     * Load book data from disk.
     */
    @Nonnull
    private HexBookData loadFromDisk(@Nonnull World world, @Nonnull UUID bookId) {
        Path filePath = getBookFilePath(world, bookId);

        if (!Files.exists(filePath)) {
            LOGGER.at(Level.FINE).log("No saved book data at %s, creating new", filePath);
            HexBookData newData = new HexBookData();
            newData.setBookId(bookId);
            return newData;
        }

        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return parseBookJson(json);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("Failed to load book data from %s", filePath);
            HexBookData fallback = new HexBookData();
            fallback.setBookId(bookId);
            return fallback;
        }
    }

    /**
     * Parse book JSON with version handling.
     */
    @Nonnull
    private HexBookData parseBookJson(@Nonnull JsonObject json) {
        // Check version
        int version = json.has("version") ? json.get("version").getAsInt() : 1;

        if (version < DATA_VERSION) {
            LOGGER.at(Level.INFO).log("Migrating book data from version %d to %d", version, DATA_VERSION);
            // Future: Add migration logic here
        }

        // Parse data section (or root for legacy format)
        if (json.has("data") && json.get("data").isJsonObject()) {
            return HexBookData.fromJson(json.getAsJsonObject("data"));
        } else {
            return HexBookData.fromJson(json);
        }
    }

    /**
     * Save book data to disk.
     */
    private void saveToDisk(@Nonnull World world, @Nonnull HexBookData data) {
        Path filePath = getBookFilePath(world, data.getBookId());

        try {
            // Ensure parent directories exist
            Path parentDir = filePath.getParent();
            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // Build JSON with metadata
            JsonObject root = new JsonObject();
            root.addProperty("version", DATA_VERSION);
            root.addProperty("bookId", data.getBookId().toString());
            if (data.getOwnerId() != null) {
                root.addProperty("ownerId", data.getOwnerId().toString());
            }
            root.addProperty("worldName", world.getName());
            root.addProperty("savedAt", System.currentTimeMillis());
            root.add("data", data.toJson());

            // Write to file
            try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }

            LOGGER.at(Level.FINE).log("Saved book %s to %s", data.getBookId(), filePath);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).withCause(e).log("Failed to save book %s to %s", data.getBookId(), filePath);
        }
    }

    /**
     * Save all dirty data across all worlds.
     */
    private void saveAllDirty() {
        int totalDirty = 0;

        for (Map.Entry<String, Map<UUID, CachedBookData>> worldEntry : cache.entrySet()) {
            for (Map.Entry<UUID, CachedBookData> bookEntry : worldEntry.getValue().entrySet()) {
                if (bookEntry.getValue().dirty.get()) {
                    totalDirty++;
                    // Note: We can't save without the World object
                    LOGGER.at(Level.WARNING).log(
                            "Dirty book not saved during shutdown: world=%s, bookId=%s",
                            worldEntry.getKey(), bookEntry.getKey());
                }
            }
        }

        if (totalDirty > 0) {
            LOGGER.at(Level.WARNING).log("%d dirty book entries may need manual save", totalDirty);
        }
    }

    // ==================== CACHE HELPERS ====================

    @Nullable
    private CachedBookData getCachedData(@Nonnull String worldName, @Nonnull UUID bookId) {
        Map<UUID, CachedBookData> worldCache = cache.get(worldName);
        if (worldCache == null) return null;
        return worldCache.get(bookId);
    }

    private void putInCache(@Nonnull String worldName, @Nonnull UUID bookId, @Nonnull CachedBookData data) {
        cache.computeIfAbsent(worldName, k -> new ConcurrentHashMap<>())
             .put(bookId, data);
    }

    // ==================== STATISTICS ====================

    /**
     * Get the number of cached worlds.
     */
    public int getCachedWorldCount() {
        return cache.size();
    }

    /**
     * Get the total number of cached books.
     */
    public int getTotalCachedBooks() {
        return cache.values().stream()
                .mapToInt(Map::size)
                .sum();
    }
}
