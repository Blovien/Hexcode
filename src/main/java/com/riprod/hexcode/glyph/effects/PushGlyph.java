package com.riprod.hexcode.glyph.effects;

import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.GlyphVisual;

import java.util.Set;

/**
 * Push effect glyph - applies knockback without damage.
 */
public class PushGlyph extends EffectGlyph {
    public static final String ID = "hexcode:push";
    public static final int BASE_COST = 10;
    public static final float BASE_KNOCKBACK = 10.0f;

    public PushGlyph() {
        super(
            ID,
            "Push",
            BASE_COST,
            GlyphVisual.effect(GlyphVisual.COLOR_UTILITY),
            Set.of("hexcode:power")
        );
    }

    @Override
    public void applyEffect(ExecutionContext ctx, TargetSet targets) {
        float knockback = getModifiedAmount(ctx, BASE_KNOCKBACK);

        // TODO: Implement knockback effect (no damage)
        // For each target entity:
        // 1. Calculate push direction (away from origin)
        // 2. Apply knockback velocity
    }
}
