package com.riprod.hexcode.util;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.riprod.hexcode.codec.HexCodecs;
import com.riprod.hexcode.data.HexBookData;
import com.riprod.hexcode.hex.HexNode;
import com.riprod.hexcode.item.HexBookItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Utility class for managing HexBookData stored in ItemStack metadata.
 *
 * <p><b>IMPORTANT:</b> Hytale's ItemStack is <b>immutable</b>. All write operations
 * return a NEW ItemStack. Callers MUST use the returned ItemStack and update their
 * inventory slot accordingly.
 *
 * <h2>Storage Model</h2>
 * <p>Each Hex Book stores its complete spell data directly in ItemStack metadata:
 * <ul>
 *   <li>BookId, OwnerId - Identity</li>
 *   <li>Glyphs - Map of learned glyph instances</li>
 *   <li>SavedHexes - List of saved spell configurations</li>
 * </ul>
 *
 * <p>This makes each book self-contained. Trading a book transfers all spell data with it.
 *
 * <h2>Capacity Limits</h2>
 * <p>Book capacity is defined in the item's asset JSON via {@link HexBookItem}:
 * <ul>
 *   <li>MaxGlyphs - Maximum learnable glyphs</li>
 *   <li>MaxSavedHexes - Maximum saved spell configurations</li>
 *   <li>MaxHexDepth - Maximum spell tree complexity</li>
 * </ul>
 *
 * <h2>Usage Pattern</h2>
 * <pre>
 * // Read data from book
 * HexBookData data = HexBookItemData.getData(bookStack);
 *
 * // Modify and write back (note: withData returns NEW ItemStack!)
 * data.recordGlyphDrawing("hexcode:fire", 0.95f, 2.3f);
 * ItemStack newStack = HexBookItemData.withData(bookStack, data);
 * inventory.setUtilityItem(newStack);  // CRITICAL: update inventory!
 *
 * // Or use convenience methods that return results with the new ItemStack
 * LearnResult result = HexBookItemData.learnGlyph(bookStack, playerId, glyphId, accuracy, speed);
 * if (result.success()) {
 *     inventory.setUtilityItem(result.book());
 * }
 * </pre>
 *
 * @see HexCodecs
 * @see HexBookData
 * @see HexBookItem
 */
public final class HexBookItemData {

    private HexBookItemData() {} // Utility class

    // ==================== READ OPERATIONS ====================

    /**
     * Read HexBookData from an ItemStack.
     *
     * <p>This is a read-only operation that does not modify the ItemStack.
     *
     * @param book The Hex Book item stack
     * @return The HexBookData, or null if the item has no book data
     */
    @Nullable
    public static HexBookData getData(@Nullable ItemStack book) {
        if (book == null || book.isEmpty()) {
            return null;
        }
        return book.getFromMetadataOrNull(HexCodecs.BOOK_DATA_KEY);
    }

    /**
     * Get existing data or create new empty data for a book.
     *
     * <p>If the book has existing data, returns it unchanged.
     * Otherwise creates new data with the given owner.
     *
     * <p><b>Note:</b> If new data was created, you MUST call {@link #withData}
     * and update your inventory slot to persist it.
     *
     * @param book The Hex Book item stack
     * @param ownerId The player UUID to use if creating new data
     * @return Existing data or newly created data
     */
    @Nonnull
    public static HexBookData getOrCreateData(@Nonnull ItemStack book, @Nonnull UUID ownerId) {
        HexBookData existing = getData(book);
        if (existing != null) {
            return existing;
        }
        return HexBookData.createNew(ownerId);
    }

    /**
     * Check if the ItemStack has book data stored.
     *
     * @param book The item stack to check
     * @return true if the item has HexBookData in metadata
     */
    public static boolean hasData(@Nullable ItemStack book) {
        return getData(book) != null;
    }

    // ==================== WRITE OPERATIONS ====================

    /**
     * Write HexBookData to an ItemStack.
     *
     * <p><b>CRITICAL:</b> ItemStack is immutable! This returns a NEW ItemStack.
     * You MUST update your inventory slot with the returned stack.
     *
     * <p>Automatically updates the lastModifiedAt timestamp.
     *
     * @param book The original Hex Book item stack
     * @param data The HexBookData to store
     * @return NEW ItemStack with the data stored (caller must update inventory!)
     */
    @Nonnull
    public static ItemStack withData(@Nonnull ItemStack book, @Nonnull HexBookData data) {
        data.markModified();
        return book.withMetadata(HexCodecs.BOOK_DATA_KEY, data);
    }

    // ==================== CAPACITY CHECKING ====================

    /**
     * Get the capacity limits for a book based on its item definition.
     *
     * <p>If the item is a {@link HexBookItem}, returns its configured limits.
     * Otherwise returns default values.
     *
     * @param book The Hex Book item stack
     * @return The book's capacity limits
     */
    @Nonnull
    public static BookCapacity getCapacity(@Nonnull ItemStack book) {
        Item itemDef = book.getItem();
        if (itemDef instanceof HexBookItem hexBook) {
            return new BookCapacity(
                hexBook.getMaxGlyphs(),
                hexBook.getMaxSavedHexes(),
                hexBook.getMaxHexDepth()
            );
        }
        // Fallback to defaults for non-HexBookItem books
        return new BookCapacity(
            HexBookItem.DEFAULT_MAX_GLYPHS,
            HexBookItem.DEFAULT_MAX_SAVED_HEXES,
            HexBookItem.DEFAULT_MAX_HEX_DEPTH
        );
    }

    /**
     * Check if a book can hold more glyphs.
     *
     * @param book The Hex Book item stack
     * @return true if the book has room for more glyphs
     */
    public static boolean canAddGlyph(@Nonnull ItemStack book) {
        HexBookData data = getData(book);
        if (data == null) {
            return true;  // Empty book can add glyphs
        }
        BookCapacity capacity = getCapacity(book);
        return data.getGlyphCount() < capacity.maxGlyphs();
    }

    /**
     * Check if a book can hold more saved hexes.
     *
     * @param book The Hex Book item stack
     * @return true if the book has room for more saved hexes
     */
    public static boolean canAddSavedHex(@Nonnull ItemStack book) {
        HexBookData data = getData(book);
        if (data == null) {
            return true;  // Empty book can add hexes
        }
        BookCapacity capacity = getCapacity(book);
        return data.getSavedHexCount() < capacity.maxSavedHexes();
    }

    // ==================== CONVENIENCE OPERATIONS ====================

    /**
     * Learn a glyph and return the updated ItemStack.
     *
     * <p>If the glyph is already known, updates it with better accuracy if applicable.
     * Respects the book's MaxGlyphs capacity for new glyphs.
     *
     * @param book The Hex Book item stack
     * @param ownerId The player UUID (used if creating new book data)
     * @param glyphId The glyph ID to learn
     * @param accuracy Drawing accuracy (0.0-1.0)
     * @param drawSpeed Time in seconds to draw
     * @return Result containing the updated book and success status
     */
    @Nonnull
    public static LearnResult learnGlyph(@Nonnull ItemStack book, @Nonnull UUID ownerId,
                                          @Nonnull String glyphId, float accuracy, float drawSpeed) {
        HexBookData data = getOrCreateData(book, ownerId);
        BookCapacity capacity = getCapacity(book);

        // Check if glyph already known (update is always allowed)
        boolean alreadyKnown = data.hasGlyph(glyphId);

        // Check capacity for new glyphs
        if (!alreadyKnown && data.getGlyphCount() >= capacity.maxGlyphs()) {
            return new LearnResult(book, false, "Book is full (max " + capacity.maxGlyphs() + " glyphs)");
        }

        // Record the glyph drawing
        data.recordGlyphDrawing(glyphId, accuracy, drawSpeed);
        return new LearnResult(withData(book, data), true, null);
    }

    /**
     * Add a glyph with default values (learned but not drawn).
     *
     * @param book The Hex Book item stack
     * @param ownerId The player UUID (used if creating new book data)
     * @param glyphId The glyph ID to add
     * @return Result containing the updated book and success status
     */
    @Nonnull
    public static LearnResult addGlyph(@Nonnull ItemStack book, @Nonnull UUID ownerId,
                                        @Nonnull String glyphId) {
        HexBookData data = getOrCreateData(book, ownerId);
        BookCapacity capacity = getCapacity(book);

        // Check if glyph already known
        if (data.hasGlyph(glyphId)) {
            return new LearnResult(book, true, null);  // Already known, no change needed
        }

        // Check capacity
        if (data.getGlyphCount() >= capacity.maxGlyphs()) {
            return new LearnResult(book, false, "Book is full (max " + capacity.maxGlyphs() + " glyphs)");
        }

        data.addGlyphWithDefault(glyphId);
        return new LearnResult(withData(book, data), true, null);
    }

    /**
     * Save a hex spell (HexNode root) and return the updated ItemStack.
     *
     * <p>With unified glyph/hex treatment, spells are stored as HexNode trees.
     * A single glyph is a HexNode with no children.
     *
     * <p>Respects the book's MaxSavedHexes and MaxHexDepth limits.
     * Replacing an existing hex (same ID) is always allowed.
     *
     * @param book The Hex Book item stack
     * @param ownerId The player UUID (used if creating new book data)
     * @param hexNode The HexNode root to save
     * @return Result containing the updated book and success status
     */
    @Nonnull
    public static SaveResult saveHex(@Nonnull ItemStack book, @Nonnull UUID ownerId,
                                      @Nonnull HexNode hexNode) {
        HexBookData data = getOrCreateData(book, ownerId);
        BookCapacity capacity = getCapacity(book);

        // Check hex depth
        int depth = hexNode.getDepth();
        if (depth > capacity.maxHexDepth()) {
            return new SaveResult(book, false,
                "Hex too complex (depth " + depth + ", max " + capacity.maxHexDepth() + ")");
        }

        // Check capacity (replacing existing is allowed)
        boolean isReplacing = data.hasSavedHex(hexNode.getId());
        if (!isReplacing && data.getSavedHexCount() >= capacity.maxSavedHexes()) {
            return new SaveResult(book, false,
                "Book is full (max " + capacity.maxSavedHexes() + " hexes)");
        }

        // Save the hex node
        if (!data.saveHex(hexNode, hexNode.getId())) {
            return new SaveResult(book, false, "Failed to save hex");
        }

        return new SaveResult(withData(book, data), true, null);
    }

    /**
     * Delete a saved hex and return the updated ItemStack.
     *
     * @param book The Hex Book item stack
     * @param hexId The ID of the hex to delete
     * @return Result containing the updated book and success status
     */
    @Nonnull
    public static DeleteResult deleteSavedHex(@Nonnull ItemStack book, @Nonnull String hexId) {
        HexBookData data = getData(book);
        if (data == null) {
            return new DeleteResult(book, false, "Book has no data");
        }

        if (!data.deleteSavedHex(hexId)) {
            return new DeleteResult(book, false, "Hex not found: " + hexId);
        }

        return new DeleteResult(withData(book, data), true, null);
    }

    /**
     * Record that a glyph was used (increment usage count).
     *
     * @param book The Hex Book item stack
     * @param glyphId The glyph that was used
     * @return Updated ItemStack, or original if glyph not found
     */
    @Nonnull
    public static ItemStack recordGlyphUsage(@Nonnull ItemStack book, @Nonnull String glyphId) {
        HexBookData data = getData(book);
        if (data == null || !data.hasGlyph(glyphId)) {
            return book;
        }

        data.recordGlyphUsage(glyphId);
        return withData(book, data);
    }

    /**
     * Record that a saved hex was used.
     *
     * @param book The Hex Book item stack
     * @param hexId The hex that was used
     * @return Updated ItemStack, or original if hex not found
     */
    @Nonnull
    public static ItemStack recordSavedHexUsage(@Nonnull ItemStack book, @Nonnull String hexId) {
        HexBookData data = getData(book);
        if (data == null || !data.hasSavedHex(hexId)) {
            return book;
        }

        data.recordSavedHexUsage(hexId);
        return withData(book, data);
    }

    // ==================== MIGRATION ====================

    /**
     * Migrate a book from the old UUID-reference system to the new embedded data system.
     *
     * <p>If the book already has embedded data, returns unchanged.
     * If it has an old-style UUID reference, attempts to load from file storage
     * and embed the data.
     *
     * @param book The Hex Book item stack
     * @param oldData Previously loaded data from file storage (or null)
     * @param ownerId The player UUID to use if creating new data
     * @return Migrated ItemStack with embedded data
     */
    @Nonnull
    public static ItemStack migrateIfNeeded(@Nonnull ItemStack book,
                                             @Nullable HexBookData oldData,
                                             @Nonnull UUID ownerId) {
        // Already has new format - no migration needed
        if (hasData(book)) {
            return book;
        }

        // Check for old UUID reference (from HexBookMetadata)
        UUID oldBookId = HexBookMetadata.getBookUUID(book);

        if (oldData != null) {
            // Migrate old data to embedded format
            return withData(book, oldData);
        } else if (oldBookId != null) {
            // Has old UUID but no data to migrate - create new with same ID
            HexBookData newData = HexBookData.createNew(oldBookId, ownerId);
            return withData(book, newData);
        } else {
            // Completely new book - return unchanged (data will be created on first use)
            return book;
        }
    }

    // ==================== RESULT RECORDS ====================

    /**
     * Capacity limits for a Hex Book.
     *
     * @param maxGlyphs Maximum learnable glyphs
     * @param maxSavedHexes Maximum saved spell configurations
     * @param maxHexDepth Maximum spell tree depth
     */
    public record BookCapacity(int maxGlyphs, int maxSavedHexes, int maxHexDepth) {}

    /**
     * Result of a glyph learning operation.
     *
     * @param book The (possibly updated) ItemStack
     * @param success Whether the operation succeeded
     * @param errorMessage Error message if failed, null if succeeded
     */
    public record LearnResult(
            @Nonnull ItemStack book,
            boolean success,
            @Nullable String errorMessage
    ) {}

    /**
     * Result of a hex save operation.
     *
     * @param book The (possibly updated) ItemStack
     * @param success Whether the operation succeeded
     * @param errorMessage Error message if failed, null if succeeded
     */
    public record SaveResult(
            @Nonnull ItemStack book,
            boolean success,
            @Nullable String errorMessage
    ) {}

    /**
     * Result of a hex delete operation.
     *
     * @param book The (possibly updated) ItemStack
     * @param success Whether the operation succeeded
     * @param errorMessage Error message if failed, null if succeeded
     */
    public record DeleteResult(
            @Nonnull ItemStack book,
            boolean success,
            @Nullable String errorMessage
    ) {}
}
