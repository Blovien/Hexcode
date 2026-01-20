package com.riprod.hexcode.glyph.selects;

import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;

import java.util.Set;

/**
 * Cone select glyph - selects entities in a cone in front of the caster.
 * Instant - no travel time.
 */
public class ConeGlyph extends SelectGlyph {
    public static final String ID = "hexcode:cone";
    public static final float BASE_RANGE = 8.0f;
    public static final float CONE_ANGLE = 45.0f; // degrees

    public ConeGlyph() {
        super(
            ID,
            "Cone",
            false, // instant
            Set.of("hexcode:range")
        );
    }

    @Override
    public TargetSet selectTargets(ExecutionContext ctx) {
        float range = getModifiedRange(ctx, BASE_RANGE);

        // TODO: Implement cone selection
        // 1. Get caster position and look direction
        // 2. Find all entities within range
        // 3. Filter to those within cone angle of look direction
        // 4. Return all matching entities

        return TargetSet.empty();
    }
}
