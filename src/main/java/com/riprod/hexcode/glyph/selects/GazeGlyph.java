package com.riprod.hexcode.glyph.selects;

import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;

import java.util.Set;

/**
 * Gaze select glyph - targets first entity in line of sight.
 * Instant - no travel time, but longer range than Touch.
 */
public class GazeGlyph extends SelectGlyph {
    public static final String ID = "hexcode:gaze";
    public static final float BASE_RANGE = 50.0f;

    public GazeGlyph() {
        super(
            ID,
            "Gaze",
            false, // instant
            Set.of("hexcode:range")
        );
    }

    @Override
    public TargetSet selectTargets(ExecutionContext ctx) {
        float range = getModifiedRange(ctx, BASE_RANGE);

        // TODO: Implement raycast to find first entity in line of sight
        // 1. Get caster position and look direction
        // 2. Raycast from eye position in look direction
        // 3. Return first entity hit within range

        return TargetSet.empty();
    }
}
