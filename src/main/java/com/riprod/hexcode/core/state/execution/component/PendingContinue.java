package com.riprod.hexcode.core.state.execution.component;

import java.util.List;

public class PendingContinue {
    private final List<String> glyphIds;
    private final HexContext executionContext;
    private final float delaySeconds;
    private float elapsedSeconds;

    public PendingContinue(List<String> glyphIds, HexContext executionContext, float delaySeconds) {
        this.glyphIds = glyphIds;
        this.executionContext = executionContext;
        this.delaySeconds = delaySeconds;
        this.elapsedSeconds = 0;
    }

    public List<String> getGlyphIds() {
        return glyphIds;
    }

    public HexContext getExecutionContext() {
        return executionContext;
    }

    public boolean isReady() {
        return elapsedSeconds >= delaySeconds;
    }

    public void tick(float dt) {
        elapsedSeconds += dt;
    }
}
