package com.riprod.hexcode.item;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;

/**
 * Custom item type for Hex Books with configurable capacity limits.
 *
 * <p>Hex Books store spell data (glyphs and hexes) directly in ItemStack metadata.
 * Different book types can define different capacity limits in their asset JSON.
 *
 * <h2>Asset JSON Configuration</h2>
 * <pre>
 * {
 *   "Id": "My_Custom_HexBook",
 *   "Type": "HexBook",
 *   "Parent": "Template_HexBook",
 *   "MaxGlyphs": 50,
 *   "MaxSavedHexes": 20,
 *   "MaxHexDepth": 10
 * }
 * </pre>
 *
 * <h2>Default Values (from Template_HexBook)</h2>
 * <ul>
 *   <li><b>MaxGlyphs</b>: 30 - Maximum glyphs that can be learned</li>
 *   <li><b>MaxSavedHexes</b>: 10 - Maximum saved spell configurations</li>
 *   <li><b>MaxHexDepth</b>: 8 - Maximum depth of hex spell trees</li>
 * </ul>
 *
 * @see com.riprod.hexcode.codec.HexCodecs
 * @see com.riprod.hexcode.util.HexBookItemData
 */
public class HexBookItem extends Item {

    // ==================== DEFAULT CAPACITY VALUES ====================

    /** Default maximum number of glyphs a book can hold */
    public static final int DEFAULT_MAX_GLYPHS = 30;

    /** Default maximum number of saved hexes per book */
    public static final int DEFAULT_MAX_SAVED_HEXES = 10;

    /** Default maximum depth of hex spell trees */
    public static final int DEFAULT_MAX_HEX_DEPTH = 8;

    // ==================== CODEC ====================

    /**
     * BuilderCodec for HexBookItem deserialization from asset JSON.
     *
     * <p>Uses appendInherited for proper inheritance from parent items
     * (e.g., Template_HexBook).
     */
    public static final BuilderCodec<HexBookItem> CODEC = BuilderCodec.builder(
            HexBookItem.class,
            HexBookItem::new)
        .appendInherited(
            new KeyedCodec<>("MaxGlyphs", Codec.INTEGER),
            (item, value) -> item.maxGlyphs = value,
            item -> item.maxGlyphs,
            (item, parent) -> item.maxGlyphs = parent.maxGlyphs)
        .add()
        .appendInherited(
            new KeyedCodec<>("MaxSavedHexes", Codec.INTEGER),
            (item, value) -> item.maxSavedHexes = value,
            item -> item.maxSavedHexes,
            (item, parent) -> item.maxSavedHexes = parent.maxSavedHexes)
        .add()
        .appendInherited(
            new KeyedCodec<>("MaxHexDepth", Codec.INTEGER),
            (item, value) -> item.maxHexDepth = value,
            item -> item.maxHexDepth,
            (item, parent) -> item.maxHexDepth = parent.maxHexDepth)
        .add()
        .build();

    // ==================== CAPACITY FIELDS ====================

    /** Maximum number of glyphs this book can hold */
    protected int maxGlyphs = DEFAULT_MAX_GLYPHS;

    /** Maximum number of saved hexes this book can store */
    protected int maxSavedHexes = DEFAULT_MAX_SAVED_HEXES;

    /** Maximum depth of hex spell trees in this book */
    protected int maxHexDepth = DEFAULT_MAX_HEX_DEPTH;

    // ==================== CONSTRUCTOR ====================

    /**
     * Default constructor required for codec deserialization.
     */
    public HexBookItem() {
        super();
    }

    // ==================== GETTERS ====================

    /**
     * Get the maximum number of glyphs this book can hold.
     *
     * @return Maximum glyph capacity
     */
    public int getMaxGlyphs() {
        return maxGlyphs;
    }

    /**
     * Get the maximum number of saved hexes this book can store.
     *
     * @return Maximum saved hex capacity
     */
    public int getMaxSavedHexes() {
        return maxSavedHexes;
    }

    /**
     * Get the maximum depth of hex spell trees in this book.
     *
     * <p>Hex depth determines how complex spells can be. A depth of 8 means
     * spells can have up to 8 levels of nested glyph compositions.
     *
     * @return Maximum hex tree depth
     */
    public int getMaxHexDepth() {
        return maxHexDepth;
    }

    // ==================== UTILITY ====================

    /**
     * Check if this book has room for more glyphs.
     *
     * @param currentCount Current number of glyphs in the book
     * @return true if more glyphs can be added
     */
    public boolean canAddGlyph(int currentCount) {
        return currentCount < maxGlyphs;
    }

    /**
     * Check if this book has room for more saved hexes.
     *
     * @param currentCount Current number of saved hexes in the book
     * @return true if more hexes can be saved
     */
    public boolean canAddSavedHex(int currentCount) {
        return currentCount < maxSavedHexes;
    }

    /**
     * Check if a hex depth is valid for this book.
     *
     * @param depth The depth to check
     * @return true if the depth is within limits
     */
    public boolean isValidHexDepth(int depth) {
        return depth <= maxHexDepth;
    }

    @Override
    public String toString() {
        return String.format("HexBookItem{id=%s, maxGlyphs=%d, maxSavedHexes=%d, maxHexDepth=%d}",
                getId(), maxGlyphs, maxSavedHexes, maxHexDepth);
    }
}
