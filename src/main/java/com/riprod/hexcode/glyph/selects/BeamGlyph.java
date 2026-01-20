package com.riprod.hexcode.glyph.selects;

import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;

import java.util.Set;

/**
 * Beam select glyph - fires a raycast projectile that executes children on hit.
 * Delayed - has travel time, children execute when beam hits.
 */
public class BeamGlyph extends SelectGlyph {
    public static final String ID = "hexcode:beam";
    public static final float BASE_RANGE = 30.0f;
    public static final float BASE_SPEED = 50.0f;

    public BeamGlyph() {
        super(
            ID,
            "Beam",
            true, // delayed
            Set.of("hexcode:range", "hexcode:speed", "hexcode:split")
        );
    }

    @Override
    public TargetSet selectTargets(ExecutionContext ctx) {
        // For delayed glyphs, this returns the origin point
        // Actual target resolution happens when beam hits
        return TargetSet.ofPosition(ctx.getCastOrigin()).withOrigin(ctx.getCastOrigin());
    }

    /**
     * Get the beam range after modifiers.
     */
    public float getRange(ExecutionContext ctx) {
        return getModifiedRange(ctx, BASE_RANGE);
    }

    /**
     * Get the beam speed after modifiers.
     */
    public float getSpeed(ExecutionContext ctx) {
        return getModifiedSpeed(ctx, BASE_SPEED);
    }
}
