package com.riprod.hexcode.data;

import com.google.gson.JsonObject;
import com.riprod.hexcode.execution.SpellContext;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphRegistry;

import javax.annotation.Nonnull;

/**
 * Immutable glyph instance data stored per-book.
 *
 * Contains drawing accuracy, speed, and usage statistics for a single glyph
 * as recorded in a specific Hex Book.
 *
 * @see HexBookData
 */
public class GlyphInstance {

    private Glyph glyph;
    private float accuracy;
    private float drawSpeed;
    private long drawnTimestamp;
    private int timesUsed;

    /**
     * Create a new glyph instance data.
     *
     * @param glyph          The glyph
     * @param accuracy       Drawing accuracy (0.0-1.0)
     * @param drawSpeed      Time in seconds to draw
     * @param drawnTimestamp When the glyph was drawn (epoch millis)
     * @param timesUsed      How many times this instance has been cast
     */
    public GlyphInstance(Glyph glyph, float accuracy, float drawSpeed,
            long drawnTimestamp, int timesUsed) {
        glyph.setExecutionData(accuracy, drawSpeed);
        this.glyph = glyph;
        this.accuracy = clampAccuracy(accuracy);
        this.drawSpeed = Math.max(0f, drawSpeed);
        this.drawnTimestamp = drawnTimestamp;
        this.timesUsed = Math.max(0, timesUsed);
    }

    /** Private constructor for deserialization */
    private GlyphInstance() {
        this.accuracy = 0.75f;
        this.drawSpeed = 0f;
        this.drawnTimestamp = System.currentTimeMillis();
        this.timesUsed = 0;
    }

    // ========== FACTORY METHODS ==========

    /**
     * Create glyph data.
     */
    public static GlyphInstance initial(String glyphId) {
        Glyph glyph = GlyphRegistry.getInstance().getGlyph(glyphId);
        return initial(glyph);
    }

    /**
     * Create glyph data.
     */
    public static GlyphInstance initial(Glyph glyph) {
        return new GlyphInstance(glyph, 1, 1, System.currentTimeMillis(), 0);
    }
    
    /**
     * Create glyph data.
     */
    public static GlyphInstance fromDrawing(String glyphId, float accuracy, float drawSpeed) {
        Glyph glyph = GlyphRegistry.getInstance().getGlyph(glyphId);
        return fromDrawing(glyph, accuracy, drawSpeed);
    }
    
    /**
     * Create glyph data.
     */
    public static GlyphInstance fromDrawing(Glyph glyph, float accuracy, float drawSpeed) {
        return new GlyphInstance(glyph, accuracy, drawSpeed, System.currentTimeMillis(), 0);
    }

    // ========== GETTERS ==========

    public Glyph getGlyph() {
        return glyph;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public float getDrawSpeed() {
        return drawSpeed;
    }

    public long getDrawnTimestamp() {
        return drawnTimestamp;
    }

    public int getTimesUsed() {
        return timesUsed;
    }

    public SpellContext cast(SpellContext context) {
        return this.glyph.cast(context);
    }

    // ========== IMMUTABLE UPDATE METHODS ==========

    /**
     * Create a new instance with incremented usage count.
     */
    public GlyphInstance withIncrementedUsage() {
        return new GlyphInstance(glyph, accuracy, drawSpeed, drawnTimestamp, timesUsed + 1);
    }

    public int incrementUsage() {
        return timesUsed + 1;
    }

    public GlyphInstance withBestAccuracy(float newAccuracy, float newDrawSpeed) {
        if (newAccuracy > this.accuracy) {
            return new GlyphInstance(glyph, newAccuracy, newDrawSpeed, System.currentTimeMillis(), timesUsed);
        } else {
            return this;
        }
    }

    // ========== JSON SERIALIZATION ==========

    /**
     * Serialize to JSON object.
     */
    @Nonnull
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("glyphId", glyph.getId());
        json.addProperty("accuracy", accuracy);
        json.addProperty("drawSpeed", drawSpeed);
        json.addProperty("drawnTimestamp", drawnTimestamp);
        json.addProperty("timesUsed", timesUsed);
        return json;
    }

    /**
     * Deserialize from JSON object.
     */
    @Nonnull
    public static GlyphInstance fromJson(@Nonnull JsonObject json) {
        String glyphId = json.has("glyphId") ? json.get("glyphId").getAsString() : "unknown";
        Glyph glyph = GlyphRegistry.getInstance().getGlyph(glyphId);
        float accuracy = json.has("accuracy") ? json.get("accuracy").getAsFloat() : 0.75f;
        float drawSpeed = json.has("drawSpeed") ? json.get("drawSpeed").getAsFloat() : 0f;
        long drawnTimestamp = json.has("drawnTimestamp") ? json.get("drawnTimestamp").getAsLong()
                : System.currentTimeMillis();
        int timesUsed = json.has("timesUsed") ? json.get("timesUsed").getAsInt() : 0;

        return new GlyphInstance(glyph, accuracy, drawSpeed, drawnTimestamp, timesUsed);
    }

    // ========== UTILITY ==========

    private static float clampAccuracy(float accuracy) {
        return Math.max(0f, Math.min(1f, accuracy));
    }

    /**
     * Get a quality rating based on accuracy.
     */
    public String getQualityRating() {
        if (accuracy >= 0.98f)
            return "Perfect";
        if (accuracy >= 0.85f)
            return "Excellent";
        if (accuracy >= 0.70f)
            return "Good";
        if (accuracy >= 0.50f)
            return "Fair";
        return "Poor";
    }

    @Override
    public String toString() {
        return String.format("BookGlyphInstanceData{id='%s', accuracy=%.2f (%s), used=%d}",
                glyph.getId(), accuracy, getQualityRating(), timesUsed);
    }
}
