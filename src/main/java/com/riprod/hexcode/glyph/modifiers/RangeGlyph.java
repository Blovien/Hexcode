package com.riprod.hexcode.glyph.modifiers;

import java.util.Set;

/**
 * Range modifier glyph - extends distance/radius by 50%.
 *
 * Incompatible with most EFFECT glyphs (they don't have range).
 * Compatible with SELECT glyphs that have range (BEAM, PROJECTILE, BURST, etc.)
 */
public class RangeGlyph extends ModifierGlyph {
    public static final String ID = "hexcode:range";
    public static final float MULTIPLIER = 1.5f;

    public RangeGlyph() {
        super(
            ID,
            "Range",
            MULTIPLIER,
            // Incompatible with most effect glyphs
            Set.of(
                "hexcode:fire",
                "hexcode:ice",
                "hexcode:lightning",
                "hexcode:earth",
                "hexcode:void",
                "hexcode:light",
                "hexcode:shield",
                "hexcode:heal",
                "hexcode:push",
                "hexcode:self" // SELF has no range
            )
        );
    }
}
