package com.riprod.hexcode.glyph.effects;

import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.GlyphVisual;

import java.util.Set;

/**
 * Earth effect glyph - deals physical damage with knockback.
 */
public class EarthGlyph extends EffectGlyph {
    public static final String ID = "hexcode:earth";
    public static final int BASE_COST = 15;
    public static final float BASE_DAMAGE = 15.0f;
    public static final float KNOCKBACK_STRENGTH = 8.0f;

    public EarthGlyph() {
        super(
            ID,
            "Earth",
            BASE_COST,
            GlyphVisual.effect(GlyphVisual.COLOR_EARTH),
            Set.of("hexcode:power")
        );
    }

    @Override
    public void applyEffect(ExecutionContext ctx, TargetSet targets) {
        float damage = getModifiedAmount(ctx, BASE_DAMAGE);
        float knockback = getModifiedAmount(ctx, KNOCKBACK_STRENGTH);

        // TODO: Implement actual damage and knockback application
        // For each target entity:
        // 1. Apply instant physical damage
        // 2. Apply knockback away from caster
    }
}
