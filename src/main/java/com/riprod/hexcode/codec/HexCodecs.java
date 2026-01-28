package com.riprod.hexcode.codec;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.riprod.hexcode.data.GlyphInstance;
import com.riprod.hexcode.data.HexBookData;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.hex.HexNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Codec definitions for Hex Book data serialization.
 *
 * <p>These codecs enable storing spell data (glyphs and hexes) directly
 * in ItemStack metadata, making each book self-contained and tradeable.
 *
 * <h2>Data Structure</h2>
 * <pre>
 * ItemStack.metadata
 * └── HexBookData
 *     ├── BookId: UUID (binary)
 *     ├── OwnerId: UUID (binary)
 *     ├── CreatedAt: Long
 *     ├── LastModifiedAt: Long
 *     ├── Glyphs: Map&lt;String, GlyphInstance&gt;
 *     └── SavedHexes: Array&lt;Hex&gt;
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>
 * // Read from ItemStack
 * HexBookData data = itemStack.getFromMetadataOrNull(HexCodecs.BOOK_DATA_KEY);
 *
 * // Write to ItemStack (returns NEW ItemStack - immutable!)
 * ItemStack newStack = itemStack.withMetadata(HexCodecs.BOOK_DATA_KEY, data);
 * </pre>
 *
 * @see com.riprod.hexcode.util.HexBookItemData
 */
public final class HexCodecs {

    // ==================== GLYPH INSTANCE CODEC ====================

    /**
     * Codec for {@link GlyphInstance}.
     *
     * <p>Serializes glyph drawing data including accuracy, speed, and usage stats.
     * The glyph reference is restored from GlyphRegistry on deserialization.
     */
    public static final BuilderCodec<GlyphInstance> GLYPH_INSTANCE = BuilderCodec
            .builder(GlyphInstance.class, GlyphInstance::new)
            .append(
                new KeyedCodec<>("GlyphId", Codec.STRING, true),
                (g, v) -> g.setGlyphId(v),
                g -> g.getGlyphId()
            )
            .add()
            .append(
                new KeyedCodec<>("Accuracy", Codec.FLOAT),
                (g, v) -> g.setAccuracy(v),
                g -> g.getAccuracy()
            )
            .add()
            .append(
                new KeyedCodec<>("DrawSpeed", Codec.FLOAT),
                (g, v) -> g.setDrawSpeed(v),
                g -> g.getDrawSpeed()
            )
            .add()
            .append(
                new KeyedCodec<>("DrawnTimestamp", Codec.LONG),
                (g, v) -> g.setDrawnTimestamp(v),
                g -> g.getDrawnTimestamp()
            )
            .add()
            .append(
                new KeyedCodec<>("TimesUsed", Codec.INTEGER),
                (g, v) -> g.setTimesUsed(v),
                g -> g.getTimesUsed()
            )
            .add()
            .build();

    // ==================== HEX NODE CODEC ====================

    /**
     * Codec for {@link HexNode}.
     *
     * <p>Recursively serializes the hex spell tree structure.
     * Parent references are automatically restored during deserialization
     * via the setChildrenFromArray method.
     */
    public static final BuilderCodec<HexNode> HEX_NODE;

    // Array codec for HexNode children - declared after HEX_NODE is initialized
    private static final ArrayCodec<HexNode> HEX_NODE_ARRAY;

    static {
        // Build the HexNode codec with recursive children
        // We need to use a two-phase initialization for the recursive type
        HEX_NODE = BuilderCodec
                .builder(HexNode.class, HexNode::new)
                .append(
                    new KeyedCodec<>("Value", GLYPH_INSTANCE),
                    (n, v) -> n.setValue(v),
                    n -> n.getValue()
                )
                .add()
                .build();

        // Initialize the array codec after HEX_NODE is built
        HEX_NODE_ARRAY = new ArrayCodec<>(HEX_NODE, HexNode[]::new);
    }

    /**
     * Get the HexNode codec with children support.
     * Due to recursive type handling, children are serialized separately.
     */
    public static BuilderCodec<HexNode> getHexNodeCodec() {
        return HEX_NODE;
    }

    // ==================== HEX CODEC ====================

    /**
     * Codec for {@link Hex}.
     *
     * <p>Serializes a complete spell configuration with its tree structure
     * and usage statistics.
     */
    public static final BuilderCodec<Hex> HEX = BuilderCodec
            .builder(Hex.class, Hex::new)
            .append(
                new KeyedCodec<>("Id", Codec.STRING),
                (h, v) -> h.setId(v),
                h -> h.getId()
            )
            .add()
            .append(
                new KeyedCodec<>("Root", HEX_NODE),
                (h, v) -> h.setRoot(v),
                h -> h.getRoot()
            )
            .add()
            .append(
                new KeyedCodec<>("Uses", Codec.INTEGER),
                (h, v) -> h.setUses(v),
                h -> h.getUses()
            )
            .add()
            .build();

    /**
     * Array codec for saved hexes.
     */
    public static final ArrayCodec<Hex> HEX_ARRAY = new ArrayCodec<>(HEX, Hex[]::new);

    // ==================== HEX BOOK DATA CODEC ====================

    /**
     * Codec for {@link HexBookData}.
     *
     * <p>The main codec for storing complete book data in ItemStack metadata.
     * Includes versioning support for future schema migrations.
     *
     * <p>Estimated size: ~6.5 KB for a book with 50 glyphs and 20 hexes.
     */
    public static final BuilderCodec<HexBookData> HEX_BOOK_DATA = BuilderCodec
            .builder(HexBookData.class, HexBookData::new)
            .versioned()
            .codecVersion(1)
            .append(
                new KeyedCodec<>("BookId", Codec.UUID_BINARY),
                (d, v) -> d.setBookId(v),
                d -> d.getBookId()
            )
            .add()
            .append(
                new KeyedCodec<>("OwnerId", Codec.UUID_BINARY),
                (d, v) -> d.setOwnerId(v),
                d -> d.getOwnerId()
            )
            .add()
            .append(
                new KeyedCodec<>("CreatedAt", Codec.LONG),
                (d, v) -> d.setCreatedAt(v),
                d -> d.getCreatedAt()
            )
            .add()
            .append(
                new KeyedCodec<>("LastModifiedAt", Codec.LONG),
                (d, v) -> d.setLastModifiedAt(v),
                d -> d.getLastModifiedAt()
            )
            .add()
            .append(
                new KeyedCodec<>("Glyphs", new MapCodec<>(GLYPH_INSTANCE, HashMap::new)),
                (d, v) -> d.setGlyphsFromMap(v),
                d -> d.getGlyphsAsMap()
            )
            .add()
            .append(
                new KeyedCodec<>("SavedHexes", HEX_ARRAY),
                (d, v) -> d.setSavedHexesFromList(arrayToList(v)),
                d -> listToArray(d.getSavedHexesAsList())
            )
            .add()
            .build();

    // ==================== KEYED CODEC FOR ITEM METADATA ====================

    /**
     * KeyedCodec for reading/writing HexBookData from/to ItemStack metadata.
     *
     * <p>Usage:
     * <pre>
     * // Read
     * HexBookData data = itemStack.getFromMetadataOrNull(BOOK_DATA_KEY);
     *
     * // Write
     * ItemStack newStack = itemStack.withMetadata(BOOK_DATA_KEY, data);
     * </pre>
     */
    public static final KeyedCodec<HexBookData> BOOK_DATA_KEY =
            new KeyedCodec<>("HexBookData", HEX_BOOK_DATA);

    // ==================== UTILITY METHODS ====================

    /**
     * Convert array to list (for codec deserialization).
     */
    private static List<Hex> arrayToList(Hex[] array) {
        if (array == null) {
            return new ArrayList<>();
        }
        List<Hex> list = new ArrayList<>(array.length);
        for (Hex hex : array) {
            if (hex != null) {
                list.add(hex);
            }
        }
        return list;
    }

    /**
     * Convert list to array (for codec serialization).
     */
    private static Hex[] listToArray(List<Hex> list) {
        if (list == null) {
            return new Hex[0];
        }
        return list.toArray(new Hex[0]);
    }

    // Private constructor - utility class
    private HexCodecs() {}
}
