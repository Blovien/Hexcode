package com.riprod.hexcode.glyph.modifiers;

import java.util.Set;

/**
 * Speed modifier glyph - increases projectile/beam velocity by 50%.
 *
 * Only compatible with SELECT glyphs that have travel time.
 */
public class SpeedGlyph extends ModifierGlyph {
    public static final String ID = "hexcode:speed";
    public static final float MULTIPLIER = 1.5f;

    public SpeedGlyph() {
        super(
            ID,
            "Speed",
            MULTIPLIER,
            // Incompatible with all effects and instant selects
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
