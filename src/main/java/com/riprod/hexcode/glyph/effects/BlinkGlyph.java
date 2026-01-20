package com.riprod.hexcode.glyph.effects;

import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.GlyphVisual;

import java.util.Set;

/**
 * Blink effect glyph - teleports target a short distance.
 */
public class BlinkGlyph extends EffectGlyph {
    public static final String ID = "hexcode:blink";
    public static final int BASE_COST = 25;
    public static final float BASE_DISTANCE = 8.0f;

    public BlinkGlyph() {
        super(
            ID,
            "Blink",
            BASE_COST,
            GlyphVisual.effect(GlyphVisual.COLOR_UTILITY),
            Set.of("hexcode:range")
        );
    }

    @Override
    public void applyEffect(ExecutionContext ctx, TargetSet targets) {
        float distance = ctx.calculateModifiedRange(BASE_DISTANCE);

        // TODO: Implement teleport effect
        // For each target entity:
        // 1. Calculate teleport destination (forward in look direction)
        // 2. Validate destination is safe
        // 3. Teleport entity
    }
}
