package com.riprod.hexcode.builtin.glyphs.freeze;

import java.util.ArrayList;
import java.util.List;

import com.riprod.hexcode.builtin.glyphs.freeze.component.FrozenBlock;
import com.riprod.hexcode.core.common.construct.state.ConstructState;

public class FreezeState implements ConstructState {

    private List<FrozenBlock> frozenBlocks;
    private float durationSeconds;
    private float elapsedSeconds;

    public FreezeState() {
        this.frozenBlocks = new ArrayList<>();
    }

    public FreezeState(List<FrozenBlock> frozenBlocks, float durationSeconds) {
        this.frozenBlocks = frozenBlocks;
        this.durationSeconds = durationSeconds;
        this.elapsedSeconds = 0f;
    }

    public List<FrozenBlock> getFrozenBlocks() {
        return frozenBlocks;
    }

    public float getDurationSeconds() {
        return durationSeconds;
    }

    public float getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void tick(float dt) {
        elapsedSeconds += dt;
    }

    public boolean isExpired() {
        return elapsedSeconds >= durationSeconds;
    }

    @Override
    public FreezeState copy() {
        FreezeState c = new FreezeState(new ArrayList<>(frozenBlocks), durationSeconds);
        c.elapsedSeconds = this.elapsedSeconds;
        return c;
    }
}
