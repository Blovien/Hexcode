package com.riprod.hexcode.glyph.effects;

import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.GlyphVisual;

import java.util.Set;

/**
 * Light effect glyph - creates a light source at target location.
 */
public class LightGlyph extends EffectGlyph {
    public static final String ID = "hexcode:light";
    public static final int BASE_COST = 10;
    public static final float LIGHT_RADIUS = 10.0f;
    public static final float LIGHT_INTENSITY = 15.0f;
    public static final float LIGHT_DURATION = 60.0f; // 1 minute

    public LightGlyph() {
        super(
            ID,
            "Light",
            BASE_COST,
            GlyphVisual.effect(GlyphVisual.COLOR_UTILITY),
            Set.of("hexcode:power", "hexcode:duration")
        );
    }

    @Override
    public void applyEffect(ExecutionContext ctx, TargetSet targets) {
        float intensity = getModifiedAmount(ctx, LIGHT_INTENSITY);
        float duration = getModifiedDuration(ctx, LIGHT_DURATION);

        // TODO: Implement light source creation
        // For each target position:
        // 1. Spawn a light entity at the position
        // 2. Set duration for despawn
    }
}
