package com.riprod.hexcode.glyph.effects;

import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.GlyphVisual;

import java.util.Set;

/**
 * Fire effect glyph - deals fire damage and applies burn DOT.
 */
public class FireGlyph extends EffectGlyph {
    public static final String ID = "hexcode:fire";
    public static final int BASE_COST = 15;
    public static final float BASE_DAMAGE = 10.0f;
    public static final float BURN_DURATION = 3.0f;
    public static final float BURN_DAMAGE_PER_SECOND = 2.0f;

    public FireGlyph() {
        super(
            ID,
            "Fire",
            BASE_COST,
            GlyphVisual.effect(GlyphVisual.COLOR_FIRE),
            Set.of("hexcode:power", "hexcode:duration")
        );
    }

    @Override
    public void applyEffect(ExecutionContext ctx, TargetSet targets) {
        float damage = getModifiedAmount(ctx, BASE_DAMAGE);
        float burnDuration = getModifiedDuration(ctx, BURN_DURATION);

        // TODO: Implement actual damage and burn effect application
        // For each target entity:
        // 1. Apply instant fire damage
        // 2. Apply burn DOT effect for burnDuration seconds
    }
}
