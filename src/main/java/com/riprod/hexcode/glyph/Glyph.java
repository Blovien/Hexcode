package com.riprod.hexcode.glyph;

import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;

import java.util.Set;

/**
 * Base interface for all glyphs in the Hexcode spell system.
 *
 * Glyphs are the building blocks of Hexes (spell constructs). They come in three roles:
 * - EFFECT: Actions like FIRE, HEAL (always leaves in the tree)
 * - MODIFIER: Amplifiers like POWER, RANGE (wrap exactly one glyph)
 * - SELECT: Targeting like BEAM, BURST (wrap one or many linked glyphs)
 */
public interface Glyph {

    /**
     * @return Unique identifier for this glyph (e.g., "hexcode:fire")
     */
    String getId();

    /**
     * @return Human-readable display name (e.g., "Fire")
     */
    String getDisplayName();

    /**
     * @return The role of this glyph: EFFECT, MODIFIER, or SELECT
     */
    GlyphRole getRole();

    /**
     * @return Visual properties (color, model, particles)
     */
    GlyphVisual getVisual();

    // ========== MODIFIER GLYPH METHODS ==========

    /**
     * For MODIFIER glyphs: Set of glyph IDs this modifier is compatible with.
     * Empty set means compatible with all glyphs of the appropriate role.
     *
     * @return Set of compatible glyph IDs, or empty for universal compatibility
     */
    default Set<String> getCompatibleGlyphs() {
        return Set.of();
    }

    /**
     * For MODIFIER glyphs: Set of glyph IDs this modifier is incompatible with.
     *
     * @return Set of incompatible glyph IDs
     */
    default Set<String> getIncompatibleGlyphs() {
        return Set.of();
    }

    /**
     * For MODIFIER glyphs: The multiplier applied to effects.
     *
     * @return Modifier multiplier (e.g., 1.5 for +50%)
     */
    default float getModifierMultiplier() {
        return 1.0f;
    }

    /**
     * For MODIFIER glyphs: The mana cost multiplier.
     *
     * @return Cost multiplier (usually same as effect multiplier)
     */
    default float getCostMultiplier() {
        return getModifierMultiplier();
    }

    // ========== SELECT GLYPH METHODS ==========

    /**
     * For SELECT glyphs: Whether this select has travel time (BEAM, PROJECTILE).
     *
     * @return true if execution is delayed until hit
     */
    default boolean isDelayed() {
        return false;
    }

    /**
     * For SELECT glyphs: Select targets based on current execution context.
     *
     * @param ctx Current execution context with caster and origin info
     * @return Set of targets (entities, positions, or both)
     */
    default TargetSet selectTargets(ExecutionContext ctx) {
        return TargetSet.empty();
    }

    // ========== EFFECT GLYPH METHODS ==========

    /**
     * For EFFECT glyphs: Base mana cost before modifiers.
     *
     * @return Base mana cost
     */
    default int getBaseCost() {
        return 0;
    }

    /**
     * For EFFECT glyphs: Apply the effect to targets.
     *
     * @param ctx Execution context with modifiers and caster info
     * @param targets Set of targets to apply effect to
     */
    default void applyEffect(ExecutionContext ctx, TargetSet targets) {
        // Default: no effect
    }

    /**
     * Check if this glyph is compatible with a given modifier.
     *
     * @param modifier The modifier glyph to check
     * @return true if the modifier can wrap this glyph
     */
    default boolean isCompatibleWith(Glyph modifier) {
        if (modifier.getRole() != GlyphRole.MODIFIER) {
            return false;
        }
        Set<String> compatible = modifier.getCompatibleGlyphs();
        Set<String> incompatible = modifier.getIncompatibleGlyphs();

        // If incompatible list contains this glyph, not compatible
        if (incompatible.contains(this.getId())) {
            return false;
        }

        // If compatible list is empty, universal compatibility (minus incompatibles)
        if (compatible.isEmpty()) {
            return true;
        }

        // Otherwise, must be in compatible list
        return compatible.contains(this.getId());
    }
}
