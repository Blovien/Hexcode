package com.riprod.hexcode.mode;

import com.riprod.hexcode.glyph.GlyphRole;
import javax.annotation.Nonnull;

/**
 * Interface for elements that can be placed in the orbital ring
 * and used in hex composition.
 *
 * Both individual Glyphs and SavedHexes implement this interface.
 *
 * @see com.riprod.hexcode.glyph.Glyph
 * @see SavedHexElement
 */
public interface CastableElement {

    /**
     * Get unique identifier for this element.
     * For glyphs: "hexcode:fire"
     * For saved hexes: "saved:FireBall"
     */
    @Nonnull
    String getId();

    /**
     * Get display name shown in UI.
     */
    @Nonnull
    String getDisplayName();

    /**
     * Check if this is a saved hex (vs individual glyph).
     */
    boolean isSavedHex();

    /**
     * Get the role of this element for composition rules.
     * Glyphs return their actual role (EFFECT, MODIFIER, SELECT).
     * SavedHexes always return SELECT (they are complete spells).
     */
    @Nonnull
    GlyphRole getRole();

    /**
     * Get base mana cost for casting this element.
     */
    float getBaseCost();

    /**
     * Get the visual color for this element (RGB int).
     */
    int getColor();
}
