package com.riprod.hexcode.glyph.modifiers;

import java.util.Set;

/**
 * Power modifier glyph - amplifies damage/healing intensity by 50%.
 */
public class PowerGlyph extends ModifierGlyph {
    public static final String ID = "hexcode:power";
    public static final float MULTIPLIER = 1.5f;

    public PowerGlyph() {
        super(
            ID,
            "Power",
            MULTIPLIER,
            Set.of() // Compatible with all glyphs that accept it
        );
    }
}
