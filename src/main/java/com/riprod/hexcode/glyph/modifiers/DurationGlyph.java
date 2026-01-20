package com.riprod.hexcode.glyph.modifiers;

import java.util.Set;

/**
 * Duration modifier glyph - extends duration by 50%.
 *
 * Affects DOT/buff duration on effects and travel time on selects.
 */
public class DurationGlyph extends ModifierGlyph {
    public static final String ID = "hexcode:duration";
    public static final float MULTIPLIER = 1.5f;

    public DurationGlyph() {
        super(
            ID,
            "Duration",
            MULTIPLIER,
            // Incompatible with instant effects that have no duration
            Set.of(
                "hexcode:lightning",
                "hexcode:earth",
                "hexcode:push",
                "hexcode:blink"
            )
        );
    }
}
