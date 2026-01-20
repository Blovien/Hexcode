package com.riprod.hexcode.glyph.effects;

import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.GlyphVisual;

import java.util.Set;

/**
 * Shield effect glyph - applies damage absorption buff.
 */
public class ShieldGlyph extends EffectGlyph {
    public static final String ID = "hexcode:shield";
    public static final int BASE_COST = 20;
    public static final float BASE_ABSORPTION = 20.0f;
    public static final float SHIELD_DURATION = 10.0f;

    public ShieldGlyph() {
        super(
            ID,
            "Shield",
            BASE_COST,
            GlyphVisual.effect(GlyphVisual.COLOR_UTILITY),
            Set.of("hexcode:power", "hexcode:duration")
        );
    }

    @Override
    public void applyEffect(ExecutionContext ctx, TargetSet targets) {
        float absorption = getModifiedAmount(ctx, BASE_ABSORPTION);
        float duration = getModifiedDuration(ctx, SHIELD_DURATION);

        // TODO: Implement shield buff application
        // For each target entity:
        // 1. Apply damage absorption effect
        // 2. Set duration for buff expiry
    }
}
