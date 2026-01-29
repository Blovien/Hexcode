package com.riprod.hexcode.util;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.server.core.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Utility for managing book UUID metadata on Hex Book ItemStacks.
 *
 * <p><b>IMPORTANT:</b> Hytale's ItemStack is <b>immutable</b>. All metadata operations
 * return a NEW ItemStack. Callers MUST use the returned ItemStack and update their
 * inventory slot accordingly.
 *
 * <h2>Storage Model (Hybrid Approach)</h2>
 * <ul>
 *   <li><b>Book UUID</b>: Stored in ItemStack metadata (set once on creation)</li>
 * </ul>
 *
 * <p>This hybrid approach avoids frequent ItemStack mutations for queued hex data,
 * which would require constant inventory slot updates.
 *
 * <h2>Usage Pattern</h2>
 * <pre>
 * // Getting or creating book UUID (may require inventory update)
 * BookUUIDResult result = HexBookMetadata.getOrCreateBookUUID(bookStack);
 * if (result.wasCreated()) {
 *     // MUST update inventory slot with new ItemStack!
 *     InventoryUtil.updateOffhandItem(inventory, result.stack());
 * }
 * UUID bookUuid = result.uuid();
 *
 * // Get existing UUID (no inventory update needed)
 * UUID existingUuid = HexBookMetadata.getBookUUID(bookStack);
 *
 * // Create new ItemStack with UUID set
 * ItemStack newStack = HexBookMetadata.withBookUUID(bookStack, uuid);
 * inventory.setUtilityItem(newStack); // Update inventory
 * </pre>
 *
 */
public class HexBookMetadata {

    /** Metadata key for the book's unique UUID */
    public static final String KEY_BOOK_UUID = "hexcode:book_uuid";

    /** Codec for UUID storage as String in ItemStack metadata */
    private static final Codec<String> UUID_CODEC = Codec.STRING;

    private HexBookMetadata() {} // Utility class

    // ==================== BOOK UUID ====================

    /**
     * Get the book's UUID if it exists.
     *
     * <p>This is a read-only operation that does not modify the ItemStack.
     *
     * @param bookStack The Hex Book item stack
     * @return The book's UUID, or null if not set
     */
    @Nullable
    public static UUID getBookUUID(@Nonnull ItemStack bookStack) {
        String uuidStr = bookStack.getFromMetadataOrNull(KEY_BOOK_UUID, UUID_CODEC);
        if (uuidStr == null || uuidStr.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            // Invalid UUID format stored
            return null;
        }
    }

    /**
     * Create a new ItemStack with a UUID set.
     *
     * <p><b>IMPORTANT:</b> ItemStack is immutable! Caller MUST use the returned ItemStack
     * and update the inventory slot with it.
     *
     * @param bookStack The original Hex Book item stack
     * @param uuid The UUID to set
     * @return NEW ItemStack with UUID metadata set
     */
    @Nonnull
    public static ItemStack withBookUUID(@Nonnull ItemStack bookStack, @Nonnull UUID uuid) {
        return bookStack.withMetadata(KEY_BOOK_UUID, UUID_CODEC, uuid.toString());
    }

    /**
     * Get or create book UUID, returning the (possibly new) ItemStack.
     *
     * <p><b>IMPORTANT:</b> If {@link BookUUIDResult#wasCreated()} returns true, the caller
     * MUST update their inventory slot with the new ItemStack from {@link BookUUIDResult#stack()}.
     *
     * <h3>Example Usage</h3>
     * <pre>
     * BookUUIDResult result = HexBookMetadata.getOrCreateBookUUID(bookStack);
     * if (result.wasCreated()) {
     *     inventory.setUtilityItem(result.stack());  // Update inventory!
     * }
     * UUID bookUuid = result.uuid();
     * </pre>
     *
     * @param bookStack The Hex Book item stack
     * @return Result containing the (possibly new) ItemStack, UUID, and whether it was created
     */
    @Nonnull
    public static BookUUIDResult getOrCreateBookUUID(@Nonnull ItemStack bookStack) {
        UUID existing = getBookUUID(bookStack);
        if (existing != null) {
            // UUID already exists - return original stack unchanged
            return new BookUUIDResult(bookStack, existing, false);
        }

        // Generate new UUID and create new ItemStack
        UUID newUuid = UUID.randomUUID();
        ItemStack newStack = withBookUUID(bookStack, newUuid);
        return new BookUUIDResult(newStack, newUuid, true);
    }

    /**
     * Check if the book has a UUID set.
     *
     * @param bookStack The Hex Book item stack
     * @return true if a valid UUID is stored in metadata
     */
    public static boolean hasBookUUID(@Nonnull ItemStack bookStack) {
        return getBookUUID(bookStack) != null;
    }

    // ==================== RESULT RECORD ====================

    /**
     * Result of {@link #getOrCreateBookUUID(ItemStack)}.
     *
     * <p>Contains the (possibly new) ItemStack, the UUID, and whether the UUID was newly created.
     * When {@code wasCreated} is true, the caller MUST update their inventory slot with
     * the new {@code stack}.
     *
     * @param stack The ItemStack (may be new if UUID was created)
     * @param uuid The book's UUID
     * @param wasCreated true if a new UUID was generated (requiring inventory update)
     */
    public record BookUUIDResult(
            @Nonnull ItemStack stack,
            @Nonnull UUID uuid,
            boolean wasCreated
    ) {
        /**
         * Get the ItemStack.
         * @return The ItemStack (may be new if UUID was created)
         */
        @Override
        @Nonnull
        public ItemStack stack() {
            return stack;
        }

        /**
         * Get the book's UUID.
         * @return The UUID
         */
        @Override
        @Nonnull
        public UUID uuid() {
            return uuid;
        }
    }
}
