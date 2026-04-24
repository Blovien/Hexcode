package com.riprod.hexcode.builtin.glyphs.fortify;

import com.riprod.hexcode.core.common.construct.state.ConstructState;

public class FortifyState implements ConstructState {

    private float damageReduction;
    private float remainingDuration;

    public FortifyState() {
    }

    public FortifyState(float damageReduction, float remainingDuration) {
        this.damageReduction = damageReduction;
        this.remainingDuration = remainingDuration;
    }

    public float getDamageReduction() {
        return damageReduction;
    }

    public void setDamageReduction(float damageReduction) {
        this.damageReduction = damageReduction;
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
    public FortifyState copy() {
        return new FortifyState(damageReduction, remainingDuration);
    }
}
