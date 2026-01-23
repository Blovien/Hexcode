package com.riprod.hexcode.data;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.util.HexStaffUtil;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utility class for managing HexBookData on Hex Book items.
 *
 * <p>This manager uses a UUID-based reference system where:
 * <ul>
 *   <li>Each Hex Book stores only a UUID in its item metadata</li>
 *   <li>The actual glyph/hex data is stored in {@link HexBookDataStore}</li>
 *   <li>Data is persisted to a JSON file via HexBookDataStore</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * // Read from player's held book
 * HexBookData data = HexBookDataManager.getHeldBookData(store, playerRef);
 *
 * // Modify
 * data.recordGlyphDrawing("hexcode:fire", 0.95f, 2.3f);
 *
 * // Write back (auto-saves to JSON file)
 * HexBookDataManager.updateHeldBookData(store, playerRef, data);
 * </pre>
 *
 * @see HexBookData
 * @see HexBookDataStore
 * @see HexStaffUtil
 */
public class HexBookDataManager {

    /** Metadata key for storing the book's UUID on the ItemStack */
    public static final String UUID_KEY = "HexBookUUID";

    private HexBookDataManager() {} // Utility class

    // ==================== ITEM DETECTION ====================

    /**
     * Check if an ItemStack is a Hex Book.
     */
    public static boolean isHexBook(@Nullable ItemStack itemStack) {
        return HexStaffUtil.isHexBook(itemStack);
    }

    /**
     * Find the Hex Book in player's offhand.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @return The Hex Book ItemStack, or null if not found
     */
    @Nullable
    public static ItemStack findHeldHexBook(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        if (store == null || playerRef == null) {
            return null;
        }

        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            return null;
        }

        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return null;
        }

        ItemStack book = HexStaffUtil.getHexBookFromOffhand(inventory);
        if (book != null) {
            return book;
        } else {
            return HexStaffUtil.getHexBookFromMainHand(inventory);
        }
    }

    /**
     * Get the player's inventory.
     */
    @Nullable
    private static Inventory getInventory(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        if (store == null || playerRef == null) {
            return null;
        }

        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            return null;
        }

        return player.getInventory();
    }

    // ==================== UUID MANAGEMENT ====================

    /**
     * Get the UUID stored on a Hex Book.
     *
     * @param hexBook The Hex Book ItemStack
     * @return The book's UUID, or null if none is stored
     */
    @Nullable
    public static String getBookUUID(@Nonnull ItemStack hexBook) {
        BsonDocument metadata = hexBook.getMetadata();
        if (metadata == null) {
            return null;
        }

        BsonValue uuidValue = metadata.get(UUID_KEY);
        if (uuidValue == null || !uuidValue.isString()) {
            return null;
        }

        return uuidValue.asString().getValue();
    }

    /**
     * Create a new ItemStack with a UUID set.
     *
     * @param hexBook The original Hex Book
     * @param uuid The UUID to set
     * @return New ItemStack with UUID metadata
     */
    @Nonnull
    public static ItemStack withUUID(@Nonnull ItemStack hexBook, @Nonnull String uuid) {
        return hexBook.withMetadata(UUID_KEY, new BsonString(uuid));
    }

    /**
     * Get or create a UUID for a Hex Book.
     * If the book doesn't have a UUID, one will be generated.
     *
     * @param hexBook The Hex Book ItemStack
     * @return The existing or new UUID
     */
    @Nonnull
    public static String getOrCreateUUID(@Nonnull ItemStack hexBook) {
        String existing = getBookUUID(hexBook);
        if (existing != null) {
            return existing;
        }
        return HexBookDataStore.generateBookUUID();
    }

    // ==================== DATA ACCESS ====================

    /**
     * Read HexBookData from an ItemStack via UUID lookup.
     * Returns new empty HexBookData if the book has no data.
     *
     * @param hexBook The Hex Book ItemStack
     * @return The HexBookData (existing or new empty)
     */
    @Nonnull
    public static HexBookData getData(@Nonnull ItemStack hexBook) {
        String uuid = getBookUUID(hexBook);
        if (uuid == null) {
            return new HexBookData();
        }

        return HexBookDataStore.get().getOrCreateBookData(uuid);
    }

    /**
     * Read HexBookData from player's held Hex Book.
     * Returns null if player doesn't have a Hex Book equipped.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @return The HexBookData, or null if no book equipped
     */
    @Nullable
    public static HexBookData getHeldBookData(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        ItemStack hexBook = findHeldHexBook(store, playerRef);
        if (hexBook == null) {
            return null;
        }
        return getData(hexBook);
    }

    /**
     * Get the UUID of the player's held Hex Book.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @return The book's UUID, or null if no book equipped
     */
    @Nullable
    public static String getHeldBookUUID(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        ItemStack hexBook = findHeldHexBook(store, playerRef);
        if (hexBook == null) {
            return null;
        }
        return getBookUUID(hexBook);
    }

    // ==================== DATA MODIFICATION ====================

    /**
     * Update the Hex Book in player's inventory with new data.
     * The data is stored in HexBookDataStore and persisted to JSON.
     * If the book doesn't have a UUID, one will be assigned.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param data The new HexBookData to save
     * @return true if updated successfully, false if no Hex Book found
     */
    public static boolean updateHeldBookData(Store<EntityStore> store,
                                              Ref<EntityStore> playerRef,
                                              @Nonnull HexBookData data) {
        Inventory inventory = getInventory(store, playerRef);
        if (inventory == null) {
            return false;
        }

        ItemStack oldStack = inventory.getUtilityItem();
        if (!isHexBook(oldStack)) {
            return false;
        }

        // Get or create UUID for this book
        String uuid = getBookUUID(oldStack);
        boolean needsUUIDUpdate = (uuid == null);

        if (needsUUIDUpdate) {
            uuid = HexBookDataStore.generateBookUUID();
        }

        // Store data in the data store
        HexBookDataStore.get().setBookData(uuid, data);

        // Save to JSON file
        HexBookDataStore.save();

        // If we created a new UUID, update the item metadata
        if (needsUUIDUpdate) {
            ItemStack newStack = withUUID(oldStack, uuid);
            inventory.getUtility().replaceItemStackInSlot(inventory.getActiveUtilitySlot(), oldStack, newStack);
        }

        return true;
    }

    /**
     * Assign a UUID to a Hex Book that doesn't have one yet.
     * This is useful for initializing new books.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @return The assigned UUID, or null if no Hex Book found
     */
    @Nullable
    public static String initializeHeldBookUUID(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        Inventory inventory = getInventory(store, playerRef);
        if (inventory == null) {
            return null;
        }

        ItemStack oldStack = inventory.getUtilityItem();
        if (!isHexBook(oldStack)) {
            return null;
        }

        // Check if already has UUID
        String existingUUID = getBookUUID(oldStack);
        if (existingUUID != null) {
            return existingUUID;
        }

        // Generate new UUID
        String uuid = HexBookDataStore.generateBookUUID();

        // Create empty data entry in store
        HexBookDataStore.get().getOrCreateBookData(uuid);

        // Update item with UUID
        ItemStack newStack = withUUID(oldStack, uuid);
        inventory.getUtility().replaceItemStackInSlot(inventory.getActiveUtilitySlot(), oldStack, newStack);

        // Save to file
        HexBookDataStore.save();

        return uuid;
    }

    // ==================== CONVENIENCE METHODS ====================

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
     * Add a glyph to the player's held Hex Book with default values.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param glyphId The glyph to add
     * @return true if added successfully
     */
    public static boolean addGlyph(Store<EntityStore> store,
                                    Ref<EntityStore> playerRef,
                                    @Nonnull String glyphId) {
        HexBookData data = getHeldBookData(store, playerRef);
        if (data == null) {
            return false;
        }

        data.addGlyphWithDefault(glyphId);
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
                                   @Nonnull Hex hex) {
        HexBookData data = getHeldBookData(store, playerRef);
        if (data == null) {
            return false;
        }

        if (!data.saveHex(hex, hex.getId())) {
            return false;  // At max capacity
        }

        return updateHeldBookData(store, playerRef, data);
    }

    /**
     * Delete a saved hex from the player's held Hex Book.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param name Name of the hex to delete
     * @return true if deleted successfully
     */
    public static boolean deleteSavedHex(Store<EntityStore> store,
                                          Ref<EntityStore> playerRef,
                                          @Nonnull String name) {
        HexBookData data = getHeldBookData(store, playerRef);
        if (data == null) {
            return false;
        }

        if (!data.deleteSavedHex(name)) {
            return false;  // Not found
        }

        return updateHeldBookData(store, playerRef, data);
    }

    /**
     * Record that a glyph was used (increment usage count).
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param glyphId The glyph that was used
     * @return true if recorded successfully
     */
    public static boolean recordGlyphUsage(Store<EntityStore> store,
                                            Ref<EntityStore> playerRef,
                                            @Nonnull String glyphId) {
        HexBookData data = getHeldBookData(store, playerRef);
        if (data == null) {
            return false;
        }

        data.recordGlyphUsage(glyphId);
        return updateHeldBookData(store, playerRef, data);
    }

    /**
     * Record that a saved hex was used.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param hexName Name of the hex that was used
     * @return true if recorded successfully
     */
    public static boolean recordSavedHexUsage(Store<EntityStore> store,
                                               Ref<EntityStore> playerRef,
                                               @Nonnull String hexName) {
        HexBookData data = getHeldBookData(store, playerRef);
        if (data == null) {
            return false;
        }

        data.recordSavedHexUsage(hexName);
        return updateHeldBookData(store, playerRef, data);
    }
}
