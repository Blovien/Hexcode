package com.riprod.hexcode.builtin.glyphs.erode;

import com.riprod.hexcode.core.common.construct.state.ConstructState;

public class ErodeState implements ConstructState {

    private float vulnerabilityMultiplier;
    private float remainingDuration;

    public ErodeState() {
    }

    public ErodeState(float vulnerabilityMultiplier, float remainingDuration) {
        this.vulnerabilityMultiplier = vulnerabilityMultiplier;
        this.remainingDuration = remainingDuration;
    }

    public float getVulnerabilityMultiplier() {
        return vulnerabilityMultiplier;
    }

    public void setVulnerabilityMultiplier(float vulnerabilityMultiplier) {
        this.vulnerabilityMultiplier = vulnerabilityMultiplier;
    }

    public float getRemainingDuration() {
        return remainingDuration;
    }

    public void setRemainingDuration(float remainingDuration) {
        this.remainingDuration = remainingDuration;
    }

    public void tick(float dt) {
        remainingDuration -= dt;
    }

    public boolean isExpired() {
        return remainingDuration <= 0f;
    }

    @Override
    public ErodeState copy() {
        return new ErodeState(vulnerabilityMultiplier, remainingDuration);
    }
}
