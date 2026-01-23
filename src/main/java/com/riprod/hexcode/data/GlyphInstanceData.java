package com.riprod.hexcode.data;

import com.google.gson.JsonObject;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonString;

import javax.annotation.Nonnull;

/**
 * Immutable data class for a single glyph instance's drawing data.
 *
 * <p>Each player can have one instance of each glyph, storing:
 * <ul>
 *   <li><b>Accuracy</b>: How well the glyph was drawn (0.0-1.0)</li>
 *   <li><b>Draw Speed</b>: Time in seconds to draw the glyph</li>
 *   <li><b>Drawn Timestamp</b>: When the glyph was drawn</li>
 *   <li><b>Times Used</b>: How many times this glyph instance has been cast</li>
 * </ul>
 *
 * <p>This data affects spell power through accuracy bonuses and may affect
 * future features like glyph mastery.
 *
 * <h2>Serialization</h2>
 * <p>Uses BSON serialization via {@link #toBson()} and {@link #fromBson(BsonDocument)}
 * for persistence with BsonUtil.
 *
 * @see PlayerGlyphData
 * @deprecated Use {@link GlyphInstance} instead.
 */
public class GlyphInstanceData {

    private final String baseGlyphId;
    private final float accuracy;
    private final float drawSpeed;
    private final long drawnTimestamp;
    private final int timesUsed;

    /**
     * Create a new glyph instance data.
     *
     * @param baseGlyphId The base glyph ID (e.g., "hexcode:fire")
     * @param accuracy Drawing accuracy (0.0-1.0)
     * @param drawSpeed Time in seconds to draw
     * @param drawnTimestamp When the glyph was drawn (epoch millis)
     * @param timesUsed How many times this instance has been cast
     */
    public GlyphInstanceData(String baseGlyphId, float accuracy, float drawSpeed,
                             long drawnTimestamp, int timesUsed) {
        this.baseGlyphId = baseGlyphId;
        this.accuracy = clampAccuracy(accuracy);
        this.drawSpeed = Math.max(0, drawSpeed);
        this.drawnTimestamp = drawnTimestamp;
        this.timesUsed = Math.max(0, timesUsed);
    }

    /**
     * Create initial glyph data when first learned (before drawing).
     *
     * @param baseGlyphId The base glyph ID
     * @return Initial glyph instance with default values
     */
    public static GlyphInstanceData initial(String baseGlyphId) {
        return new GlyphInstanceData(baseGlyphId, 0.75f, 0f, System.currentTimeMillis(), 0);
    }

    /**
     * Create glyph data from a drawing.
     *
     * @param baseGlyphId The base glyph ID
     * @param accuracy Drawing accuracy
     * @param drawSpeed Time to draw
     * @return New glyph instance data
     */
    public static GlyphInstanceData fromDrawing(String baseGlyphId, float accuracy, float drawSpeed) {
        return new GlyphInstanceData(baseGlyphId, accuracy, drawSpeed, System.currentTimeMillis(), 0);
    }

    // ========== GETTERS ==========

    /**
     * @return The base glyph ID (e.g., "hexcode:fire")
     */
    public String getBaseGlyphId() {
        return baseGlyphId;
    }

    /**
     * @return Drawing accuracy from 0.0 (worst) to 1.0 (perfect)
     */
    public float getAccuracy() {
        return accuracy;
    }

    /**
     * @return Time in seconds to draw this glyph
     */
    public float getDrawSpeed() {
        return drawSpeed;
    }

    /**
     * @return Epoch millis when this glyph was drawn
     */
    public long getDrawnTimestamp() {
        return drawnTimestamp;
    }

    /**
     * @return Number of times this glyph instance has been cast
     */
    public int getTimesUsed() {
        return timesUsed;
    }

    // ========== BUILDER METHODS (Return new instance) ==========

    /**
     * Create a new instance with incremented use count.
     *
     * @return New GlyphInstanceData with timesUsed + 1
     */
    public GlyphInstanceData withIncrementedUseCount() {
        return new GlyphInstanceData(baseGlyphId, accuracy, drawSpeed, drawnTimestamp, timesUsed + 1);
    }

    /**
     * Create a new instance with updated drawing data.
     *
     * @param newAccuracy New accuracy value
     * @param newDrawSpeed New draw speed
     * @return New GlyphInstanceData with updated values
     */
    public GlyphInstanceData withNewDrawing(float newAccuracy, float newDrawSpeed) {
        return new GlyphInstanceData(baseGlyphId, newAccuracy, newDrawSpeed, System.currentTimeMillis(), timesUsed);
    }

    /**
     * Create a new instance with merged best values.
     *
     * <p>Takes the best accuracy between this and the new drawing.
     *
     * @param newAccuracy New accuracy value
     * @param newDrawSpeed New draw speed
     * @return New GlyphInstanceData with best accuracy
     */
    public GlyphInstanceData withBestDrawing(float newAccuracy, float newDrawSpeed) {
        float bestAccuracy = Math.max(this.accuracy, newAccuracy);
        return new GlyphInstanceData(baseGlyphId, bestAccuracy, newDrawSpeed, System.currentTimeMillis(), timesUsed);
    }

    // ========== SERIALIZATION ==========

    /**
     * Serialize to BSON document.
     *
     * @return BsonDocument representation
     */
    @Nonnull
    public BsonDocument toBson() {
        BsonDocument doc = new BsonDocument();
        doc.put("baseGlyphId", new BsonString(baseGlyphId));
        doc.put("accuracy", new org.bson.BsonDouble(accuracy));
        doc.put("drawSpeed", new org.bson.BsonDouble(drawSpeed));
        doc.put("drawnTimestamp", new BsonInt64(drawnTimestamp));
        doc.put("timesUsed", new BsonInt32(timesUsed));
        return doc;
    }

    /**
     * Deserialize from BSON document.
     *
     * @param doc The BsonDocument to parse
     * @return New GlyphInstanceData
     */
    @Nonnull
    public static GlyphInstanceData fromBson(@Nonnull BsonDocument doc) {
        String baseGlyphId = doc.containsKey("baseGlyphId") ? doc.getString("baseGlyphId").getValue() : "unknown";
        float accuracy = doc.containsKey("accuracy") ? (float) doc.getDouble("accuracy").getValue() : 0.75f;
        float drawSpeed = doc.containsKey("drawSpeed") ? (float) doc.getDouble("drawSpeed").getValue() : 0f;
        long drawnTimestamp = doc.containsKey("drawnTimestamp") ? doc.getInt64("drawnTimestamp").getValue() : System.currentTimeMillis();
        int timesUsed = doc.containsKey("timesUsed") ? doc.getInt32("timesUsed").getValue() : 0;

        return new GlyphInstanceData(baseGlyphId, accuracy, drawSpeed, drawnTimestamp, timesUsed);
    }

    /**
     * Serialize to JSON (for backward compatibility).
     *
     * @return JsonObject representation
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("baseGlyphId", baseGlyphId);
        json.addProperty("accuracy", accuracy);
        json.addProperty("drawSpeed", drawSpeed);
        json.addProperty("drawnTimestamp", drawnTimestamp);
        json.addProperty("timesUsed", timesUsed);
        return json;
    }

    /**
     * Deserialize from JSON (for backward compatibility).
     *
     * @param json The JsonObject to parse
     * @return New GlyphInstanceData
     */
    public static GlyphInstanceData fromJson(JsonObject json) {
        String baseGlyphId = json.has("baseGlyphId") ? json.get("baseGlyphId").getAsString() : "unknown";
        float accuracy = json.has("accuracy") ? json.get("accuracy").getAsFloat() : 0.75f;
        float drawSpeed = json.has("drawSpeed") ? json.get("drawSpeed").getAsFloat() : 0f;
        long drawnTimestamp = json.has("drawnTimestamp") ? json.get("drawnTimestamp").getAsLong() : System.currentTimeMillis();
        int timesUsed = json.has("timesUsed") ? json.get("timesUsed").getAsInt() : 0;

        return new GlyphInstanceData(baseGlyphId, accuracy, drawSpeed, drawnTimestamp, timesUsed);
    }

    // ========== UTILITY ==========

    /**
     * Clamp accuracy to valid range [0.0, 1.0].
     */
    private static float clampAccuracy(float accuracy) {
        return Math.max(0f, Math.min(1f, accuracy));
    }

    /**
     * Get a quality rating based on accuracy.
     *
     * @return Rating: "Poor", "Fair", "Good", "Excellent", or "Perfect"
     */
    public String getQualityRating() {
        if (accuracy >= 0.98f) return "Perfect";
        if (accuracy >= 0.85f) return "Excellent";
        if (accuracy >= 0.70f) return "Good";
        if (accuracy >= 0.50f) return "Fair";
        return "Poor";
    }

    @Override
    public String toString() {
        return String.format("GlyphInstanceData{id='%s', accuracy=%.2f (%s), used=%d}",
                baseGlyphId, accuracy, getQualityRating(), timesUsed);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        GlyphInstanceData other = (GlyphInstanceData) obj;
        return Float.compare(other.accuracy, accuracy) == 0 &&
               Float.compare(other.drawSpeed, drawSpeed) == 0 &&
               drawnTimestamp == other.drawnTimestamp &&
               timesUsed == other.timesUsed &&
               baseGlyphId.equals(other.baseGlyphId);
    }

    @Override
    public int hashCode() {
        int result = baseGlyphId.hashCode();
        result = 31 * result + Float.floatToIntBits(accuracy);
        result = 31 * result + Float.floatToIntBits(drawSpeed);
        result = 31 * result + (int) (drawnTimestamp ^ (drawnTimestamp >>> 32));
        result = 31 * result + timesUsed;
        return result;
    }
}
