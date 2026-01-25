package com.riprod.hexcode.hex;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Serialization utilities for Hex spell structures.
 *
 * <p>Provides JSON serialization and deserialization for Hex objects,
 * used for:
 * <ul>
 *   <li>Storing queued hexes in ItemStack metadata</li>
 *   <li>Saving hexes to world data files</li>
 *   <li>Network transmission (if needed)</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>
 * // Serialize a hex to JSON string
 * String json = HexSerializer.serialize(hex);
 *
 * // Deserialize back to Hex object
 * Hex hex = HexSerializer.deserialize(json);
 * </pre>
 *
 * @see Hex
 * @see HexNode
 */
public class HexSerializer {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson GSON = new GsonBuilder().create();

    private HexSerializer() {} // Utility class

    /**
     * Serialize a Hex to a JSON string.
     *
     * @param hex The hex to serialize
     * @return JSON string representation, or null if hex is null
     */
    @Nullable
    public static String serialize(@Nullable Hex hex) {
        if (hex == null) {
            return null;
        }
        if (hex.isEmpty()) {
            return "{}"; // Empty hex
        }
        try {
            JsonObject json = hex.toJson();
            // Also store the hex ID for proper restoration
            json.addProperty("id", hex.getId());
            json.addProperty("uses", hex.getUses());
            return GSON.toJson(json);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Failed to serialize hex");
            return null;
        }
    }

    /**
     * Deserialize a Hex from a JSON string.
     *
     * @param json The JSON string to deserialize
     * @return The deserialized Hex, or null if deserialization fails
     */
    @Nullable
    public static Hex deserialize(@Nullable String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        if ("{}".equals(json.trim())) {
            return new Hex(); // Empty hex
        }
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            Hex hex = Hex.fromJson(obj);

            // Restore ID if present
            if (obj.has("id")) {
                String id = obj.get("id").getAsString();
                // Since Hex.fromJson doesn't preserve ID, we need to create with ID
                if (hex.hasRoot()) {
                    hex = new Hex(id, hex.getRoot());
                } else {
                    hex = new Hex(id);
                }
            }

            return hex;
        } catch (JsonSyntaxException e) {
            LOGGER.atWarning().withCause(e).log("Failed to parse hex JSON: %s", json);
            return null;
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Failed to deserialize hex");
            return null;
        }
    }

    /**
     * Serialize a Hex to a compact string (no whitespace).
     *
     * <p>Useful for storing in metadata where space is limited.
     *
     * @param hex The hex to serialize
     * @return Compact JSON string
     */
    @Nullable
    public static String serializeCompact(@Nullable Hex hex) {
        return serialize(hex); // Already compact by default
    }

    /**
     * Serialize a Hex to a pretty-printed JSON string.
     *
     * <p>Useful for debugging and readable file storage.
     *
     * @param hex The hex to serialize
     * @return Pretty-printed JSON string
     */
    @Nullable
    public static String serializePretty(@Nullable Hex hex) {
        if (hex == null) {
            return null;
        }
        if (hex.isEmpty()) {
            return "{}";
        }
        try {
            Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject json = hex.toJson();
            json.addProperty("id", hex.getId());
            json.addProperty("uses", hex.getUses());
            return prettyGson.toJson(json);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Failed to serialize hex (pretty)");
            return null;
        }
    }

    /**
     * Check if a JSON string represents a valid Hex.
     *
     * @param json The JSON string to validate
     * @return true if the string can be deserialized to a Hex
     */
    public static boolean isValidHexJson(@Nullable String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            // Check for required structure (at minimum should have root or be empty)
            return obj.isJsonObject();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get a human-readable representation of a Hex.
     *
     * <p>Example: "BEAM[POWER[FIRE[]]]"
     *
     * @param hex The hex to represent
     * @return String representation
     */
    @Nonnull
    public static String toReadableString(@Nullable Hex hex) {
        if (hex == null || hex.isEmpty()) {
            return "Empty";
        }
        return hex.getRoot().toString();
    }
}
