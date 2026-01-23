package com.riprod.hexcode.glyph;

import com.google.gson.JsonObject;
import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.data.GlyphInstance;
import com.riprod.hexcode.execution.SpellContext;

/**
 * Base interface for all glyphs in the Hexcode spell system.
 *
 * <p>Glyphs are the building blocks of Hexes (spell constructs). They come in three roles:
 * <ul>
 *   <li><b>EFFECT</b>: Actions like FIRE, HEAL (always leaves in the tree)</li>
 *   <li><b>MODIFIER</b>: Amplifiers like POWER, RANGE (wrap exactly one glyph)</li>
 *   <li><b>SELECT</b>: Targeting like BEAM, BURST (wrap one or many linked glyphs)</li>
 * </ul>
 *
 * <p>All glyphs are asset-driven: their properties (power, cost, variability) are loaded
 * from JSON asset files rather than being hard-coded. This enables external plugins to
 * define glyphs using the same system.
 *
 * <h2>Execution Model</h2>
 * <p>Glyphs execute via the {@link #cast(SpellContext)} method, which:
 * <ol>
 *   <li>Receives the current spell context with caster, targets, and multipliers</li>
 *   <li>Performs the glyph's action (damage, select targets, apply modifier, etc.)</li>
 *   <li>Returns the modified context for subsequent glyphs</li>
 * </ol>
 *
 * <h2>Per-Execution Data</h2>
 * <p>Each glyph instance can store per-execution data from the drawing system:
 * <ul>
 *   <li><b>Accuracy</b> (0.0-1.0): How well the player drew the glyph</li>
 *   <li><b>Draw Speed</b>: How long it took to draw (in seconds)</li>
 * </ul>
 * This data affects spell power through the decay calculations.
 *
 * @see GlyphRole
 * @see GlyphAssetDefinition
 * @see SpellContext
 */
public interface Glyph {

    // ========== IDENTITY ==========

    /**
     * Get the unique identifier for this glyph.
     *
     * <p>IDs follow the namespace format: {@code namespace:name}
     * (e.g., "hexcode:fire", "myplugin:custom").
     *
     * @return The unique glyph ID
     */
    String getId();

    /**
     * Get the human-readable display name.
     *
     * @return The display name (e.g., "Fire", "Power")
     */
    String getDisplayName();

    // ========== ASSET-DRIVEN PROPERTIES ==========

    /**
     * Get the asset definition for this glyph.
     *
     * <p>The asset definition contains all configurable properties loaded from
     * the glyph's JSON asset file, including:
     * <ul>
     *   <li>Base power, mana cost, and variability</li>
     *   <li>Model and drawing template paths</li>
     *   <li>Role-specific properties (damage type, range, etc.)</li>
     * </ul>
     *
     * @return The asset definition, never null for properly registered glyphs
     */
    GlyphAssetDefinition getAssetDefinition();

    // ========== REGISTRATION ==========

    /**
     * Called once when this glyph is registered with the GlyphRegistry.
     *
     * <p>Use this to perform any one-time initialization or validation.
     * The registry is provided in case the glyph needs to look up other glyphs
     * for compatibility checks.
     *
     * @param registry The glyph registry
     * @return Registration metadata (can be used for logging/debugging)
     */
    default RegisterResult onRegister(GlyphRegistry registry) {
        return RegisterResult.success(getId());
    }

    // ========== EXECUTION ==========

    /**
     * Execute this glyph as part of a spell cast.
     *
     * <p>This is the core execution method called during spell execution.
     * The implementation varies by role:
     * <ul>
     *   <li><b>EFFECT</b>: Apply damage, healing, or utility effects to targets</li>
     *   <li><b>MODIFIER</b>: Modify context multipliers (power, range, duration)</li>
     *   <li><b>SELECT</b>: Select targets and populate the context's target list</li>
     * </ul>
     *
     * <p>The context is mutable - modifications affect subsequent glyphs in the
     * same hex chain. For chain siblings, a fresh context copy is used.
     *
     * @param context The current spell context with caster, targets, and state
     * @return The (possibly modified) context for subsequent glyphs
     */
    SpellContext cast(SpellContext context);

    // ========== PER-EXECUTION DATA ==========

    /**
     * Get the drawing accuracy for this execution.
     *
     * <p>Accuracy is set by the drawing system based on how well the player
     * drew the glyph shape. Higher accuracy = more powerful effects.
     *
     * @return Accuracy from 0.0 (worst) to 1.0 (perfect)
     */
    float getAccuracy();

    /**
     * Get the draw speed for this execution.
     *
     * <p>Draw speed is the time in seconds the player took to draw the glyph.
     * Faster drawing may affect certain glyph behaviors.
     *
     * @return Draw time in seconds
     */
    float getDrawSpeed();

    /**
     * Set the per-execution drawing data.
     *
     * <p>Called by the drawing system before spell execution.
     *
     * @param accuracy Drawing accuracy from 0.0-1.0
     * @param drawSpeed Time in seconds to draw
     */
    void setExecutionData(float accuracy, float drawSpeed);

    // ========== MANA CALCULATION ==========

    /**
     * Calculate the mana cost for executing this glyph.
     *
     * <p>The base cost comes from the asset definition. Implementations may
     * apply context-based modifiers (e.g., based on number of targets).
     *
     * @param context The current spell context
     * @return The mana cost for this glyph
     */
    float calculateManaCost(SpellContext context);

    // ========== VISUAL ==========

    /**
     * Get the visual properties for this glyph.
     *
     * <p>Visual properties include color, shape, and particle effects
     * used for rendering the glyph in the orbital ring and crafting space.
     *
     * @return The visual properties
     */
    GlyphVisual getVisual();

    // ========== REGISTRATION RESULT ==========

    /**
     * Result of glyph registration, containing success status and metadata.
     */
    class RegisterResult {
        private final boolean success;
        private final String glyphId;
        private final String message;

        private RegisterResult(boolean success, String glyphId, String message) {
            this.success = success;
            this.glyphId = glyphId;
            this.message = message;
        }

        public static RegisterResult success(String glyphId) {
            return new RegisterResult(true, glyphId, "Registered successfully");
        }

        public static RegisterResult success(String glyphId, String message) {
            return new RegisterResult(true, glyphId, message);
        }

        public static RegisterResult failure(String glyphId, String message) {
            return new RegisterResult(false, glyphId, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getGlyphId() {
            return glyphId;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return String.format("RegisterResult{success=%s, glyphId='%s', message='%s'}",
                    success, glyphId, message);
        }
    }
}
