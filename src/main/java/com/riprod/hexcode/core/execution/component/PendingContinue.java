package com.riprod.hexcode.core.execution.component;

import java.util.List;
import java.util.UUID;

public class PendingContinue {
    private final List<UUID> glyphIds;
    private final HexContext executionContext;
    private final int delayTicks;
    private int elapsedTicks;

    public PendingContinue(List<UUID> glyphIds, HexContext executionContext, int delayTicks) {
        this.glyphIds = glyphIds;
        this.executionContext = executionContext;
        this.delayTicks = delayTicks;
        this.elapsedTicks = 0;
    }

    public List<UUID> getGlyphIds() {
        return glyphIds;
    }

    public HexContext getExecutionContext() {
        return executionContext;
    }

    public boolean isReady() {
        return elapsedTicks >= delayTicks;
    }

    public void tick() {
        elapsedTicks++;
    }
}
