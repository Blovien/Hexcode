package com.riprod.hexcode.data;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.util.HexBookItemData;
import com.riprod.hexcode.util.HexStaffUtil;
import com.riprod.hexcode.util.InventoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Utility class for managing HexBookData on Hex Book items.
 *
 * <p><b>NEW ARCHITECTURE:</b> Data is now stored directly in ItemStack metadata,
 * making each book self-contained and tradeable with its complete spell data.
 *
 * <p><b>IMPORTANT - Immutable ItemStack Pattern:</b> All write operations return
 * a NEW ItemStack. This class handles the inventory update automatically via
 * {@link InventoryUtil}.
 *
 * <h2>Storage Model</h2>
 * <ul>
 *   <li><b>Current</b>: Data embedded in ItemStack metadata via {@link HexBookItemData}</li>
 *   <li><b>Legacy</b>: File-based per-player storage via {@link WorldBookDataStore}</li>
 * </ul>
 *
 * <h2>Migration</h2>
 * <p>Books with old UUID-only metadata are automatically migrated on first access.
 * The migration loads existing data from file storage and embeds it in the ItemStack.
 *
 * <h2>Usage</h2>
 * <pre>
 * // Read data from held book
 * HexBookData data = HexBookDataManager.getHeldBookData(store, playerRef);
 *
 * // Record a glyph drawing (auto-updates inventory)
 * boolean success = HexBookDataManager.recordGlyphDrawing(
 *     store, playerRef, "hexcode:fire", 0.95f, 2.3f);
 *
 * // Save a hex (auto-updates inventory)
 * HexBookDataManager.saveHex(store, playerRef, hex);
 * </pre>
 *
 * @see HexBookData
 * @see HexBookItemData
 * @see WorldBookDataStore (legacy)
 */
public class HexBookDataManager {

    private HexBookDataManager() {} // Utility class

    // ==================== ITEM DETECTION ====================

    /**
     * Check if an ItemStack is a Hex Book.
     */
    public static boolean isHexBook(@Nullable ItemStack itemStack) {
        return HexStaffUtil.isHexBook(itemStack);
    }

    /**
     * Find the Hex Book in player's offhand or main hand.
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

    // ==================== PRIVATE HELPERS ====================

    /**
     * Get the player's UUID from the entity store.
     */
    @Nullable
    private static UUID getPlayerUUID(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        if (store == null || playerRef == null) {
            return null;
        }

        UUIDComponent uuidComponent = store.getComponent(playerRef, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return null;
        }

        return uuidComponent.getUuid();
    }

    /**
     * Get the player's inventory.
     */
    @Nullable
    private static Inventory getPlayerInventory(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        if (store == null || playerRef == null) {
            return null;
        }

        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            return null;
        }

        return player.getInventory();
    }

    /**
     * Determine the BookType from an ItemStack.
     * Currently defaults to HEX_BOOK but will support different book types in the future.
     */
    @Nonnull
    private static BookType getBookType(@Nonnull ItemStack hexBook) {
        String book_id = hexBook.getItem().getId();
        if (book_id != null && book_id.toLowerCase().contains("hex") && book_id.toLowerCase().contains("book")) {
            return BookType.fromFileId(book_id);
        }
        // Default to standard hex book
        return BookType.HEX_BOOK;
    }

    /**
     * Check if the book is in offhand (utility slot).
     */
    private static boolean isInOffhand(Inventory inventory, ItemStack book) {
        if (inventory == null || book == null) {
            return false;
        }
        ItemStack utilityItem = inventory.getUtilityItem();
        return utilityItem != null && utilityItem.equals(book);
    }

    /**
     * Update the book in the appropriate inventory slot.
     */
    private static void updateBookInInventory(Inventory inventory, ItemStack originalBook, ItemStack newBook) {
        if (inventory == null || originalBook == null || newBook == null) {
            return;
        }

        // Check if book is in offhand (utility slot)
        ItemStack utilityItem = inventory.getUtilityItem();
        if (utilityItem != null && HexStaffUtil.isHexBook(utilityItem)) {
            InventoryUtil.updateOffhandItem(inventory, newBook);
            return;
        }

        // Check if book is in main hand
        ItemStack mainHandItem = inventory.getItemInHand();
        if (mainHandItem != null && HexStaffUtil.isHexBook(mainHandItem)) {
            InventoryUtil.updateMainHandItem(inventory, newBook);
        }
    }

    // ==================== DATA ACCESS ====================

    /**
     * Read HexBookData from player's held Hex Book.
     *
     * <p>Data is read directly from ItemStack metadata. If the book has old-style
     * UUID-only metadata (from legacy system), this performs automatic migration.
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

        UUID playerUuid = getPlayerUUID(store, playerRef);
        if (playerUuid == null) {
            return null;
        }

        // Try to get data from item metadata (new system)
        HexBookData data = HexBookItemData.getData(hexBook);
        if (data != null) {
            return data;
        }

        // Check for legacy migration
        Inventory inventory = getPlayerInventory(store, playerRef);
        World world = store.getExternalData().getWorld();

        if (world != null && inventory != null) {
            // Try to migrate from old file-based storage
            BookType bookType = getBookType(hexBook);
            HexBookData legacyData = WorldBookDataStore.get().getBookData(world, playerUuid, bookType);
            if (legacyData != null && legacyData.getGlyphCount() > 0) {
                // Migrate legacy data to item metadata
                ItemStack migratedBook = HexBookItemData.migrateIfNeeded(hexBook, legacyData, playerUuid);
                updateBookInInventory(inventory, hexBook, migratedBook);
                return legacyData;
            }
        }

        // Create new empty data
        return HexBookItemData.getOrCreateData(hexBook, playerUuid);
    }

    /**
     * Read HexBookData for a player in a specific world.
     *
     * <p><b>Legacy method</b> - prefer using {@link #getHeldBookData(Store, Ref)}
     * which reads from ItemStack metadata.
     *
     * @param world The world
     * @param playerUuid The player's UUID
     * @param bookType The book type
     * @return The HexBookData (existing or new empty)
     */
    @Nonnull
    public static HexBookData getData(@Nonnull World world, @Nonnull UUID playerUuid, @Nonnull BookType bookType) {
        return WorldBookDataStore.get().getBookData(world, playerUuid, bookType);
    }

    // ==================== DATA MODIFICATION ====================

    /**
     * Update the Hex Book data for a player's held book.
     *
     * <p>Writes data to ItemStack metadata and updates the inventory slot.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param data The HexBookData to save
     * @return true if updated successfully, false if no book found
     */
    public static boolean updateHeldBookData(Store<EntityStore> store,
                                              Ref<EntityStore> playerRef,
                                              @Nonnull HexBookData data) {
        ItemStack hexBook = findHeldHexBook(store, playerRef);
        if (hexBook == null) {
            return false;
        }

        Inventory inventory = getPlayerInventory(store, playerRef);
        if (inventory == null) {
            return false;
        }

        // Write to item metadata (returns new ItemStack)
        ItemStack updatedBook = HexBookItemData.withData(hexBook, data);
        updateBookInInventory(inventory, hexBook, updatedBook);
        return true;
    }

    // ==================== CONVENIENCE METHODS ====================

    /**
     * Record a glyph drawing in the player's held Hex Book.
     *
     * <p>Automatically updates the inventory with the modified book.
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
        ItemStack hexBook = findHeldHexBook(store, playerRef);
        if (hexBook == null) {
            return false;
        }

        UUID playerUuid = getPlayerUUID(store, playerRef);
        if (playerUuid == null) {
            return false;
        }

        Inventory inventory = getPlayerInventory(store, playerRef);
        if (inventory == null) {
            return false;
        }

        // Use HexBookItemData convenience method
        HexBookItemData.LearnResult result = HexBookItemData.learnGlyph(
            hexBook, playerUuid, glyphId, accuracy, drawSpeed);

        if (result.success()) {
            updateBookInInventory(inventory, hexBook, result.book());
            return true;
        }

        return false;
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
        ItemStack hexBook = findHeldHexBook(store, playerRef);
        if (hexBook == null) {
            return false;
        }

        UUID playerUuid = getPlayerUUID(store, playerRef);
        if (playerUuid == null) {
            return false;
        }

        Inventory inventory = getPlayerInventory(store, playerRef);
        if (inventory == null) {
            return false;
        }

        HexBookItemData.LearnResult result = HexBookItemData.addGlyph(hexBook, playerUuid, glyphId);

        if (result.success()) {
            updateBookInInventory(inventory, hexBook, result.book());
            return true;
        }

        return false;
    }

    /**
     * Save a hex to the player's held Hex Book.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param hex The hex to save
     * @return true if saved successfully, false if no book or at capacity
     */
    public static boolean saveHex(Store<EntityStore> store,
                                   Ref<EntityStore> playerRef,
                                   @Nonnull Hex hex) {
        ItemStack hexBook = findHeldHexBook(store, playerRef);
        if (hexBook == null) {
            return false;
        }

        UUID playerUuid = getPlayerUUID(store, playerRef);
        if (playerUuid == null) {
            return false;
        }

        Inventory inventory = getPlayerInventory(store, playerRef);
        if (inventory == null) {
            return false;
        }

        HexBookItemData.SaveResult result = HexBookItemData.saveHex(hexBook, playerUuid, hex);

        if (result.success()) {
            updateBookInInventory(inventory, hexBook, result.book());
            return true;
        }

        return false;
    }

    /**
     * Delete a saved hex from the player's held Hex Book.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param hexId ID of the hex to delete
     * @return true if deleted successfully
     */
    public static boolean deleteSavedHex(Store<EntityStore> store,
                                          Ref<EntityStore> playerRef,
                                          @Nonnull String hexId) {
        ItemStack hexBook = findHeldHexBook(store, playerRef);
        if (hexBook == null) {
            return false;
        }

        Inventory inventory = getPlayerInventory(store, playerRef);
        if (inventory == null) {
            return false;
        }

        HexBookItemData.DeleteResult result = HexBookItemData.deleteSavedHex(hexBook, hexId);

        if (result.success()) {
            updateBookInInventory(inventory, hexBook, result.book());
            return true;
        }

        return false;
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
        ItemStack hexBook = findHeldHexBook(store, playerRef);
        if (hexBook == null) {
            return false;
        }

        Inventory inventory = getPlayerInventory(store, playerRef);
        if (inventory == null) {
            return false;
        }

        ItemStack updatedBook = HexBookItemData.recordGlyphUsage(hexBook, glyphId);
        if (updatedBook != hexBook) {
            updateBookInInventory(inventory, hexBook, updatedBook);
            return true;
        }

        return false;
    }

    /**
     * Record that a saved hex was used.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param hexId ID of the hex that was used
     * @return true if recorded successfully
     */
    public static boolean recordSavedHexUsage(Store<EntityStore> store,
                                               Ref<EntityStore> playerRef,
                                               @Nonnull String hexId) {
        ItemStack hexBook = findHeldHexBook(store, playerRef);
        if (hexBook == null) {
            return false;
        }

        Inventory inventory = getPlayerInventory(store, playerRef);
        if (inventory == null) {
            return false;
        }

        ItemStack updatedBook = HexBookItemData.recordSavedHexUsage(hexBook, hexId);
        if (updatedBook != hexBook) {
            updateBookInInventory(inventory, hexBook, updatedBook);
            return true;
        }

        return false;
    }

    // ==================== LEGACY METHODS (for backwards compatibility) ====================

    /**
     * Read HexBookData from player's held Hex Book with world context.
     *
     * @deprecated Use {@link #getHeldBookData(Store, Ref)} instead.
     *             World context is no longer needed for item-based storage.
     */
    @Deprecated
    @Nullable
    public static HexBookData getHeldBookData(Store<EntityStore> store, Ref<EntityStore> playerRef, @Nonnull World world) {
        return getHeldBookData(store, playerRef);
    }

    /**
     * Update the Hex Book data with world context.
     *
     * @deprecated Use {@link #updateHeldBookData(Store, Ref, HexBookData)} instead.
     *             World context is no longer needed for item-based storage.
     */
    @Deprecated
    public static boolean updateHeldBookData(Store<EntityStore> store,
                                              Ref<EntityStore> playerRef,
                                              @Nonnull World world,
                                              @Nonnull HexBookData data) {
        return updateHeldBookData(store, playerRef, data);
    }

    /**
     * Record a glyph drawing with world context.
     *
     * @deprecated Use the version without world parameter.
     */
    @Deprecated
    public static boolean recordGlyphDrawing(Store<EntityStore> store,
                                              Ref<EntityStore> playerRef,
                                              @Nonnull World world,
                                              @Nonnull String glyphId,
                                              float accuracy,
                                              float drawSpeed) {
        return recordGlyphDrawing(store, playerRef, glyphId, accuracy, drawSpeed);
    }

    /**
     * Add a glyph with world context.
     *
     * @deprecated Use the version without world parameter.
     */
    @Deprecated
    public static boolean addGlyph(Store<EntityStore> store,
                                    Ref<EntityStore> playerRef,
                                    @Nonnull World world,
                                    @Nonnull String glyphId) {
        return addGlyph(store, playerRef, glyphId);
    }

    /**
     * Save a hex with world context.
     *
     * @deprecated Use the version without world parameter.
     */
    @Deprecated
    public static boolean saveHex(Store<EntityStore> store,
                                   Ref<EntityStore> playerRef,
                                   @Nonnull World world,
                                   @Nonnull Hex hex) {
        return saveHex(store, playerRef, hex);
    }

    /**
     * Delete a saved hex with world context.
     *
     * @deprecated Use the version without world parameter.
     */
    @Deprecated
    public static boolean deleteSavedHex(Store<EntityStore> store,
                                          Ref<EntityStore> playerRef,
                                          @Nonnull World world,
                                          @Nonnull String name) {
        return deleteSavedHex(store, playerRef, name);
    }

    /**
     * Record glyph usage with world context.
     *
     * @deprecated Use the version without world parameter.
     */
    @Deprecated
    public static boolean recordGlyphUsage(Store<EntityStore> store,
                                            Ref<EntityStore> playerRef,
                                            @Nonnull World world,
                                            @Nonnull String glyphId) {
        return recordGlyphUsage(store, playerRef, glyphId);
    }

    /**
     * Record saved hex usage with world context.
     *
     * @deprecated Use the version without world parameter.
     */
    @Deprecated
    public static boolean recordSavedHexUsage(Store<EntityStore> store,
                                               Ref<EntityStore> playerRef,
                                               @Nonnull World world,
                                               @Nonnull String hexName) {
        return recordSavedHexUsage(store, playerRef, hexName);
    }
}
