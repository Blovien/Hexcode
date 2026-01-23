# Hex Book Per-Item Data Storage Implementation Plan

## Implementation Status

**Last Updated**: 2026-01-23

### Phase 1-3: Data Classes ✅ COMPLETE
- [x] `BookGlyphInstanceData.java` - Per-glyph data with BSON serialization
- [x] `SavedHexData.java` - Saved hex configuration
- [x] `HexBookData.java` - Main container for book metadata
- [x] `HexBookDataManager.java` - Utility for reading/writing book data

### Phase 4: Commands ✅ COMPLETE
- [x] `/hexcode save <name>` - Save current hex to book
- [x] `/hexcode hexes` - List saved hexes in book
- [x] `/hexcode deletehex <name>` - Delete saved hex from book

### Phase 3: GlyphMode Integration ✅ COMPLETE
- [x] `GlyphMode.java` - Added bookData field and methods
- [x] `GlyphMode.java` - Updated spawnOrbitalGlyphs to include saved hexes
- [x] `GlyphModeManager.java` - Now passes book data when entering glyph mode

### Phase 5: Castable Elements ✅ COMPLETE
- [x] `CastableElement.java` - Interface for glyphs and saved hexes
- [x] `SavedHexElement.java` - Wrapper for saved hexes
- [x] `OrbitalSavedHexEntity.java` - Orbital entity for saved hexes
- [x] `CompositionState.java` - Updated for saved hex support

### Phase 6: Integration ✅ COMPLETE
- [x] `HexExecutor.java` - Now uses book data for accuracy (with legacy fallback)
- [x] Glyph usage recording to book

### Remaining Work (Manual Testing Required)
- [ ] In-game testing of all features

---

## Overview

This document describes the architectural change from **per-player** glyph/hex storage to **per-Hex_Book (item)** storage. After this implementation:

- Each Hex Book item stores its own glyphs (with accuracy/speed data) and saved Hexes
- When a player draws a glyph, it's stored in the currently held book's data
- In Casting Mode, available glyphs come from the book's stored glyphs
- Players can save composed Hexes into the book via `/hexcode save`
- Saved Hexes appear as "castable elements" alongside glyphs in Casting Mode

---

## Key Hytale APIs (from /docs/extracted)

### ItemStack Metadata API

**Location**: `docs/extracted/com/hypixel/hytale/server/core/inventory/ItemStack.java`

ItemStack has a `BsonDocument metadata` field for storing arbitrary per-item data.

**Key Methods**:
```java
// Write metadata (returns NEW ItemStack - immutable pattern)
public <T> ItemStack withMetadata(@Nonnull String key, @Nonnull Codec<T> codec, @Nullable T data)
public <T> ItemStack withMetadata(@Nonnull KeyedCodec<T> keyedCodec, @Nullable T data)

// Read metadata
@Nullable
public <T> T getFromMetadataOrNull(@Nonnull String key, @Nonnull Codec<T> codec)
@Nullable
public <T> T getFromMetadataOrNull(@Nonnull KeyedCodec<T> keyedCodec)
public <T> T getFromMetadataOrDefault(@Nonnull String key, @Nonnull BuilderCodec<T> codec)
```

**CRITICAL**: `withMetadata()` returns a NEW ItemStack. You must replace the item in inventory:
```java
ItemStack oldStack = inventory.getItemAt(slot);
HexBookData data = oldStack.getFromMetadataOrNull(HexBookData.KEYED_CODEC);
data.addGlyph(...);
ItemStack newStack = oldStack.withMetadata(HexBookData.KEYED_CODEC, data);
inventory.setItemStackForSlot(slot, newStack);  // Replace in inventory
```

### Codec System

**Location**: `docs/extracted/com/hypixel/hytale/codec/`

- `Codec<T>` - Base interface for serialization
- `BuilderCodec<T>` - Declarative codec builder with defaults
- `KeyedCodec<T>` - Type-safe named field wrapper

**Example Pattern** (from AdventureMetadata):
```java
public class HexBookData {
    public static final String KEY = "HexBookData";

    public static final BuilderCodec<HexBookData> CODEC = BuilderCodec.builder(HexBookData.class, HexBookData::new)
        .append(new KeyedCodec<>("Glyphs", glyphMapCodec),
                (data, glyphs) -> data.glyphs = glyphs,
                data -> data.glyphs)
        .add()
        .append(new KeyedCodec<>("SavedHexes", hexListCodec),
                (data, hexes) -> data.savedHexes = hexes,
                data -> data.savedHexes)
        .add()
        .build();

    public static final KeyedCodec<HexBookData> KEYED_CODEC = new KeyedCodec<>(KEY, CODEC);
}
```

---

## Current Architecture (What Exists)

### Per-Player Glyph Data

**Files**:
- `src/main/java/com/riprod/hexcode/data/GlyphInstanceData.java` - Single glyph's accuracy/speed
- `src/main/java/com/riprod/hexcode/data/PlayerGlyphData.java` - All glyphs for one player
- `src/main/java/com/riprod/hexcode/data/PlayerGlyphDataManager.java` - Load/save per-player data

**Current Storage**: `{dataDirectory}/playerdata/{uuid}/hexcode.json`

**GlyphInstanceData Fields**:
```java
String baseGlyphId      // e.g., "hexcode:fire"
float accuracy          // 0.0-1.0
float drawSpeed         // Seconds to draw
long drawnTimestamp     // When drawn (epoch ms)
int timesUsed           // Cast count
```

### GlyphMode and Loadout

**Files**:
- `src/main/java/com/riprod/hexcode/mode/GlyphMode.java` - Player's casting mode state
- `src/main/java/com/riprod/hexcode/loadout/Loadout.java` - List of glyph IDs (max 12)
- `src/main/java/com/riprod/hexcode/loadout/LoadoutManager.java` - Per-player loadouts (in-memory)

**Current Flow**:
1. Player enters Glyph Mode → `GlyphModeManager.toggleGlyphMode()`
2. Mode receives `Loadout` from `LoadoutManager.getLoadout(playerId)`
3. Loadout resolves glyph IDs → `GlyphRegistry` → spawns orbital entities
4. Player data (accuracy) comes from `PlayerGlyphDataManager.getOrCreatePlayerData(playerId)`

### Hex Structure

**Files**:
- `src/main/java/com/riprod/hexcode/hex/Hex.java` - Tree of glyphs
- `src/main/java/com/riprod/hexcode/hex/HexNode.java` - Node in hex tree
- `src/main/java/com/riprod/hexcode/hex/HexSerializer.java` - Serialize to/from string format

**Hex Format**: `BEAM[POWER[FIRE[]]]` (nested bracket notation)

### Hex_Book Item

**File**: `src/main/resources/Server/Item/Items/Tool/Book/Hex_Book.json`

Currently just an item definition with no custom data storage. Used as offhand requirement for Glyph Mode.

---

## Implementation Steps

### Phase 1: Create HexBookData Classes

#### Step 1.1: Create BookGlyphInstanceData

Create a new class that mirrors `GlyphInstanceData` but is designed for per-book storage.

**File**: `src/main/java/com/riprod/hexcode/data/BookGlyphInstanceData.java`

```java
package com.riprod.hexcode.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import javax.annotation.Nonnull;

/**
 * Immutable glyph instance data stored per-book.
 *
 * Contains drawing accuracy, speed, and usage statistics for a single glyph
 * as recorded in a specific Hex Book.
 */
public class BookGlyphInstanceData {

    public static final BuilderCodec<BookGlyphInstanceData> CODEC = BuilderCodec.builder(
            BookGlyphInstanceData.class, BookGlyphInstanceData::new)
        .append(new KeyedCodec<>("GlyphId", Codec.STRING),
                (d, v) -> d.glyphId = v, d -> d.glyphId)
        .add()
        .append(new KeyedCodec<>("Accuracy", Codec.FLOAT),
                (d, v) -> d.accuracy = v, d -> d.accuracy)
        .add()
        .append(new KeyedCodec<>("DrawSpeed", Codec.FLOAT),
                (d, v) -> d.drawSpeed = v, d -> d.drawSpeed)
        .add()
        .append(new KeyedCodec<>("DrawnTimestamp", Codec.LONG),
                (d, v) -> d.drawnTimestamp = v, d -> d.drawnTimestamp)
        .add()
        .append(new KeyedCodec<>("TimesUsed", Codec.INTEGER),
                (d, v) -> d.timesUsed = v, d -> d.timesUsed)
        .add()
        .build();

    private String glyphId;
    private float accuracy;
    private float drawSpeed;
    private long drawnTimestamp;
    private int timesUsed;

    public BookGlyphInstanceData() {
        // Default constructor for codec
        this.glyphId = "";
        this.accuracy = 0.75f;
        this.drawSpeed = 0f;
        this.drawnTimestamp = System.currentTimeMillis();
        this.timesUsed = 0;
    }

    public BookGlyphInstanceData(String glyphId, float accuracy, float drawSpeed,
                                  long drawnTimestamp, int timesUsed) {
        this.glyphId = glyphId;
        this.accuracy = Math.max(0f, Math.min(1f, accuracy));
        this.drawSpeed = Math.max(0f, drawSpeed);
        this.drawnTimestamp = drawnTimestamp;
        this.timesUsed = Math.max(0, timesUsed);
    }

    // Factory methods
    public static BookGlyphInstanceData initial(String glyphId) {
        return new BookGlyphInstanceData(glyphId, 0.75f, 0f, System.currentTimeMillis(), 0);
    }

    public static BookGlyphInstanceData fromDrawing(String glyphId, float accuracy, float drawSpeed) {
        return new BookGlyphInstanceData(glyphId, accuracy, drawSpeed, System.currentTimeMillis(), 0);
    }

    // Getters
    public String getGlyphId() { return glyphId; }
    public float getAccuracy() { return accuracy; }
    public float getDrawSpeed() { return drawSpeed; }
    public long getDrawnTimestamp() { return drawnTimestamp; }
    public int getTimesUsed() { return timesUsed; }

    // Immutable update methods (return new instance)
    public BookGlyphInstanceData withBestAccuracy(float newAccuracy, float newDrawSpeed) {
        float bestAccuracy = Math.max(this.accuracy, newAccuracy);
        return new BookGlyphInstanceData(glyphId, bestAccuracy, newDrawSpeed,
                                         System.currentTimeMillis(), timesUsed);
    }

    public BookGlyphInstanceData withIncrementedUsage() {
        return new BookGlyphInstanceData(glyphId, accuracy, drawSpeed, drawnTimestamp, timesUsed + 1);
    }

    public String getQualityRating() {
        if (accuracy >= 0.98f) return "Perfect";
        if (accuracy >= 0.85f) return "Excellent";
        if (accuracy >= 0.70f) return "Good";
        if (accuracy >= 0.50f) return "Fair";
        return "Poor";
    }
}
```

**Key Differences from GlyphInstanceData**:
- Uses Hytale `BuilderCodec` instead of manual BSON/JSON serialization
- Designed for ItemStack metadata storage, not file-based storage

#### Step 1.2: Create SavedHexData

Create a class to store saved Hex configurations.

**File**: `src/main/java/com/riprod/hexcode/data/SavedHexData.java`

```java
package com.riprod.hexcode.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A saved Hex configuration stored in a Hex Book.
 *
 * Saved Hexes can be cast like individual glyphs in Casting Mode.
 * They are stored as serialized strings using HexSerializer format.
 */
public class SavedHexData {

    public static final BuilderCodec<SavedHexData> CODEC = BuilderCodec.builder(
            SavedHexData.class, SavedHexData::new)
        .append(new KeyedCodec<>("Name", Codec.STRING),
                (d, v) -> d.name = v, d -> d.name)
        .add()
        .append(new KeyedCodec<>("HexString", Codec.STRING),
                (d, v) -> d.hexString = v, d -> d.hexString)
        .add()
        .append(new KeyedCodec<>("CreatedTimestamp", Codec.LONG),
                (d, v) -> d.createdTimestamp = v, d -> d.createdTimestamp)
        .add()
        .append(new KeyedCodec<>("TimesUsed", Codec.INTEGER),
                (d, v) -> d.timesUsed = v, d -> d.timesUsed)
        .add()
        .build();

    private String name;           // Display name for the hex
    private String hexString;      // Serialized format: "BEAM[POWER[FIRE[]]]"
    private long createdTimestamp;
    private int timesUsed;

    public SavedHexData() {
        this.name = "";
        this.hexString = "";
        this.createdTimestamp = System.currentTimeMillis();
        this.timesUsed = 0;
    }

    public SavedHexData(String name, String hexString) {
        this.name = name;
        this.hexString = hexString;
        this.createdTimestamp = System.currentTimeMillis();
        this.timesUsed = 0;
    }

    // Getters
    public String getName() { return name; }
    public String getHexString() { return hexString; }
    public long getCreatedTimestamp() { return createdTimestamp; }
    public int getTimesUsed() { return timesUsed; }

    // Setters (for codec)
    public void setName(String name) { this.name = name; }
    public void setHexString(String hexString) { this.hexString = hexString; }

    public SavedHexData withIncrementedUsage() {
        SavedHexData copy = new SavedHexData(name, hexString);
        copy.createdTimestamp = this.createdTimestamp;
        copy.timesUsed = this.timesUsed + 1;
        return copy;
    }
}
```

#### Step 1.3: Create HexBookData (Main Container)

**File**: `src/main/java/com/riprod/hexcode/data/HexBookData.java`

```java
package com.riprod.hexcode.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.types.MapCodec;
import com.hypixel.hytale.codec.types.ListCodec;
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
 * HexBookData data = itemStack.getFromMetadataOrNull(HexBookData.KEYED_CODEC);
 * if (data == null) data = new HexBookData();
 *
 * // Modify
 * data.recordGlyphDrawing("hexcode:fire", 0.95f, 2.3f);
 *
 * // Write back (creates new ItemStack)
 * ItemStack newStack = itemStack.withMetadata(HexBookData.KEYED_CODEC, data);
 * </pre>
 */
public class HexBookData {

    public static final String KEY = "HexBookData";

    // Codec for Map<String, BookGlyphInstanceData>
    private static final Codec<Map<String, BookGlyphInstanceData>> GLYPH_MAP_CODEC =
        new MapCodec<>(Codec.STRING, BookGlyphInstanceData.CODEC);

    // Codec for List<SavedHexData>
    private static final Codec<List<SavedHexData>> HEX_LIST_CODEC =
        new ListCodec<>(SavedHexData.CODEC);

    public static final BuilderCodec<HexBookData> CODEC = BuilderCodec.builder(
            HexBookData.class, HexBookData::new)
        .append(new KeyedCodec<>("Glyphs", GLYPH_MAP_CODEC),
                (d, v) -> d.glyphs = new HashMap<>(v),
                d -> d.glyphs)
        .add()
        .append(new KeyedCodec<>("SavedHexes", HEX_LIST_CODEC),
                (d, v) -> d.savedHexes = new ArrayList<>(v),
                d -> d.savedHexes)
        .add()
        .build();

    public static final KeyedCodec<HexBookData> KEYED_CODEC = new KeyedCodec<>(KEY, CODEC);

    // Maximum limits
    public static final int MAX_SAVED_HEXES = 20;

    private Map<String, BookGlyphInstanceData> glyphs;
    private List<SavedHexData> savedHexes;

    public HexBookData() {
        this.glyphs = new HashMap<>();
        this.savedHexes = new ArrayList<>();
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
    public BookGlyphInstanceData getGlyphData(@Nonnull String glyphId) {
        return glyphs.get(glyphId);
    }

    /**
     * Get accuracy for a glyph (defaults to 0.75 if not drawn).
     */
    public float getAccuracy(@Nonnull String glyphId) {
        BookGlyphInstanceData data = glyphs.get(glyphId);
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
    public Collection<BookGlyphInstanceData> getAllGlyphData() {
        return Collections.unmodifiableCollection(glyphs.values());
    }

    /**
     * Record a glyph drawing. If glyph exists, keeps best accuracy.
     *
     * @param glyphId The glyph ID (e.g., "hexcode:fire")
     * @param accuracy Drawing accuracy 0.0-1.0
     * @param drawSpeed Time in seconds to draw
     */
    public void recordGlyphDrawing(@Nonnull String glyphId, float accuracy, float drawSpeed) {
        BookGlyphInstanceData existing = glyphs.get(glyphId);
        if (existing == null) {
            glyphs.put(glyphId, BookGlyphInstanceData.fromDrawing(glyphId, accuracy, drawSpeed));
        } else {
            glyphs.put(glyphId, existing.withBestAccuracy(accuracy, drawSpeed));
        }
    }

    /**
     * Add a glyph with default values (learned but not drawn).
     */
    public void addGlyphWithDefault(@Nonnull String glyphId) {
        if (!glyphs.containsKey(glyphId)) {
            glyphs.put(glyphId, BookGlyphInstanceData.initial(glyphId));
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
        BookGlyphInstanceData existing = glyphs.get(glyphId);
        if (existing != null) {
            glyphs.put(glyphId, existing.withIncrementedUsage());
        }
    }

    // ==================== SAVED HEX OPERATIONS ====================

    /**
     * Get all saved hexes.
     */
    @Nonnull
    public List<SavedHexData> getSavedHexes() {
        return Collections.unmodifiableList(savedHexes);
    }

    /**
     * Get a saved hex by name.
     */
    @Nullable
    public SavedHexData getSavedHex(@Nonnull String name) {
        for (SavedHexData hex : savedHexes) {
            if (hex.getName().equals(name)) {
                return hex;
            }
        }
        return null;
    }

    /**
     * Get a saved hex by index.
     */
    @Nullable
    public SavedHexData getSavedHex(int index) {
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
    public boolean saveHex(@Nonnull String name, @Nonnull String hexString) {
        // Check if replacing existing
        for (int i = 0; i < savedHexes.size(); i++) {
            if (savedHexes.get(i).getName().equals(name)) {
                savedHexes.set(i, new SavedHexData(name, hexString));
                return true;
            }
        }

        // Check capacity
        if (savedHexes.size() >= MAX_SAVED_HEXES) {
            return false;
        }

        savedHexes.add(new SavedHexData(name, hexString));
        return true;
    }

    /**
     * Delete a saved hex by name.
     */
    public boolean deleteSavedHex(@Nonnull String name) {
        return savedHexes.removeIf(hex -> hex.getName().equals(name));
    }

    /**
     * Record that a saved hex was used.
     */
    public void recordSavedHexUsage(@Nonnull String name) {
        for (int i = 0; i < savedHexes.size(); i++) {
            if (savedHexes.get(i).getName().equals(name)) {
                savedHexes.set(i, savedHexes.get(i).withIncrementedUsage());
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
```

---

### Phase 2: Create HexBookDataManager Utility

This utility class provides helper methods for reading/writing HexBookData from ItemStacks, and for finding the player's currently held Hex Book.

**File**: `src/main/java/com/riprod/hexcode/data/HexBookDataManager.java`

```java
package com.riprod.hexcode.data;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.util.HexStaffUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utility class for managing HexBookData on Hex Book items.
 *
 * Provides methods to:
 * - Find a player's currently held Hex Book
 * - Read HexBookData from an ItemStack
 * - Write HexBookData to an ItemStack (replacing in inventory)
 */
public class HexBookDataManager {

    public static final String HEX_BOOK_ITEM_ID = "hexcode:hex_book";

    private HexBookDataManager() {} // Utility class

    // ==================== ITEM DETECTION ====================

    /**
     * Check if an ItemStack is a Hex Book.
     */
    public static boolean isHexBook(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        return itemStack.getItemId().equals(HEX_BOOK_ITEM_ID);
    }

    /**
     * Find the Hex Book in player's offhand (utility slot 0).
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @return The Hex Book ItemStack, or null if not found
     */
    @Nullable
    public static ItemStack findHeldHexBook(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        InventoryComponent invComp = store.getComponent(playerRef, InventoryComponent.getComponentType());
        if (invComp == null) {
            return null;
        }

        Inventory inventory = invComp.getInventory();
        if (inventory == null) {
            return null;
        }

        // Hex Book is in utility slot (offhand)
        ItemContainer utility = inventory.getUtility();
        if (utility == null) {
            return null;
        }

        ItemStack offhand = utility.getItemStack((short) 0);
        if (isHexBook(offhand)) {
            return offhand;
        }

        return null;
    }

    /**
     * Get the slot index where Hex Book is located (for updates).
     * Returns -1 if not found.
     */
    public static short findHexBookSlot(Inventory inventory) {
        if (inventory == null) return -1;

        ItemContainer utility = inventory.getUtility();
        if (utility == null) return -1;

        ItemStack offhand = utility.getItemStack((short) 0);
        if (isHexBook(offhand)) {
            return 0;  // Utility slot 0
        }

        return -1;
    }

    // ==================== DATA ACCESS ====================

    /**
     * Read HexBookData from an ItemStack.
     * Returns new empty HexBookData if none exists.
     */
    @Nonnull
    public static HexBookData getData(@Nonnull ItemStack hexBook) {
        HexBookData data = hexBook.getFromMetadataOrNull(HexBookData.KEYED_CODEC);
        return data != null ? data : new HexBookData();
    }

    /**
     * Read HexBookData from player's held Hex Book.
     * Returns null if player doesn't have a Hex Book equipped.
     */
    @Nullable
    public static HexBookData getHeldBookData(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        ItemStack hexBook = findHeldHexBook(store, playerRef);
        if (hexBook == null) {
            return null;
        }
        return getData(hexBook);
    }

    // ==================== DATA MODIFICATION ====================

    /**
     * Create a new ItemStack with updated HexBookData.
     *
     * IMPORTANT: This returns a NEW ItemStack. You must replace it in inventory!
     */
    @Nonnull
    public static ItemStack withData(@Nonnull ItemStack hexBook, @Nonnull HexBookData data) {
        return hexBook.withMetadata(HexBookData.KEYED_CODEC, data);
    }

    /**
     * Update the Hex Book in player's inventory with new data.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param data The new HexBookData to save
     * @return true if updated successfully, false if no Hex Book found
     */
    public static boolean updateHeldBookData(Store<EntityStore> store,
                                              Ref<EntityStore> playerRef,
                                              @Nonnull HexBookData data) {
        InventoryComponent invComp = store.getComponent(playerRef, InventoryComponent.getComponentType());
        if (invComp == null) {
            return false;
        }

        Inventory inventory = invComp.getInventory();
        if (inventory == null) {
            return false;
        }

        ItemContainer utility = inventory.getUtility();
        if (utility == null) {
            return false;
        }

        ItemStack oldStack = utility.getItemStack((short) 0);
        if (!isHexBook(oldStack)) {
            return false;
        }

        // Create new ItemStack with updated metadata
        ItemStack newStack = withData(oldStack, data);

        // Replace in inventory
        utility.setItemStackForSlot((short) 0, newStack);

        return true;
    }

    /**
     * Record a glyph drawing in the player's held Hex Book.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param glyphId The glyph that was drawn
     * @param accuracy Drawing accuracy 0.0-1.0
     * @param drawSpeed Time in seconds to draw
     * @return true if recorded successfully
     */
    public static boolean recordGlyphDrawing(Store<EntityStore> store,
                                              Ref<EntityStore> playerRef,
                                              @Nonnull String glyphId,
                                              float accuracy,
                                              float drawSpeed) {
        HexBookData data = getHeldBookData(store, playerRef);
        if (data == null) {
            return false;
        }

        data.recordGlyphDrawing(glyphId, accuracy, drawSpeed);
        return updateHeldBookData(store, playerRef, data);
    }

    /**
     * Save a hex to the player's held Hex Book.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param name Display name for the hex
     * @param hexString Serialized hex string
     * @return true if saved successfully, false if no book or at capacity
     */
    public static boolean saveHex(Store<EntityStore> store,
                                   Ref<EntityStore> playerRef,
                                   @Nonnull String name,
                                   @Nonnull String hexString) {
        HexBookData data = getHeldBookData(store, playerRef);
        if (data == null) {
            return false;
        }

        if (!data.saveHex(name, hexString)) {
            return false;  // At max capacity
        }

        return updateHeldBookData(store, playerRef, data);
    }
}
```

---

### Phase 3: Modify GlyphMode to Use Book Data

#### Step 3.1: Update GlyphMode Constructor

**File**: `src/main/java/com/riprod/hexcode/mode/GlyphMode.java`

The `GlyphMode` class needs to derive available glyphs from the book's data instead of `Loadout`.

**Changes Required**:

1. Add field for `HexBookData` reference
2. Modify `getAvailableGlyphs()` to return glyphs from book
3. Add saved hexes as "castable elements"
4. Update orbital glyph spawning to include saved hexes

```java
// ADD these imports
import com.riprod.hexcode.data.HexBookData;
import com.riprod.hexcode.data.SavedHexData;

// In GlyphMode class, ADD:
private HexBookData bookData;  // Data from the held Hex Book

// MODIFY constructor to accept HexBookData:
public GlyphMode(Ref<EntityStore> player, Loadout loadout, HexBookData bookData) {
    this.player = player;
    this.loadout = loadout;  // Keep for backwards compatibility
    this.bookData = bookData;  // NEW: book-specific data
    this.composition = new CompositionState();
    // ... rest of constructor
}

// ADD method to get available glyphs from book:
/**
 * Get glyphs available for casting from the book's data.
 * This replaces the loadout-based approach.
 */
public List<Glyph> getAvailableGlyphsFromBook() {
    if (bookData == null) {
        return Collections.emptyList();
    }

    GlyphRegistry registry = GlyphRegistry.getInstance();
    List<Glyph> glyphs = new ArrayList<>();

    for (String glyphId : bookData.getGlyphIds()) {
        Glyph glyph = registry.getGlyph(glyphId);
        if (glyph != null) {
            glyphs.add(glyph);
        }
    }

    return glyphs;
}

// ADD method to get accuracy for a glyph:
/**
 * Get accuracy for a glyph from book data.
 */
public float getGlyphAccuracy(String glyphId) {
    if (bookData == null) {
        return 0.75f;  // Default
    }
    return bookData.getAccuracy(glyphId);
}

// ADD method to get saved hexes:
/**
 * Get saved hexes that can be cast like glyphs.
 */
public List<SavedHexData> getSavedHexes() {
    if (bookData == null) {
        return Collections.emptyList();
    }
    return bookData.getSavedHexes();
}

// ADD method to update book data reference:
public void setBookData(HexBookData bookData) {
    this.bookData = bookData;
}

public HexBookData getBookData() {
    return bookData;
}
```

#### Step 3.2: Update GlyphModeManager

**File**: `src/main/java/com/riprod/hexcode/mode/GlyphModeManager.java`

Modify `toggleGlyphMode()` to pass book data to GlyphMode.

```java
// ADD import
import com.riprod.hexcode.data.HexBookData;
import com.riprod.hexcode.data.HexBookDataManager;

// MODIFY toggleGlyphMode method:
public boolean toggleGlyphMode(UUID playerId, Ref<EntityStore> playerRef,
                                Store<EntityStore> store,
                                CommandBuffer<EntityStore> commandBuffer) {
    GlyphMode existing = sessions.get(playerId);

    if (existing != null && existing.isActive()) {
        // Exit mode
        existing.exit(commandBuffer);
        sessions.remove(playerId);
        return false;
    } else {
        // Enter mode - get book data
        HexBookData bookData = null;
        if (store != null && playerRef != null) {
            bookData = HexBookDataManager.getHeldBookData(store, playerRef);
        }

        if (bookData == null) {
            // No Hex Book equipped or no data - use default empty book
            bookData = new HexBookData();
        }

        // Get loadout (kept for backwards compatibility)
        Loadout loadout = LoadoutManager.getInstance().getLoadout(playerId);

        // Create mode with book data
        GlyphMode mode = new GlyphMode(playerRef, loadout, bookData);
        mode.enter(commandBuffer);
        sessions.put(playerId, mode);
        return true;
    }
}
```

#### Step 3.3: Update spawnOrbitalGlyphs

In `GlyphMode.java`, modify `spawnOrbitalGlyphs()` to spawn glyphs from book data AND saved hexes.

```java
private void spawnOrbitalGlyphs(CommandBuffer<EntityStore> commandBuffer) {
    orbitalEntities.clear();

    Vector3d playerPosition = getPlayerPosition(commandBuffer.getStore());

    // Get glyphs from book data (instead of loadout)
    List<Glyph> glyphs = getAvailableGlyphsFromBook();

    // Also get saved hexes - they will appear as orbital entities too
    List<SavedHexData> savedHexes = getSavedHexes();

    int totalEntities = glyphs.size() + savedHexes.size();
    float angleStep = (float) (2 * Math.PI / Math.max(1, totalEntities));

    int index = 0;

    // Spawn glyph orbitals
    for (Glyph glyph : glyphs) {
        float initialAngle = angleStep * index;
        OrbitalGlyphEntity orbitalEntity = new OrbitalGlyphEntity(glyph, player, initialAngle);
        orbitalEntity.spawn(commandBuffer, playerPosition);
        orbitalEntities.add(orbitalEntity);
        index++;
    }

    // Spawn saved hex orbitals (need new OrbitalHexEntity or modify OrbitalGlyphEntity)
    // See Phase 5 for saved hex orbital implementation

    LOGGER.atInfo().log("Spawned %d orbital entities (%d glyphs, %d saved hexes)",
                        orbitalEntities.size(), glyphs.size(), savedHexes.size());
}
```

---

### Phase 4: Implement /hexcode save Command

**File**: `src/main/java/com/riprod/hexcode/command/HexcodeCommand.java`

Add the `save` subcommand to save the current composition to the book.

```java
// In execute() switch statement, ADD:
case "save":
    saveHex(ctx, store, ref, playerRef, world, args);
    break;

// ADD this new method:
private void saveHex(CommandContext ctx, Store<EntityStore> store,
                     Ref<EntityStore> ref, PlayerRef playerRef,
                     World world, String args) {
    // Parse name from args: /hexcode save <name>
    String[] parts = args.split(" ", 2);
    String hexName = null;

    if (parts.length >= 2 && !parts[1].isEmpty()) {
        hexName = parts[1].trim();
    }

    UUID playerId = playerRef.getUuid();
    if (playerId == null) {
        ctx.sendMessage(Message.raw("Could not get player ID"));
        return;
    }

    // Get current composition
    GlyphModeManager manager = GlyphModeManager.getInstance();
    GlyphMode mode = manager.getSession(playerId);

    if (mode == null || mode.getComposition().isEmpty()) {
        ctx.sendMessage(Message.raw("No hex composition to save. Enter glyph mode and compose a hex first."));
        return;
    }

    Hex hex = mode.getComposition().getHex();
    if (!hex.isValid()) {
        ctx.sendMessage(Message.raw("Current composition is not a valid hex."));
        return;
    }

    // Generate name if not provided
    if (hexName == null || hexName.isEmpty()) {
        // Auto-generate name based on root glyph
        HexNode root = hex.getRoot();
        hexName = root != null ? root.getGlyph().getDisplayName() + " Hex" : "Unnamed Hex";
    }

    // Serialize the hex
    HexSerializer serializer = new HexSerializer();
    String hexString = serializer.serialize(hex);

    // Save to book
    boolean saved = HexBookDataManager.saveHex(store, ref, hexName, hexString);

    if (saved) {
        ctx.sendMessage(Message.raw("Saved hex: " + hexName));
        ctx.sendMessage(Message.raw("  Structure: " + hexString));

        // Get updated count
        HexBookData data = HexBookDataManager.getHeldBookData(store, ref);
        if (data != null) {
            ctx.sendMessage(Message.raw("  Book now has " + data.getSavedHexCount() +
                                        "/" + HexBookData.MAX_SAVED_HEXES + " saved hexes"));
        }
    } else {
        // Check if it's because of capacity
        HexBookData data = HexBookDataManager.getHeldBookData(store, ref);
        if (data == null) {
            ctx.sendMessage(Message.raw("Failed to save: No Hex Book equipped in offhand"));
        } else if (data.isAtMaxHexCapacity()) {
            ctx.sendMessage(Message.raw("Failed to save: Book is at max capacity (" +
                                        HexBookData.MAX_SAVED_HEXES + " hexes). Delete some to make room."));
        } else {
            ctx.sendMessage(Message.raw("Failed to save hex"));
        }
    }
}

// Also add a list command to see saved hexes:
case "hexes":
    listSavedHexes(ctx, store, ref, playerRef);
    break;

private void listSavedHexes(CommandContext ctx, Store<EntityStore> store,
                            Ref<EntityStore> ref, PlayerRef playerRef) {
    HexBookData data = HexBookDataManager.getHeldBookData(store, ref);

    if (data == null) {
        ctx.sendMessage(Message.raw("No Hex Book equipped in offhand"));
        return;
    }

    List<SavedHexData> hexes = data.getSavedHexes();

    if (hexes.isEmpty()) {
        ctx.sendMessage(Message.raw("No saved hexes in this book"));
        return;
    }

    ctx.sendMessage(Message.raw("=== Saved Hexes (" + hexes.size() + "/" +
                                HexBookData.MAX_SAVED_HEXES + ") ==="));
    for (int i = 0; i < hexes.size(); i++) {
        SavedHexData hex = hexes.get(i);
        ctx.sendMessage(Message.raw("  " + (i+1) + ". " + hex.getName() +
                                    " - used " + hex.getTimesUsed() + " times"));
        ctx.sendMessage(Message.raw("     " + hex.getHexString()));
    }
}

// Add delete command:
case "deletehex":
    deleteSavedHex(ctx, store, ref, playerRef, args);
    break;

private void deleteSavedHex(CommandContext ctx, Store<EntityStore> store,
                            Ref<EntityStore> ref, PlayerRef playerRef, String args) {
    String[] parts = args.split(" ", 2);
    if (parts.length < 2) {
        ctx.sendMessage(Message.raw("Usage: /hexcode deletehex <name>"));
        return;
    }

    String hexName = parts[1].trim();
    HexBookData data = HexBookDataManager.getHeldBookData(store, ref);

    if (data == null) {
        ctx.sendMessage(Message.raw("No Hex Book equipped"));
        return;
    }

    if (data.deleteSavedHex(hexName)) {
        HexBookDataManager.updateHeldBookData(store, ref, data);
        ctx.sendMessage(Message.raw("Deleted hex: " + hexName));
    } else {
        ctx.sendMessage(Message.raw("No hex found with name: " + hexName));
    }
}

// UPDATE showHelp to include new commands:
private void showHelp(CommandContext ctx) {
    ctx.sendMessage(Message.raw("Hexcode Commands:"));
    // ... existing commands ...
    ctx.sendMessage(Message.raw("  /hexcode save <name>     - Save current hex to book"));
    ctx.sendMessage(Message.raw("  /hexcode hexes           - List saved hexes in book"));
    ctx.sendMessage(Message.raw("  /hexcode deletehex <name>- Delete a saved hex"));
}
```

---

### Phase 5: Make Saved Hexes Castable Like Glyphs

This is the most complex phase. Saved Hexes need to appear in the orbital ring and be usable during composition.

#### Step 5.1: Create CastableElement Interface

Create an abstraction that both Glyphs and SavedHexes can implement.

**File**: `src/main/java/com/riprod/hexcode/mode/CastableElement.java`

```java
package com.riprod.hexcode.mode;

import com.riprod.hexcode.glyph.GlyphRole;
import javax.annotation.Nonnull;

/**
 * Interface for elements that can be placed in the orbital ring
 * and used in hex composition.
 *
 * Both individual Glyphs and SavedHexes implement this interface.
 */
public interface CastableElement {

    /**
     * Get unique identifier for this element.
     */
    @Nonnull
    String getId();

    /**
     * Get display name shown in UI.
     */
    @Nonnull
    String getDisplayName();

    /**
     * Check if this is a saved hex (vs individual glyph).
     */
    boolean isSavedHex();

    /**
     * Get the role of this element for composition rules.
     * SavedHexes always return SELECT (they are complete spells).
     */
    @Nonnull
    GlyphRole getRole();

    /**
     * Get the model path for visual representation.
     */
    @Nonnull
    String getModelPath();

    /**
     * Get base mana cost.
     */
    float getBaseCost();
}
```

#### Step 5.2: Create SavedHexElement Wrapper

**File**: `src/main/java/com/riprod/hexcode/mode/SavedHexElement.java`

```java
package com.riprod.hexcode.mode;

import com.riprod.hexcode.data.SavedHexData;
import com.riprod.hexcode.glyph.GlyphRole;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.hex.HexSerializer;
import javax.annotation.Nonnull;

/**
 * Wrapper that makes a SavedHexData usable as a CastableElement.
 *
 * When placed in composition, expands to its full hex structure.
 */
public class SavedHexElement implements CastableElement {

    private final SavedHexData savedHex;
    private final Hex deserializedHex;  // Cached deserialized hex

    public SavedHexElement(SavedHexData savedHex) {
        this.savedHex = savedHex;

        // Deserialize the hex for later use
        HexSerializer serializer = new HexSerializer();
        this.deserializedHex = serializer.deserialize(savedHex.getHexString());
    }

    @Nonnull
    @Override
    public String getId() {
        return "saved:" + savedHex.getName();  // Prefix to distinguish from glyphs
    }

    @Nonnull
    @Override
    public String getDisplayName() {
        return savedHex.getName();
    }

    @Override
    public boolean isSavedHex() {
        return true;
    }

    @Nonnull
    @Override
    public GlyphRole getRole() {
        // Saved hexes act as complete spells - they can be cast directly
        // or chained with other elements
        return GlyphRole.SELECT;  // Treat as outer shell
    }

    @Nonnull
    @Override
    public String getModelPath() {
        // Use a special model for saved hexes
        // Or derive from the root glyph of the hex
        return "Hexcode/Models/SavedHex.blockymodel";
    }

    @Override
    public float getBaseCost() {
        // Calculate cost from the underlying hex
        return deserializedHex.getBaseCost();
    }

    /**
     * Get the underlying SavedHexData.
     */
    public SavedHexData getSavedHex() {
        return savedHex;
    }

    /**
     * Get the deserialized Hex structure.
     */
    public Hex getHex() {
        return deserializedHex;
    }

    /**
     * Get the serialized hex string.
     */
    public String getHexString() {
        return savedHex.getHexString();
    }
}
```

#### Step 5.3: Create OrbitalSavedHexEntity

Create a new entity type for saved hexes in the orbital ring (or modify OrbitalGlyphEntity to handle both).

**File**: `src/main/java/com/riprod/hexcode/entity/OrbitalSavedHexEntity.java`

```java
package com.riprod.hexcode.entity;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.data.SavedHexData;
import com.riprod.hexcode.mode.SavedHexElement;
import javax.annotation.Nonnull;

/**
 * Entity representing a saved hex in the orbital ring.
 *
 * Similar to OrbitalGlyphEntity but for saved hexes.
 * Uses a distinct visual (e.g., a book icon or hex symbol).
 */
public class OrbitalSavedHexEntity {

    private final SavedHexElement element;
    private final Ref<EntityStore> owner;
    private float orbitAngle;
    private final float orbitSpeed;

    private boolean isHovered;
    private Ref<EntityStore> entityRef;

    // Visual configuration
    private static final float ORBIT_RADIUS = 2.5f;
    private static final float ORBIT_SPEED = 0.3f;

    public OrbitalSavedHexEntity(@Nonnull SavedHexElement element,
                                  @Nonnull Ref<EntityStore> owner,
                                  float initialAngle) {
        this.element = element;
        this.owner = owner;
        this.orbitAngle = initialAngle;
        this.orbitSpeed = ORBIT_SPEED;
        this.isHovered = false;
    }

    public void spawn(CommandBuffer<EntityStore> commandBuffer, Vector3d playerPosition) {
        // Similar to OrbitalGlyphEntity.spawn()
        // Create entity with SavedHex model
        // See OrbitalGlyphEntity.java for reference implementation
    }

    public void despawn(CommandBuffer<EntityStore> commandBuffer) {
        if (entityRef != null) {
            commandBuffer.deleteEntity(entityRef);
            entityRef = null;
        }
    }

    public void update(float dt) {
        orbitAngle += orbitSpeed * dt;
        if (orbitAngle > Math.PI * 2) {
            orbitAngle -= Math.PI * 2;
        }
    }

    public SavedHexElement getElement() {
        return element;
    }

    public boolean isHovered() {
        return isHovered;
    }

    public void setHovered(boolean hovered) {
        this.isHovered = hovered;
    }

    // Add other methods similar to OrbitalGlyphEntity
}
```

#### Step 5.4: Update CompositionState for Saved Hexes

When a saved hex is placed in composition, it should expand to its full hex tree.

**File**: `src/main/java/com/riprod/hexcode/mode/CompositionState.java`

```java
// ADD method to handle saved hex placement:

/**
 * Add a saved hex to the composition.
 * The saved hex expands to its full tree structure.
 *
 * @param element The saved hex element to add
 * @return true if added successfully
 */
public boolean addSavedHex(SavedHexElement element) {
    Hex expandedHex = element.getHex();

    if (expandedHex == null || !expandedHex.hasRoot()) {
        return false;
    }

    // The saved hex's root becomes a new tree in the composition
    // Treat it like adding the root glyph with its full subtree

    if (currentHex == null || currentHex.getRoot() == null) {
        // First element - use the saved hex directly
        currentHex = expandedHex;
        pushAction(CompositionAction.PLACE_ROOT, expandedHex.getRoot().getGlyph());
        return true;
    }

    // Otherwise, treat the saved hex as a sibling (chain)
    // This depends on your composition rules
    // For MVP, might just add as parallel chain element

    return false;  // Implement based on composition rules
}
```

---

### Phase 6: Migration and Integration

#### Step 6.1: Integrate with Drawing System

When a player draws a glyph, it should be saved to the book. Find where drawings are recorded (likely in event handlers or glyph mode).

**Location to modify**: Look for where `PlayerGlyphDataManager.getOrCreatePlayerData()` is called and glyph drawings are recorded.

Replace calls like:
```java
// OLD
PlayerGlyphData playerData = PlayerGlyphDataManager.getOrCreatePlayerData(playerId);
playerData.updateGlyphDrawing(glyphId, accuracy, drawSpeed);
PlayerGlyphDataManager.savePlayerDataAsync(playerId);
```

With:
```java
// NEW
HexBookDataManager.recordGlyphDrawing(store, playerRef, glyphId, accuracy, drawSpeed);
```

#### Step 6.2: Update Casting to Use Book Accuracy

In `HexExecutor` or wherever spell power is calculated based on accuracy, get accuracy from the book:

```java
// OLD
PlayerGlyphData playerData = PlayerGlyphDataManager.getOrCreatePlayerData(casterId);
float accuracy = playerData.getAccuracy(glyphId);

// NEW
GlyphMode mode = GlyphModeManager.getInstance().getSession(casterId);
float accuracy = mode != null ? mode.getGlyphAccuracy(glyphId) : 0.75f;
```

Or access the book data directly if not in glyph mode:
```java
HexBookData bookData = HexBookDataManager.getHeldBookData(store, casterRef);
float accuracy = bookData != null ? bookData.getAccuracy(glyphId) : 0.75f;
```

---

## File Summary

### New Files to Create

| File | Purpose |
|------|---------|
| `data/BookGlyphInstanceData.java` | Per-glyph data with BuilderCodec |
| `data/SavedHexData.java` | Saved hex configuration with BuilderCodec |
| `data/HexBookData.java` | Main container for book metadata |
| `data/HexBookDataManager.java` | Utility for reading/writing book data |
| `mode/CastableElement.java` | Interface for glyphs and saved hexes |
| `mode/SavedHexElement.java` | Wrapper for saved hexes |
| `entity/OrbitalSavedHexEntity.java` | Orbital entity for saved hexes |

### Files to Modify

| File | Changes |
|------|---------|
| `mode/GlyphMode.java` | Add bookData field, derive glyphs from book |
| `mode/GlyphModeManager.java` | Pass book data when creating GlyphMode |
| `mode/CompositionState.java` | Handle saved hex placement |
| `command/HexcodeCommand.java` | Add save/hexes/deletehex commands |
| `execution/HexExecutor.java` | Get accuracy from book data |
| Event handlers (TBD) | Record drawings to book |

### Files That Can Be Deprecated

| File | Status |
|------|--------|
| `data/PlayerGlyphData.java` | Keep for now, phase out gradually |
| `data/PlayerGlyphDataManager.java` | Keep for backwards compatibility |
| `loadout/LoadoutManager.java` | Keep for backwards compatibility |

---

## Utility Code Reference

### Getting Player's Held Hex Book

```java
import com.riprod.hexcode.data.HexBookDataManager;

// Get the ItemStack
ItemStack hexBook = HexBookDataManager.findHeldHexBook(store, playerRef);
if (hexBook == null) {
    // Player doesn't have Hex Book equipped
    return;
}

// Get the data
HexBookData data = HexBookDataManager.getData(hexBook);
```

### Updating Book Data

```java
// Read
HexBookData data = HexBookDataManager.getHeldBookData(store, playerRef);

// Modify
data.recordGlyphDrawing("hexcode:fire", 0.95f, 2.3f);

// Write back
HexBookDataManager.updateHeldBookData(store, playerRef, data);
```

### One-liner for Recording Glyph Drawing

```java
HexBookDataManager.recordGlyphDrawing(store, playerRef, "hexcode:fire", 0.95f, 2.3f);
```

### Checking for Hex Book in Offhand

```java
import com.riprod.hexcode.util.HexStaffUtil;

// Existing utility - check if player has valid equipment
if (!HexStaffUtil.hasHexStaffInHand(store, playerRef)) {
    return;
}

// Check for book specifically
if (HexBookDataManager.findHeldHexBook(store, playerRef) == null) {
    return;
}
```

---

## Testing Checklist

1. **Basic Data Storage**
   - [ ] Create new Hex Book item
   - [ ] Draw a glyph → verify it's stored in book metadata
   - [ ] Check another Hex Book → verify data is separate

2. **Glyph Mode Integration**
   - [ ] Enter Glyph Mode → orbital glyphs come from book
   - [ ] Empty book → no orbital glyphs (or starter glyphs?)
   - [ ] Book with glyphs → those glyphs appear in orbital ring

3. **Save Command**
   - [ ] `/hexcode save TestHex` → saves current composition
   - [ ] `/hexcode hexes` → lists saved hexes
   - [ ] `/hexcode deletehex TestHex` → removes saved hex

4. **Saved Hex Casting**
   - [ ] Saved hex appears in orbital ring
   - [ ] Can drag saved hex to composition
   - [ ] Saved hex expands to full tree when placed
   - [ ] Can cast saved hex directly

5. **Data Persistence**
   - [ ] Quit and rejoin → book data persists
   - [ ] Drop and pick up book → data persists
   - [ ] Trade book to another player → data stays with book

---

## Important Notes

1. **ItemStack Immutability**: `withMetadata()` returns a NEW ItemStack. Always replace in inventory.

2. **Codec Compatibility**: Ensure `MapCodec` and `ListCodec` are available. If not, may need to serialize to BsonDocument manually.

3. **Migration Strategy**: The old per-player system can coexist during transition. Eventually deprecate `PlayerGlyphData*` classes.

4. **Starter Glyphs**: Decide what happens with a fresh Hex Book - empty? starter glyphs? This affects new player experience.

5. **Multiple Books**: Players can have multiple books with different configurations. The "active" book is always the one in offhand.
