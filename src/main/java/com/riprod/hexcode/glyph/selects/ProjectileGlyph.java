package com.riprod.hexcode.glyph.selects;

import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;

import java.util.Set;

/**
 * Projectile select glyph - launches a thrown projectile that executes children on hit.
 * Delayed - has travel time, children execute when projectile hits.
 */
public class ProjectileGlyph extends SelectGlyph {
    public static final String ID = "hexcode:projectile";
    public static final float BASE_RANGE = 40.0f;
    public static final float BASE_SPEED = 20.0f;

    public ProjectileGlyph() {
        super(
            ID,
            "Projectile",
            true, // delayed
            Set.of("hexcode:range", "hexcode:speed", "hexcode:split")
        );
    }

    @Override
    public TargetSet selectTargets(ExecutionContext ctx) {
        // For delayed glyphs, this returns the origin point
        // Actual target resolution happens when projectile hits
        return TargetSet.ofPosition(ctx.getCastOrigin()).withOrigin(ctx.getCastOrigin());
    }

    /**
     * Get the projectile range after modifiers.
     */
    public float getRange(ExecutionContext ctx) {
        return getModifiedRange(ctx, BASE_RANGE);
    }

    /**
     * Get the projectile speed after modifiers.
     */
    public float getSpeed(ExecutionContext ctx) {
        return getModifiedSpeed(ctx, BASE_SPEED);
    }
}
