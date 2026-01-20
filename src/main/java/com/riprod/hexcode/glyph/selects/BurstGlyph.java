package com.riprod.hexcode.glyph.selects;

import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;

import java.util.Set;

/**
 * Burst select glyph - selects all entities in a radius around current target/origin.
 * Instant - no travel time.
 */
public class BurstGlyph extends SelectGlyph {
    public static final String ID = "hexcode:burst";
    public static final float BASE_RADIUS = 5.0f;

    public BurstGlyph() {
        super(
            ID,
            "Burst",
            false, // instant
            Set.of("hexcode:range")
        );
    }

    @Override
    public TargetSet selectTargets(ExecutionContext ctx) {
        float radius = getModifiedRange(ctx, BASE_RADIUS);

        // TODO: Implement area selection
        // 1. Get current origin (from parent SELECT hit point or cast origin)
        // 2. Find all entities within radius of origin
        // 3. Return all found entities

        return TargetSet.empty();
    }
}
