package com.riprod.hexcode.data;

import javax.annotation.Nonnull;

/**
 * Enum representing different types of Hex Books.
 *
 * <p>Each book type has its own data file per player per world.
 * This allows for future expansion with specialized books that have
 * different glyph pools or capabilities.
 *
 * <p>Storage path pattern: {@code {world_save_path}/hexcode/{player_uuid}/{book_type.fileId}.json}
 *
 */
public enum BookType {
    /**
     * The standard Hex Book - the default book type.
     * Contains glyphs for general spell crafting.
     */
    HEX_BOOK("hex_book", "Hex Book"),

    /**
     * Fire-specialized Hex Book (future expansion).
     * Contains fire-element glyphs and fire-enhanced spells.
     */
    FIRE_HEX_BOOK("fire_hex_book", "Fire Hex Book"),

    /**
     * Ancient Hex Book (future expansion).
     * Contains ancient/lost glyphs with powerful effects.
     */
    ANCIENT_HEX_BOOK("ancient_hex_book", "Ancient Hex Book"),

    /**
     * Void Hex Book (future expansion).
     * Contains void-element glyphs for dark magic.
     */
    VOID_HEX_BOOK("void_hex_book", "Void Hex Book");

    private final String fileId;
    private final String displayName;

    BookType(@Nonnull String fileId, @Nonnull String displayName) {
        this.fileId = fileId;
        this.displayName = displayName;
    }

    /**
     * Get the file identifier used in storage paths.
     * This is used as the JSON filename (without extension).
     *
     * @return The file ID (e.g., "hex_book")
     */
    @Nonnull
    public String getFileId() {
        return fileId;
    }

    /**
     * Get the full filename including extension.
     *
     * @return The filename (e.g., "hex_book.json")
     */
    @Nonnull
    public String getFileName() {
        return fileId + ".json";
    }

    /**
     * Get the human-readable display name.
     *
     * @return The display name (e.g., "Hex Book")
     */
    @Nonnull
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get a BookType by its file ID.
     *
     * @param fileId The file ID to look up
     * @return The matching BookType, or null if not found
     */
    @Nonnull
    public static BookType fromFileId(@Nonnull String fileId) {
        for (BookType type : values()) {
            if (type.fileId.equals(fileId)) {
                return type;
            }
        }
        // Default to HEX_BOOK for unknown types
        return HEX_BOOK;
    }

    /**
     * Get a BookType by its item ID (for future item-type mapping).
     * Currently all books map to HEX_BOOK.
     *
     * @param itemId The item ID
     * @return The corresponding BookType
     */
    @Nonnull
    public static BookType fromItemId(@Nonnull String itemId) {
        // Future: Map specific item IDs to book types
        // For now, all hex books use the standard type
        if (itemId.contains("fire")) {
            return FIRE_HEX_BOOK;
        } else if (itemId.contains("ancient")) {
            return ANCIENT_HEX_BOOK;
        } else if (itemId.contains("void")) {
            return VOID_HEX_BOOK;
        }
        return HEX_BOOK;
    }
}
