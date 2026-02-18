package com.riprod.hexcode.core.execution.component;

import java.util.UUID;

import com.riprod.hexcode.components.ExecutionContext;

public class PendingContinue {
    private final UUID glyphId;
    private final ExecutionContext executionContext;
    private final int delayTicks;
    private int elapsedTicks;

    public PendingContinue(UUID glyphId, ExecutionContext executionContext, int delayTicks) {
        this.glyphId = glyphId;
        this.executionContext = executionContext;
        this.delayTicks = delayTicks;
        this.elapsedTicks = 0;
    }

    public UUID getGlyphId() {
        return glyphId;
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public boolean isReady() {
        return elapsedTicks >= delayTicks;
    }

    public void tick() {
        elapsedTicks++;
    }
}
