package com.riprod.hexcode.glyph.selects;

import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;

import java.util.Set;

/**
 * Self select glyph - targets the caster only.
 * Instant - no travel time.
 */
public class SelfGlyph extends SelectGlyph {
    public static final String ID = "hexcode:self";

    public SelfGlyph() {
        super(
            ID,
            "Self",
            false, // instant
            Set.of() // No compatible modifiers (no range, speed, etc.)
        );
    }

    @Override
    public TargetSet selectTargets(ExecutionContext ctx) {
        return TargetSet.of(ctx.getCaster());
    }
}
