package com.riprod.hexcode.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.hex.Hex;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Per-book data stored in Hex Book item metadata.
 *
 * Contains:
 * - Glyphs: Map of glyph ID to BookGlyphInstanceData (accuracy, speed, usage)
 * - SavedHexes: List of saved Hex configurations
 *
 * Usage:
 * <pre>
 * // Read from ItemStack
 * HexBookData data = HexBookDataManager.getData(hexBookStack);
 *
 * // Modify
 * data.recordGlyphDrawing("hexcode:fire", 0.95f, 2.3f);
 *
 * // Write back (creates new ItemStack)
 * ItemStack newStack = HexBookDataManager.withData(hexBookStack, data);
 * </pre>
 *
 * @see HexBookDataManager
 * @see GlyphInstance
 * @see Hex
 */
public class HexBookData {

    /** Metadata key for storing HexBookData in ItemStack */
    public static final String METADATA_KEY = "HexBookData";

    /** Maximum number of saved hexes per book */
    public static final int MAX_SAVED_HEXES = 20;

    private final Map<String, GlyphInstance> glyphs;
    private final List<Hex> savedHexes;

    /**
     * Create empty book data.
     */
    public HexBookData() {
        this.glyphs = new HashMap<>();
        this.savedHexes = new ArrayList<>();
    }

    /**
     * Create book data with existing data.
     */
    public HexBookData(Map<String, GlyphInstance> glyphs, List<Hex> savedHexes) {
        this.glyphs = new HashMap<>(glyphs);
        this.savedHexes = new ArrayList<>(savedHexes);
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
     * Get all saved hexes.
     */
    @Nonnull
    public List<Hex> getSavedHexes() {
        return Collections.unmodifiableList(savedHexes);
    }

    /**
     * Get a saved hex by name.
     */
    @Nullable
    public Hex getSavedHex(@Nonnull String name) {
        for (Hex hex : savedHexes) {
            if (hex.getId().equals(name)) {
                return hex;
            }
        }
        return null;
    }

    /**
     * Get a saved hex by index.
     */
    @Nullable
    public Hex getSavedHex(int index) {
        if (index >= 0 && index < savedHexes.size()) {
            return savedHexes.get(index);
        }
        return null;
    }

    /**
     * Save a hex. If name exists, replaces it. Otherwise adds new.
     *
     * @param name Display name for the hex
     * @param hexString Serialized hex string (e.g., "BEAM[POWER[FIRE[]]]")
     * @return true if saved successfully, false if at max capacity
     */
    public boolean saveHex(@Nonnull Hex hex, @Nullable String id) {
        // Check if replacing existing
        if (id == null) {
            id = hex.getId();
        }

        for (int i = 0; i < savedHexes.size(); i++) {
            if (savedHexes.get(i).getId().equals(id)) {
                savedHexes.set(i, hex);
                return true;
            }
        }

        // Check capacity
        if (savedHexes.size() >= MAX_SAVED_HEXES) {
            return false;
        }

        savedHexes.add(hex);
        return true;
    }

    /**
     * Delete a saved hex by name.
     */
    public boolean deleteSavedHex(@Nonnull String id) {
        return savedHexes.removeIf(hex -> hex.getId().equals(id));
    }

    /**
     * Record that a saved hex was used.
     */
    public void recordSavedHexUsage(@Nonnull String name) {
        for (int i = 0; i < savedHexes.size(); i++) {
            if (savedHexes.get(i).getId().equals(name)) {
                savedHexes.set(i, savedHexes.get(i).withIncrementedUses());
                return;
            }
        }
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
    void setGlyphsFromMap(Map<String, GlyphInstance> map) {
        this.glyphs.clear();
        if (map != null) {
            this.glyphs.putAll(map);
        }
    }

    /**
     * Get glyphs as map (used by BuilderCodec).
     */
    Map<String, GlyphInstance> getGlyphsAsMap() {
        return new HashMap<>(glyphs);
    }

    /**
     * Set saved hexes from list (used by BuilderCodec).
     */
    void setSavedHexesFromList(List<Hex> list) {
        this.savedHexes.clear();
        if (list != null) {
            this.savedHexes.addAll(list);
        }
    }

    /**
     * Get saved hexes as list (used by BuilderCodec).
     */
    List<Hex> getSavedHexesAsList() {
        return new ArrayList<>(savedHexes);
    }

    // ==================== JSON SERIALIZATION ====================

    /**
     * Serialize to JSON object.
     */
    @Nonnull
    public JsonObject toJson() {
        JsonObject json = new JsonObject();

        // Serialize glyphs
        JsonObject glyphsObj = new JsonObject();
        for (Map.Entry<String, GlyphInstance> entry : glyphs.entrySet()) {
            glyphsObj.add(entry.getKey(), entry.getValue().toJson());
        }
        json.add("glyphs", glyphsObj);

        // Serialize saved hexes
        JsonArray hexesArray = new JsonArray();
        for (Hex hex : savedHexes) {
            hexesArray.add(hex.toJson());
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

        // Deserialize saved hexes
        if (json.has("savedHexes") && json.get("savedHexes").isJsonArray()) {
            JsonArray hexesArray = json.getAsJsonArray("savedHexes");
            for (JsonElement element : hexesArray) {
                if (element.isJsonObject()) {
                    Hex hexData = Hex.fromJson(element.getAsJsonObject());
                    data.savedHexes.add(hexData);
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
        return String.format("HexBookData{glyphs=%d, savedHexes=%d}",
                             glyphs.size(), savedHexes.size());
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
