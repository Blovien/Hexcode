package com.riprod.hexcode.glyph.effects;

import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.GlyphVisual;

import java.util.Set;

/**
 * Heal effect glyph - restores health to target.
 */
public class HealGlyph extends EffectGlyph {
    public static final String ID = "hexcode:heal";
    public static final int BASE_COST = 20;
    public static final float BASE_HEALING = 15.0f;

    public HealGlyph() {
        super(
            ID,
            "Heal",
            BASE_COST,
            GlyphVisual.effect(GlyphVisual.COLOR_UTILITY),
            Set.of("hexcode:power")
        );
    }

    @Override
    public void applyEffect(ExecutionContext ctx, TargetSet targets) {
        float healing = getModifiedAmount(ctx, BASE_HEALING);

        // TODO: Implement healing effect
        // For each target entity:
        // 1. Get health component
        // 2. Add healing amount (capped at max health)
    }
}
