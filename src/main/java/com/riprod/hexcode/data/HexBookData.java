package com.riprod.hexcode.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.riprod.hexcode.hex.HexNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Per-book data stored in world-scoped storage.
 *
 * <p>Each HexBook has a unique UUID that serves as the storage key.
 * Data is persisted in the world folder at:
 * {@code <world>/hexcode/books/<book-uuid>.json}
 *
 * <p>Contains:
 * <ul>
 *   <li><b>bookId</b>: Unique identifier for this physical book</li>
 *   <li><b>ownerId</b>: UUID of the player who created the book</li>
 *   <li><b>Glyphs</b>: Map of glyph ID to GlyphInstance (accuracy, speed, usage)</li>
 *   <li><b>SavedHexes</b>: List of saved HexNode root configurations</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * // Read from world storage
 * HexBookData data = WorldHexDataStore.get().loadBook(world, bookUuid);
 *
 * // Modify
 * data.recordGlyphDrawing("hexcode:fire", 0.95f, 2.3f);
 *
 * // Save back
 * WorldHexDataStore.get().saveBook(world, data);
 * </pre>
 *
 * @see GlyphInstance
 * @see HexNode
 */
public class HexBookData {

    /** Metadata key for storing book UUID in ItemStack */
    public static final String METADATA_KEY = "HexBookData";

    /** Item metadata key for the book's UUID */
    public static final String BOOK_UUID_KEY = "hexbook_uuid";

    /** Maximum number of saved hexes per book */
    public static final int MAX_SAVED_HEXES = 20;

    /** Unique identifier for this book instance */
    private UUID bookId;

    /** UUID of the player who created/owns this book */
    private UUID ownerId;

    /** When this book was created */
    private long createdAt;

    /** When this book was last modified */
    private long lastModifiedAt;

    private final Map<String, GlyphInstance> glyphs;
    private final List<HexNode> savedHexes;

    /**
     * Create empty book data with a new UUID.
     */
    public HexBookData() {
        this.bookId = UUID.randomUUID();
        this.ownerId = null;
        this.createdAt = System.currentTimeMillis();
        this.lastModifiedAt = this.createdAt;
        this.glyphs = new HashMap<>();
        this.savedHexes = new ArrayList<>();
    }

    /**
     * Create empty book data for a specific owner.
     *
     * @param ownerId The UUID of the player who owns this book
     */
    public HexBookData(@Nonnull UUID ownerId) {
        this.bookId = UUID.randomUUID();
        this.ownerId = ownerId;
        this.createdAt = System.currentTimeMillis();
        this.lastModifiedAt = this.createdAt;
        this.glyphs = new HashMap<>();
        this.savedHexes = new ArrayList<>();
    }

    /**
     * Create book data with existing data.
     */
    public HexBookData(Map<String, GlyphInstance> glyphs, List<HexNode> savedHexes) {
        this.bookId = UUID.randomUUID();
        this.ownerId = null;
        this.createdAt = System.currentTimeMillis();
        this.lastModifiedAt = this.createdAt;
        this.glyphs = new HashMap<>(glyphs);
        this.savedHexes = new ArrayList<>(savedHexes);
    }

    /**
     * Create book data with all fields specified.
     */
    public HexBookData(@Nonnull UUID bookId, @Nullable UUID ownerId,
                       Map<String, GlyphInstance> glyphs, List<HexNode> savedHexes) {
        this.bookId = bookId;
        this.ownerId = ownerId;
        this.createdAt = System.currentTimeMillis();
        this.lastModifiedAt = this.createdAt;
        this.glyphs = new HashMap<>(glyphs);
        this.savedHexes = new ArrayList<>(savedHexes);
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Create a new empty HexBookData for a specific owner.
     * This is the preferred factory method for creating new book data
     * to be stored in ItemStack metadata.
     *
     * @param ownerId The UUID of the player who owns this book
     * @return A new HexBookData instance
     */
    @Nonnull
    public static HexBookData createNew(@Nonnull UUID ownerId) {
        HexBookData data = new HexBookData();
        data.ownerId = ownerId;
        return data;
    }

    /**
     * Create a new empty HexBookData with a specific book ID and owner.
     *
     * @param bookId The UUID for this book
     * @param ownerId The UUID of the player who owns this book
     * @return A new HexBookData instance
     */
    @Nonnull
    public static HexBookData createNew(@Nonnull UUID bookId, @Nonnull UUID ownerId) {
        HexBookData data = new HexBookData();
        data.bookId = bookId;
        data.ownerId = ownerId;
        return data;
    }

    // ==================== BOOK IDENTITY ====================

    /**
     * Get the unique book ID.
     *
     * @return The book's UUID
     */
    @Nonnull
    public UUID getBookId() {
        return bookId;
    }

    /**
     * Set the book ID (used during migration or deserialization).
     *
     * @param bookId The book's UUID
     */
    public void setBookId(@Nonnull UUID bookId) {
        this.bookId = bookId;
        markModified();
    }

    /**
     * Get the owner's player UUID.
     *
     * @return The owner's UUID, or null if not set
     */
    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    /**
     * Set the owner's player UUID.
     *
     * @param ownerId The owner's UUID
     */
    public void setOwnerId(@Nullable UUID ownerId) {
        this.ownerId = ownerId;
        markModified();
    }

    /**
     * Get when this book was created.
     *
     * @return Creation timestamp (epoch millis)
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Set when this book was created (used by codec deserialization).
     *
     * @param createdAt Creation timestamp (epoch millis)
     */
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Get when this book was last modified.
     *
     * @return Last modification timestamp (epoch millis)
     */
    public long getLastModifiedAt() {
        return lastModifiedAt;
    }

    /**
     * Set when this book was last modified (used by codec deserialization).
     *
     * @param lastModifiedAt Last modification timestamp (epoch millis)
     */
    public void setLastModifiedAt(long lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    /**
     * Mark this book as modified (updates lastModifiedAt).
     */
    public void markModified() {
        this.lastModifiedAt = System.currentTimeMillis();
    }

    // ==================== GLYPH OPERATIONS ====================

    /**
     * Check if this book has a glyph recorded.
     */
    public boolean hasGlyph(@Nonnull String glyphId) {
        return glyphs.containsKey(glyphId);
    }

    /**
     * Get glyph data, or null if not in book.
     */
    @Nullable
    public GlyphInstance getGlyphData(@Nonnull String glyphId) {
        return glyphs.get(glyphId);
    }

    /**
     * Get accuracy for a glyph (defaults to 0.75 if not drawn).
     */
    public float getAccuracy(@Nonnull String glyphId) {
        GlyphInstance data = glyphs.get(glyphId);
        return data != null ? data.getAccuracy() : 0.75f;
    }

    /**
     * Get all glyph IDs in this book.
     */
    @Nonnull
    public Set<String> getGlyphIds() {
        return Collections.unmodifiableSet(glyphs.keySet());
    }

    /**
     * Get all glyph data entries.
     */
    @Nonnull
    public Collection<GlyphInstance> getAllGlyphData() {
        return Collections.unmodifiableCollection(glyphs.values());
    }

    /**
     * Get glyph count.
     */
    public int getGlyphCount() {
        return glyphs.size();
    }

    /**
     * Record a glyph drawing. If glyph exists, keeps best accuracy.
     *
     * @param glyphId The glyph ID (e.g., "hexcode:fire")
     * @param accuracy Drawing accuracy 0.0-1.0
     * @param drawSpeed Time in seconds to draw
     */
    public void recordGlyphDrawing(@Nonnull String glyphId, float accuracy, float drawSpeed) {
        GlyphInstance existing = glyphs.get(glyphId);
        if (existing == null) {
            glyphs.put(glyphId, GlyphInstance.fromDrawing(glyphId, accuracy, drawSpeed));
        } else {
            glyphs.put(glyphId, existing.withBestAccuracy(accuracy, drawSpeed));
        }
    }

    /**
     * Add a glyph with default values (learned but not drawn).
     */
    public void addGlyphWithDefault(@Nonnull String glyphId) {
        if (!glyphs.containsKey(glyphId)) {
            glyphs.put(glyphId, GlyphInstance.initial(glyphId));
        }
    }

    /**
     * Remove a glyph from the book.
     */
    public boolean removeGlyph(@Nonnull String glyphId) {
        return glyphs.remove(glyphId) != null;
    }

    /**
     * Record that a glyph was used (increment usage count).
     */
    public void recordGlyphUsage(@Nonnull String glyphId) {
        GlyphInstance existing = glyphs.get(glyphId);
        if (existing != null) {
            glyphs.put(glyphId, existing.withIncrementedUsage());
        }
    }

    // ==================== SAVED HEX OPERATIONS ====================

    /**
     * Get all saved hexes (as HexNode roots).
     */
    @Nonnull
    public List<HexNode> getSavedHexes() {
        return Collections.unmodifiableList(savedHexes);
    }

    /**
     * Check if a saved hex exists by id.
     *
     * @param id The hex node id to check
     * @return true if a hex with this id exists
     */
    public boolean hasSavedHex(@Nonnull String id) {
        for (HexNode node : savedHexes) {
            if (node.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a saved hex by id.
     */
    @Nullable
    public HexNode getSavedHex(@Nonnull String id) {
        for (HexNode node : savedHexes) {
            if (node.getId().equals(id)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Get a saved hex by index.
     */
    @Nullable
    public HexNode getSavedHex(int index) {
        if (index >= 0 && index < savedHexes.size()) {
            return savedHexes.get(index);
        }
        return null;
    }

    /**
     * Save a hex node. If id exists, replaces it. Otherwise adds new.
     *
     * @param node The HexNode root to save
     * @param id Optional override id (uses node.getId() if null)
     * @return true if saved successfully, false if at max capacity
     */
    public boolean saveHex(@Nonnull HexNode node, @Nullable String id) {
        // Check if replacing existing
        if (id == null) {
            id = node.getId();
        }

        for (int i = 0; i < savedHexes.size(); i++) {
            if (savedHexes.get(i).getId().equals(id)) {
                savedHexes.set(i, node);
                return true;
            }
        }

        // Check capacity
        if (savedHexes.size() >= MAX_SAVED_HEXES) {
            return false;
        }

        savedHexes.add(node);
        return true;
    }

    /**
     * Delete a saved hex by id.
     */
    public boolean deleteSavedHex(@Nonnull String id) {
        return savedHexes.removeIf(node -> node.getId().equals(id));
    }

    /**
     * Record that a saved hex was used.
     * Note: Usage tracking is now handled externally or via glyph instances within the tree.
     */
    public void recordSavedHexUsage(@Nonnull String id) {
        // With unified HexNode treatment, usage tracking can be handled
        // by incrementing usage on all GlyphInstance values in the tree.
        // For now, this is a no-op as the usage count was on the Hex wrapper.
    }

    /**
     * Get count of saved hexes.
     */
    public int getSavedHexCount() {
        return savedHexes.size();
    }

    /**
     * Check if at max saved hex capacity.
     */
    public boolean isAtMaxHexCapacity() {
        return savedHexes.size() >= MAX_SAVED_HEXES;
    }

    // ==================== CODEC HELPER METHODS ====================

    /**
     * Set glyphs from map (used by BuilderCodec).
     */
    public void setGlyphsFromMap(Map<String, GlyphInstance> map) {
        this.glyphs.clear();
        if (map != null) {
            this.glyphs.putAll(map);
        }
    }

    /**
     * Get glyphs as map (used by BuilderCodec).
     */
    public Map<String, GlyphInstance> getGlyphsAsMap() {
        return new HashMap<>(glyphs);
    }

    /**
     * Set saved hexes from list (used by BuilderCodec).
     */
    public void setSavedHexesFromList(List<HexNode> list) {
        this.savedHexes.clear();
        if (list != null) {
            this.savedHexes.addAll(list);
        }
    }

    /**
     * Get saved hexes as list (used by BuilderCodec).
     */
    public List<HexNode> getSavedHexesAsList() {
        return new ArrayList<>(savedHexes);
    }

    // ==================== JSON SERIALIZATION ====================

    /**
     * Serialize to JSON object.
     */
    @Nonnull
    public JsonObject toJson() {
        JsonObject json = new JsonObject();

        // Serialize book identity
        json.addProperty("bookId", bookId.toString());
        if (ownerId != null) {
            json.addProperty("ownerId", ownerId.toString());
        }
        json.addProperty("createdAt", createdAt);
        json.addProperty("lastModifiedAt", lastModifiedAt);

        // Serialize glyphs
        JsonObject glyphsObj = new JsonObject();
        for (Map.Entry<String, GlyphInstance> entry : glyphs.entrySet()) {
            glyphsObj.add(entry.getKey(), entry.getValue().toJson());
        }
        json.add("glyphs", glyphsObj);

        // Serialize saved hexes (as HexNode roots)
        JsonArray hexesArray = new JsonArray();
        for (HexNode node : savedHexes) {
            hexesArray.add(node.toJson());
        }
        json.add("savedHexes", hexesArray);

        return json;
    }

    /**
     * Deserialize from JSON object.
     */
    @Nonnull
    public static HexBookData fromJson(@Nonnull JsonObject json) {
        HexBookData data = new HexBookData();

        // Deserialize book identity
        if (json.has("bookId")) {
            try {
                data.bookId = UUID.fromString(json.get("bookId").getAsString());
            } catch (IllegalArgumentException e) {
                // Keep auto-generated UUID if parse fails
            }
        }

        if (json.has("ownerId") && !json.get("ownerId").isJsonNull()) {
            try {
                data.ownerId = UUID.fromString(json.get("ownerId").getAsString());
            } catch (IllegalArgumentException e) {
                // Leave as null if parse fails
            }
        }

        if (json.has("createdAt")) {
            data.createdAt = json.get("createdAt").getAsLong();
        }

        if (json.has("lastModifiedAt")) {
            data.lastModifiedAt = json.get("lastModifiedAt").getAsLong();
        }

        // Deserialize glyphs
        if (json.has("glyphs") && json.get("glyphs").isJsonObject()) {
            JsonObject glyphsObj = json.getAsJsonObject("glyphs");
            for (Map.Entry<String, JsonElement> entry : glyphsObj.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    GlyphInstance glyphData = GlyphInstance.fromJson(entry.getValue().getAsJsonObject());
                    data.glyphs.put(entry.getKey(), glyphData);
                }
            }
        }

        // Deserialize saved hexes (as HexNode roots)
        if (json.has("savedHexes") && json.get("savedHexes").isJsonArray()) {
            JsonArray hexesArray = json.getAsJsonArray("savedHexes");
            for (JsonElement element : hexesArray) {
                if (element.isJsonObject()) {
                    HexNode node = HexNode.fromJson(element.getAsJsonObject());
                    // Recalculate layout after deserialization
                    node.recalculateLayout();
                    data.savedHexes.add(node);
                }
            }
        }

        return data;
    }

    // ==================== UTILITY ====================

    /**
     * Get summary string for debugging.
     */
    @Nonnull
    public String getSummary() {
        return String.format("HexBookData{bookId=%s, owner=%s, glyphs=%d, savedHexes=%d}",
                             bookId, ownerId != null ? ownerId.toString().substring(0, 8) + "..." : "none",
                             glyphs.size(), savedHexes.size());
    }

    @Override
    public String toString() {
        return getSummary();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HexBookData that = (HexBookData) o;
        return Objects.equals(bookId, that.bookId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookId);
    }
}
