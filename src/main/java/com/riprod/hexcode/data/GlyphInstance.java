package com.riprod.hexcode.data;

import com.google.gson.JsonObject;
import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.executing.SpellContext;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Immutable glyph instance data stored per-book.
 *
 * Contains drawing accuracy, speed, and usage statistics for a single glyph
 * as recorded in a specific Hex Book.
 *
 * @see HexBookData
 */
public class GlyphInstance {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private Glyph glyph;
    private String glyphId; // Stored for validation and error messages
    private float accuracy;
    private float drawSpeed;
    private long drawnTimestamp;
    private int timesUsed;
    private boolean valid;

    /**
     * Create a new glyph instance data.
     *
     * @param glyph          The glyph (must not be null)
     * @param accuracy       Drawing accuracy (0.0-1.0)
     * @param drawSpeed      Time in seconds to draw
     * @param drawnTimestamp When the glyph was drawn (epoch millis)
     * @param timesUsed      How many times this instance has been cast
     * @throws IllegalArgumentException if glyph is null
     */
    public GlyphInstance(@Nonnull Glyph glyph, float accuracy, float drawSpeed,
            long drawnTimestamp, int timesUsed) {
        if (glyph == null) {
            throw new IllegalArgumentException("Glyph cannot be null");
        }
        glyph.setExecutionData(accuracy, drawSpeed);
        this.glyph = glyph;
        this.glyphId = glyph.getId();
        this.accuracy = clampAccuracy(accuracy);
        this.drawSpeed = Math.max(0f, drawSpeed);
        this.drawnTimestamp = drawnTimestamp;
        this.timesUsed = Math.max(0, timesUsed);
        this.valid = true;
    }

    /**
     * Private constructor for invalid/placeholder instances.
     * Used when a glyph ID cannot be resolved.
     */
    private GlyphInstance(String invalidGlyphId) {
        this.glyph = null;
        this.glyphId = invalidGlyphId;
        this.accuracy = 0.75f;
        this.drawSpeed = 0f;
        this.drawnTimestamp = System.currentTimeMillis();
        this.timesUsed = 0;
        this.valid = false;
    }

    /** Private constructor for deserialization */
    private GlyphInstance() {
        this.accuracy = 0.75f;
        this.drawSpeed = 0f;
        this.drawnTimestamp = System.currentTimeMillis();
        this.timesUsed = 0;
        this.valid = false;
    }

    // ========== FACTORY METHODS ==========

    /**
     * Create glyph data from a glyph ID.
     *
     * @param glyphId The glyph ID to look up
     * @return A GlyphInstance, which may be invalid if the glyph ID is not registered
     */
    @Nonnull
    public static GlyphInstance initial(@Nonnull String glyphId) {
        Glyph glyph = GlyphRegistry.getInstance().getGlyph(glyphId);
        if (glyph == null) {
            LOGGER.atWarning().log("Cannot create GlyphInstance: glyph ID '%s' not found in registry", glyphId);
            return createInvalid(glyphId);
        }
        return initial(glyph);
    }

    /**
     * Create glyph data from a glyph ID, returning Optional.
     *
     * @param glyphId The glyph ID to look up
     * @return Optional containing the GlyphInstance, or empty if glyph not found
     */
    @Nonnull
    public static Optional<GlyphInstance> tryInitial(@Nonnull String glyphId) {
        Glyph glyph = GlyphRegistry.getInstance().getGlyph(glyphId);
        if (glyph == null) {
            return Optional.empty();
        }
        return Optional.of(initial(glyph));
    }

    /**
     * Create glyph data from a glyph.
     *
     * @param glyph The glyph (must not be null)
     * @return A valid GlyphInstance
     * @throws IllegalArgumentException if glyph is null
     */
    @Nonnull
    public static GlyphInstance initial(@Nonnull Glyph glyph) {
        return new GlyphInstance(glyph, 1, 1, System.currentTimeMillis(), 0);
    }

    /**
     * Create glyph data from drawing with glyph ID lookup.
     *
     * @param glyphId The glyph ID to look up
     * @param accuracy Drawing accuracy (0.0-1.0)
     * @param drawSpeed Time in seconds to draw
     * @return A GlyphInstance, which may be invalid if the glyph ID is not registered
     */
    @Nonnull
    public static GlyphInstance fromDrawing(@Nonnull String glyphId, float accuracy, float drawSpeed) {
        Glyph glyph = GlyphRegistry.getInstance().getGlyph(glyphId);
        if (glyph == null) {
            LOGGER.atWarning().log("Cannot create GlyphInstance from drawing: glyph ID '%s' not found in registry", glyphId);
            return createInvalid(glyphId);
        }
        return fromDrawing(glyph, accuracy, drawSpeed);
    }

    /**
     * Create glyph data from drawing, returning Optional.
     *
     * @param glyphId The glyph ID to look up
     * @param accuracy Drawing accuracy (0.0-1.0)
     * @param drawSpeed Time in seconds to draw
     * @return Optional containing the GlyphInstance, or empty if glyph not found
     */
    @Nonnull
    public static Optional<GlyphInstance> tryFromDrawing(@Nonnull String glyphId, float accuracy, float drawSpeed) {
        Glyph glyph = GlyphRegistry.getInstance().getGlyph(glyphId);
        if (glyph == null) {
            return Optional.empty();
        }
        return Optional.of(fromDrawing(glyph, accuracy, drawSpeed));
    }

    /**
     * Create glyph data from drawing.
     *
     * @param glyph The glyph (must not be null)
     * @param accuracy Drawing accuracy (0.0-1.0)
     * @param drawSpeed Time in seconds to draw
     * @return A valid GlyphInstance
     * @throws IllegalArgumentException if glyph is null
     */
    @Nonnull
    public static GlyphInstance fromDrawing(@Nonnull Glyph glyph, float accuracy, float drawSpeed) {
        return new GlyphInstance(glyph, accuracy, drawSpeed, System.currentTimeMillis(), 0);
    }

    /**
     * Create an invalid placeholder instance for an unregistered glyph ID.
     * This allows graceful handling of missing glyphs rather than NPE.
     *
     * @param glyphId The unregistered glyph ID
     * @return An invalid GlyphInstance
     */
    @Nonnull
    public static GlyphInstance createInvalid(@Nonnull String glyphId) {
        return new GlyphInstance(glyphId);
    }

    // ========== VALIDATION ==========

    /**
     * Check if this glyph instance is valid.
     * An instance is invalid if it was created with an unregistered glyph ID.
     *
     * @return true if this instance has a valid glyph
     */
    public boolean isValid() {
        return valid && glyph != null;
    }

    /**
     * Get the glyph ID (available even for invalid instances).
     *
     * @return The glyph ID
     */
    @Nonnull
    public String getGlyphId() {
        return glyphId != null ? glyphId : "unknown";
    }

    // ========== GETTERS ==========

    /**
     * Get the glyph.
     *
     * @return The glyph, or null if this instance is invalid
     */
    @Nullable
    public Glyph getGlyph() {
        return glyph;
    }

    /**
     * Get the glyph, throwing if invalid.
     *
     * @return The glyph
     * @throws IllegalStateException if this instance is invalid
     */
    @Nonnull
    public Glyph getGlyphOrThrow() {
        if (!isValid()) {
            throw new IllegalStateException("Cannot get glyph from invalid GlyphInstance for ID: " + glyphId);
        }
        return glyph;
    }

    /**
     * Get the glyph as Optional.
     *
     * @return Optional containing the glyph if valid
     */
    @Nonnull
    public Optional<Glyph> getGlyphOptional() {
        return Optional.ofNullable(glyph);
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

    /**
     * Cast this glyph in the given context.
     *
     * @param context The spell context
     * @return The modified context
     * @throws IllegalStateException if this instance is invalid
     */
    public SpellContext cast(SpellContext context) {
        if (!isValid()) {
            LOGGER.atWarning().log("Attempted to cast invalid glyph instance for ID: %s", glyphId);
            return context; // Return unmodified context for invalid glyphs
        }
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
        json.addProperty("glyphId", getGlyphId());
        json.addProperty("accuracy", accuracy);
        json.addProperty("drawSpeed", drawSpeed);
        json.addProperty("drawnTimestamp", drawnTimestamp);
        json.addProperty("timesUsed", timesUsed);
        json.addProperty("valid", valid);
        return json;
    }

    /**
     * Deserialize from JSON object.
     * Returns an invalid GlyphInstance if the glyph ID is not found in the registry.
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

        if (glyph == null) {
            LOGGER.atWarning().log("Deserializing GlyphInstance with unknown glyph ID: %s", glyphId);
            GlyphInstance invalid = createInvalid(glyphId);
            // Preserve the serialized data even for invalid instances
            invalid.accuracy = accuracy;
            invalid.drawSpeed = drawSpeed;
            invalid.drawnTimestamp = drawnTimestamp;
            invalid.timesUsed = timesUsed;
            return invalid;
        }

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
        if (!isValid()) {
            return String.format("GlyphInstance{INVALID id='%s', accuracy=%.2f, used=%d}",
                    glyphId, accuracy, timesUsed);
        }
        return String.format("GlyphInstance{id='%s', accuracy=%.2f (%s), used=%d}",
                glyphId, accuracy, getQualityRating(), timesUsed);
    }
}
