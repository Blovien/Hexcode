package com.riprod.hexcode.glyph.effects;

import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.GlyphVisual;

import java.util.Set;

/**
 * Lightning effect glyph - deals shock damage and chains to nearby entities.
 */
public class LightningGlyph extends EffectGlyph {
    public static final String ID = "hexcode:lightning";
    public static final int BASE_COST = 15;
    public static final float BASE_DAMAGE = 12.0f;
    public static final int CHAIN_COUNT = 2;
    public static final float CHAIN_RANGE = 5.0f;
    public static final float CHAIN_DAMAGE_FALLOFF = 0.7f; // 70% damage per chain

    public LightningGlyph() {
        super(
            ID,
            "Lightning",
            BASE_COST,
            GlyphVisual.effect(GlyphVisual.COLOR_LIGHTNING),
            Set.of("hexcode:power")
        );
    }

    @Override
    public void applyEffect(ExecutionContext ctx, TargetSet targets) {
        float damage = getModifiedAmount(ctx, BASE_DAMAGE);

        // TODO: Implement actual damage and chain effect application
        // For each target entity:
        // 1. Apply instant shock damage
        // 2. Find nearby entities and chain damage to them
    }
}
