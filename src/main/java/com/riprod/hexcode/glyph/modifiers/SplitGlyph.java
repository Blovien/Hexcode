package com.riprod.hexcode.glyph.modifiers;

import java.util.Set;

/**
 * Split modifier glyph - causes the wrapped SELECT to split into 3.
 *
 * Only compatible with delayed SELECT glyphs (BEAM, PROJECTILE).
 */
public class SplitGlyph extends ModifierGlyph {
    public static final String ID = "hexcode:split";
    public static final float MULTIPLIER = 3.0f; // Used to signal split count

    public SplitGlyph() {
        super(
            ID,
            "Split",
            MULTIPLIER,
            // Incompatible with all effects and most selects
            Set.of(
                "hexcode:fire",
                "hexcode:ice",
                "hexcode:lightning",
                "hexcode:earth",
                "hexcode:void",
                "hexcode:light",
                "hexcode:shield",
                "hexcode:blink",
                "hexcode:heal",
                "hexcode:push",
                "hexcode:self",
                "hexcode:touch",
                "hexcode:gaze",
                "hexcode:burst",
                "hexcode:cone"
            )
        );
    }
}
