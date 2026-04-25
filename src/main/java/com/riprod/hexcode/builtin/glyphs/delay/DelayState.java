package com.riprod.hexcode.builtin.glyphs.delay;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.riprod.hexcode.core.common.construct.state.ConstructState;
import com.riprod.hexcode.core.state.execution.component.HexColors;

public class DelayState implements ConstructState {

    private float remainingSeconds;
    private List<String> nextGlyphIds;
    @Nullable
    private HexColors colors;

    public DelayState() {
        this.nextGlyphIds = new ArrayList<>();
    }

    public DelayState(float remainingSeconds, List<String> nextGlyphIds,
            @Nullable HexColors colors) {
        this.remainingSeconds = remainingSeconds;
        this.nextGlyphIds = nextGlyphIds;
        this.colors = colors;
    }

    public float getRemainingSeconds() {
        return remainingSeconds;
    }

    public void tick(float dt) {
        remainingSeconds -= dt;
    }

    public boolean isExpired() {
        return remainingSeconds <= 0f;
    }

    public List<String> getNextGlyphIds() {
        return nextGlyphIds;
    }

    public void setNextGlyphIds(List<String> ids) {
        this.nextGlyphIds = ids != null ? ids : new ArrayList<>();
    }

    @Nullable
    public HexColors getColors() {
        return colors;
    }

    @Override
    public DelayState copy() {
        return new DelayState(remainingSeconds, new ArrayList<>(nextGlyphIds), colors);
    }
}
