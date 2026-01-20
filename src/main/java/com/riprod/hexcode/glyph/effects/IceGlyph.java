package com.riprod.hexcode.glyph.effects;

import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.GlyphVisual;

import java.util.Set;

/**
 * Ice effect glyph - deals cold damage and applies slow.
 */
public class IceGlyph extends EffectGlyph {
    public static final String ID = "hexcode:ice";
    public static final int BASE_COST = 15;
    public static final float BASE_DAMAGE = 8.0f;
    public static final float SLOW_DURATION = 4.0f;
    public static final float SLOW_AMOUNT = 0.5f; // 50% speed reduction

    public IceGlyph() {
        super(
            ID,
            "Ice",
            BASE_COST,
            GlyphVisual.effect(GlyphVisual.COLOR_ICE),
            Set.of("hexcode:power", "hexcode:duration")
        );
    }

    @Override
    public void applyEffect(ExecutionContext ctx, TargetSet targets) {
        float damage = getModifiedAmount(ctx, BASE_DAMAGE);
        float slowDuration = getModifiedDuration(ctx, SLOW_DURATION);

        // TODO: Implement actual damage and slow effect application
        // For each target entity:
        // 1. Apply instant cold damage
        // 2. Apply slow effect for slowDuration seconds
    }
}
