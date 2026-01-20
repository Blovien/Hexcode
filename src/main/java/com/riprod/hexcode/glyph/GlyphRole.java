package com.riprod.hexcode.glyph;

/**
 * Defines the role of a glyph in a Hex tree structure.
 *
 * - EFFECT: Leaf nodes that perform actions (FIRE, ICE, HEAL)
 * - MODIFIER: Inner shells that amplify/alter wrapped glyphs (POWER, RANGE)
 * - SELECT: Outer shells that determine targeting/delivery (BEAM, BURST)
 */
public enum GlyphRole {
    /**
     * Effect glyphs are always leaves - they cannot contain other glyphs.
     * They perform the actual spell action (damage, healing, utility).
     */
    EFFECT,

    /**
     * Modifier glyphs wrap exactly one glyph and only affect that direct child.
     * They amplify or alter the behavior of their child.
     */
    MODIFIER,

    /**
     * Select glyphs wrap one glyph OR a linked chain of siblings.
     * They determine how targets are selected (self, beam, burst, etc.).
     */
    SELECT
}
