package com.riprod.hexcode.glyph.effects;

import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.GlyphVisual;

import java.util.Set;

/**
 * Void effect glyph - deals void damage and applies brief blindness.
 */
public class VoidGlyph extends EffectGlyph {
    public static final String ID = "hexcode:void";
    public static final int BASE_COST = 15;
    public static final float BASE_DAMAGE = 11.0f;
    public static final float BLIND_DURATION = 2.0f;

    public VoidGlyph() {
        super(
            ID,
            "Void",
            BASE_COST,
            GlyphVisual.effect(GlyphVisual.COLOR_VOID),
            Set.of("hexcode:power", "hexcode:duration")
        );
    }

    @Override
    public void applyEffect(ExecutionContext ctx, TargetSet targets) {
        float damage = getModifiedAmount(ctx, BASE_DAMAGE);
        float blindDuration = getModifiedDuration(ctx, BLIND_DURATION);

        // TODO: Implement actual damage and blindness application
        // For each target entity:
        // 1. Apply instant void damage
        // 2. Apply blindness effect for blindDuration seconds
    }
}
