package com.riprod.hexcode.glyph.selects;

import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;

import java.util.Set;

/**
 * Touch select glyph - targets entity in melee range (3 blocks).
 * Instant - no travel time.
 */
public class TouchGlyph extends SelectGlyph {
    public static final String ID = "hexcode:touch";
    public static final float BASE_RANGE = 3.0f;

    public TouchGlyph() {
        super(
            ID,
            "Touch",
            false, // instant
            Set.of("hexcode:range")
        );
    }

    @Override
    public TargetSet selectTargets(ExecutionContext ctx) {
        float range = getModifiedRange(ctx, BASE_RANGE);

        // TODO: Implement raycast to find entity in range
        // 1. Get caster position and look direction
        // 2. Raycast from eye position in look direction
        // 3. Return first entity hit within range

        return TargetSet.empty();
    }
}
