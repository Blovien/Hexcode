package com.riprod.hexcode.core.state.execution.component;

import java.util.List;
import java.util.UUID;

public class PendingContinue {
    private final List<String> glyphIds;
    private final HexContext executionContext;
    private final int delayTicks;
    private int elapsedTicks;

    public PendingContinue(List<String> glyphIds, HexContext executionContext, int delayTicks) {
        this.glyphIds = glyphIds;
        this.executionContext = executionContext;
        this.delayTicks = delayTicks;
        this.elapsedTicks = 0;
    }

    public List<String> getGlyphIds() {
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
