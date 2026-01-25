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
import com.riprod.hexcode.util.HexStaffUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Utility class for managing HexBookData on Hex Book items.
 *
 * <p>This manager uses a per-world, per-player storage system where:
 * <ul>
 *   <li>Data is stored per-world in the world's save directory</li>
 *   <li>Each player has their own data directory</li>
 *   <li>Different book types have separate data files</li>
 *   <li>Data is persisted via {@link WorldBookDataStore}</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * // Read from player's held book with world context
 * World world = store.getExternalData().getWorld();
 * HexBookData data = HexBookDataManager.getHeldBookData(store, playerRef, world);
 *
 * // Modify
 * data.recordGlyphDrawing("hexcode:fire", 0.95f, 2.3f);
 *
 * // Write back (auto-saves to world-specific file)
 * HexBookDataManager.updateHeldBookData(store, playerRef, world, data);
 * </pre>
 *
 * @see HexBookData
 * @see WorldBookDataStore
 * @see BookType
 * @see HexStaffUtil
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

    // ==================== DATA ACCESS ====================

    /**
     * Read HexBookData for a player in a specific world.
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

    /**
     * Read HexBookData from player's held Hex Book with world context.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param world The world context
     * @return The HexBookData, or null if no book equipped
     */
    @Nullable
    public static HexBookData getHeldBookData(Store<EntityStore> store, Ref<EntityStore> playerRef, @Nonnull World world) {
        ItemStack hexBook = findHeldHexBook(store, playerRef);
        if (hexBook == null) {
            return null;
        }

        UUID playerUuid = getPlayerUUID(store, playerRef);
        if (playerUuid == null) {
            return null;
        }

        BookType bookType = getBookType(hexBook);
        return WorldBookDataStore.get().getBookData(world, playerUuid, bookType);
    }

    /**
     * Read HexBookData from player's held Hex Book with world context and explicit book type.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param world The world context
     * @param bookType The book type to use
     * @return The HexBookData, or null if no book equipped
     */
    @Nullable
    public static HexBookData getHeldBookData(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                               @Nonnull World world, @Nonnull BookType bookType) {
        ItemStack hexBook = findHeldHexBook(store, playerRef);
        if (hexBook == null) {
            return null;
        }

        UUID playerUuid = getPlayerUUID(store, playerRef);
        if (playerUuid == null) {
            return null;
        }

        return WorldBookDataStore.get().getBookData(world, playerUuid, bookType);
    }

    // ==================== DATA MODIFICATION ====================

    /**
     * Update the Hex Book data for a player in a specific world.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param world The world context
     * @param data The new HexBookData to save
     * @return true if updated successfully, false if no Hex Book found or player UUID unavailable
     */
    public static boolean updateHeldBookData(Store<EntityStore> store,
                                              Ref<EntityStore> playerRef,
                                              @Nonnull World world,
                                              @Nonnull HexBookData data) {
        ItemStack hexBook = findHeldHexBook(store, playerRef);
        if (hexBook == null) {
            return false;
        }

        UUID playerUuid = getPlayerUUID(store, playerRef);
        if (playerUuid == null) {
            return false;
        }

        BookType bookType = getBookType(hexBook);
        WorldBookDataStore.get().saveBookData(world, playerUuid, bookType, data);
        return true;
    }

    /**
     * Update the Hex Book data for a player in a specific world with explicit book type.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param world The world context
     * @param bookType The book type
     * @param data The new HexBookData to save
     * @return true if updated successfully, false if no player UUID available
     */
    public static boolean updateHeldBookData(Store<EntityStore> store,
                                              Ref<EntityStore> playerRef,
                                              @Nonnull World world,
                                              @Nonnull BookType bookType,
                                              @Nonnull HexBookData data) {
        UUID playerUuid = getPlayerUUID(store, playerRef);
        if (playerUuid == null) {
            return false;
        }

        WorldBookDataStore.get().saveBookData(world, playerUuid, bookType, data);
        return true;
    }

    // ==================== CONVENIENCE METHODS ====================

    /**
     * Record a glyph drawing in the player's held Hex Book.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param world The world context
     * @param glyphId The glyph that was drawn
     * @param accuracy Drawing accuracy 0.0-1.0
     * @param drawSpeed Time in seconds to draw
     * @return true if recorded successfully
     */
    public static boolean recordGlyphDrawing(Store<EntityStore> store,
                                              Ref<EntityStore> playerRef,
                                              @Nonnull World world,
                                              @Nonnull String glyphId,
                                              float accuracy,
                                              float drawSpeed) {
        HexBookData data = getHeldBookData(store, playerRef, world);
        if (data == null) {
            return false;
        }

        data.recordGlyphDrawing(glyphId, accuracy, drawSpeed);
        return updateHeldBookData(store, playerRef, world, data);
    }

    /**
     * Add a glyph to the player's held Hex Book with default values.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param world The world context
     * @param glyphId The glyph to add
     * @return true if added successfully
     */
    public static boolean addGlyph(Store<EntityStore> store,
                                    Ref<EntityStore> playerRef,
                                    @Nonnull World world,
                                    @Nonnull String glyphId) {
        HexBookData data = getHeldBookData(store, playerRef, world);
        if (data == null) {
            return false;
        }

        data.addGlyphWithDefault(glyphId);
        return updateHeldBookData(store, playerRef, world, data);
    }

    /**
     * Save a hex to the player's held Hex Book.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param world The world context
     * @param hex The hex to save
     * @return true if saved successfully, false if no book or at capacity
     */
    public static boolean saveHex(Store<EntityStore> store,
                                   Ref<EntityStore> playerRef,
                                   @Nonnull World world,
                                   @Nonnull Hex hex) {
        HexBookData data = getHeldBookData(store, playerRef, world);
        if (data == null) {
            return false;
        }

        if (!data.saveHex(hex, hex.getId())) {
            return false;  // At max capacity
        }

        return updateHeldBookData(store, playerRef, world, data);
    }

    /**
     * Delete a saved hex from the player's held Hex Book.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param world The world context
     * @param name Name of the hex to delete
     * @return true if deleted successfully
     */
    public static boolean deleteSavedHex(Store<EntityStore> store,
                                          Ref<EntityStore> playerRef,
                                          @Nonnull World world,
                                          @Nonnull String name) {
        HexBookData data = getHeldBookData(store, playerRef, world);
        if (data == null) {
            return false;
        }

        if (!data.deleteSavedHex(name)) {
            return false;  // Not found
        }

        return updateHeldBookData(store, playerRef, world, data);
    }

    /**
     * Record that a glyph was used (increment usage count).
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param world The world context
     * @param glyphId The glyph that was used
     * @return true if recorded successfully
     */
    public static boolean recordGlyphUsage(Store<EntityStore> store,
                                            Ref<EntityStore> playerRef,
                                            @Nonnull World world,
                                            @Nonnull String glyphId) {
        HexBookData data = getHeldBookData(store, playerRef, world);
        if (data == null) {
            return false;
        }

        data.recordGlyphUsage(glyphId);
        return updateHeldBookData(store, playerRef, world, data);
    }

    /**
     * Record that a saved hex was used.
     *
     * @param store Entity store
     * @param playerRef Player entity reference
     * @param world The world context
     * @param hexName Name of the hex that was used
     * @return true if recorded successfully
     */
    public static boolean recordSavedHexUsage(Store<EntityStore> store,
                                               Ref<EntityStore> playerRef,
                                               @Nonnull World world,
                                               @Nonnull String hexName) {
        HexBookData data = getHeldBookData(store, playerRef, world);
        if (data == null) {
            return false;
        }

        data.recordSavedHexUsage(hexName);
        return updateHeldBookData(store, playerRef, world, data);
    }
}
