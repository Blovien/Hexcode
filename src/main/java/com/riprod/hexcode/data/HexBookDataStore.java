package com.riprod.hexcode.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages persistent storage of HexBookData keyed by UUID.
 *
 * <p>This follows the Hytale pattern (similar to BarterShopState) where
 * data is stored in a JSON file in the plugin's data directory, keyed by
 * the book's UUID rather than storing data directly on the item.
 *
 * <p>Usage:
 * <pre>
 * // Initialize during plugin setup
 * HexBookDataStore.initialize(this.getDataDirectory());
 *
 * // Get or create data for a book UUID
 * HexBookData data = HexBookDataStore.get().getOrCreateBookData(uuid);
 *
 * // Modify and save
 * data.recordGlyphDrawing("hexcode:fire", 0.95f, 2.3f);
 * HexBookDataStore.save();
 * </pre>
 *
 * @see HexBookData
 * @see HexBookDataManager
 */
public class HexBookDataStore {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static HexBookDataStore instance;
    private static Path saveDirectory;

    /** Map of book UUID string to HexBookData */
    private final Map<String, HexBookData> bookDataMap = new ConcurrentHashMap<>();

    /**
     * Initialize the data store with the plugin's data directory.
     *
     * @param dataDirectory The plugin's data directory from getDataDirectory()
     */
    public static void initialize(@Nonnull Path dataDirectory) {
        saveDirectory = dataDirectory;
        load();
    }

    /**
     * Get the singleton instance, creating if needed.
     */
    @Nonnull
    public static HexBookDataStore get() {
        if (instance == null) {
            instance = new HexBookDataStore();
        }
        return instance;
    }

    /**
     * Load data from the JSON file.
     */
    public static void load() {
        if (saveDirectory == null) {
            LOGGER.at(Level.WARNING).log("Cannot load hex book data: save directory not set");
            instance = new HexBookDataStore();
            return;
        }

        Path file = saveDirectory.resolve("hex_book_data.json");
        if (!Files.exists(file)) {
            LOGGER.at(Level.INFO).log("No saved hex book data found, starting fresh");
            instance = new HexBookDataStore();
            return;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject rootJson = JsonParser.parseReader(reader).getAsJsonObject();
            instance = new HexBookDataStore();
            
            if (rootJson.has("books") && rootJson.get("books").isJsonObject()) {
                JsonObject booksJson = rootJson.getAsJsonObject("books");
                for (Map.Entry<String, JsonElement> entry : booksJson.entrySet()) {
                    if (entry.getValue().isJsonObject()) {
                        HexBookData bookData = HexBookData.fromJson(entry.getValue().getAsJsonObject());
                        instance.bookDataMap.put(entry.getKey(), bookData);
                    }
                }
            }
            
            LOGGER.at(Level.INFO).log("Loaded hex book data with %d books", instance.bookDataMap.size());
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("Failed to load hex book data, starting fresh");
            instance = new HexBookDataStore();
        }
    }

    /**
     * Save data to the JSON file.
     */
    public static void save() {
        if (saveDirectory == null || instance == null) {
            return;
        }

        try {
            if (!Files.exists(saveDirectory)) {
                Files.createDirectories(saveDirectory);
            }

            Path file = saveDirectory.resolve("hex_book_data.json");
            
            // Build JSON structure
            JsonObject rootJson = new JsonObject();
            JsonObject booksJson = new JsonObject();
            for (Map.Entry<String, HexBookData> entry : instance.bookDataMap.entrySet()) {
                booksJson.add(entry.getKey(), entry.getValue().toJson());
            }
            rootJson.add("books", booksJson);
            
            // Write to file
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(rootJson, writer);
            }
            
            LOGGER.at(Level.FINE).log("Saved hex book data");
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).withCause(e).log("Failed to save hex book data");
        }
    }

    /**
     * Shutdown and save data.
     */
    public static void shutdown() {
        save();
        instance = null;
    }

    // ==================== DATA ACCESS ====================

    /**
     * Generate a new UUID for a hex book.
     *
     * @return New UUID string
     */
    @Nonnull
    public static String generateBookUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Check if book data exists for a UUID.
     *
     * @param bookUUID The book's UUID
     * @return true if data exists
     */
    public boolean hasBookData(@Nonnull String bookUUID) {
        return bookDataMap.containsKey(bookUUID);
    }

    /**
     * Get book data for a UUID, or null if not found.
     *
     * @param bookUUID The book's UUID
     * @return HexBookData or null
     */
    @Nullable
    public HexBookData getBookData(@Nonnull String bookUUID) {
        return bookDataMap.get(bookUUID);
    }

    /**
     * Get or create book data for a UUID.
     *
     * @param bookUUID The book's UUID
     * @return HexBookData (new or existing)
     */
    @Nonnull
    public HexBookData getOrCreateBookData(@Nonnull String bookUUID) {
        return bookDataMap.computeIfAbsent(bookUUID, k -> new HexBookData());
    }

    /**
     * Set book data for a UUID.
     *
     * @param bookUUID The book's UUID
     * @param data The HexBookData to store
     */
    public void setBookData(@Nonnull String bookUUID, @Nonnull HexBookData data) {
        bookDataMap.put(bookUUID, data);
    }

    /**
     * Remove book data for a UUID.
     *
     * @param bookUUID The book's UUID
     * @return The removed data, or null if not found
     */
    @Nullable
    public HexBookData removeBookData(@Nonnull String bookUUID) {
        return bookDataMap.remove(bookUUID);
    }

    /**
     * Get the number of stored books.
     */
    public int getBookCount() {
        return bookDataMap.size();
    }
}
