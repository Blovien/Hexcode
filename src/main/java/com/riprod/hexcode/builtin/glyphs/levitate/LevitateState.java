package com.riprod.hexcode.builtin.glyphs.levitate;

import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.riprod.hexcode.core.common.construct.state.ConstructState;
import com.riprod.hexcode.core.state.execution.component.HexColors;

public class LevitateState implements ConstructState {

    private float intensity;
    private float remainingDuration;
    @Nullable
    private HexColors colors;
    @Nullable
    private PhysicsValues originalPhysicsValues;

    public LevitateState() {
    }

    public LevitateState(float intensity, float remainingDuration, @Nullable HexColors colors,
            @Nullable PhysicsValues originalPhysicsValues) {
        this.intensity = intensity;
        this.remainingDuration = remainingDuration;
        this.colors = colors;
        this.originalPhysicsValues = originalPhysicsValues;
    }

    public float getIntensity() {
        return intensity;
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    public float getRemainingDuration() {
        return remainingDuration;
    }

    public void setRemainingDuration(float remainingDuration) {
        this.remainingDuration = remainingDuration;
    }

    @Nullable
    public HexColors getColors() {
        return colors;
    }

    public void setColors(@Nullable HexColors colors) {
        this.colors = colors;
    }

    @Nullable
    public PhysicsValues getOriginalPhysicsValues() {
        return originalPhysicsValues;
    }

    public void tick(float dt) {
        remainingDuration -= dt;
    }

    public boolean isExpired() {
        return remainingDuration <= 0f;
    }

    @Override
    public LevitateState copy() {
        PhysicsValues clonedPhysics = originalPhysicsValues != null
                ? new PhysicsValues(originalPhysicsValues) : null;
        return new LevitateState(intensity, remainingDuration, colors, clonedPhysics);
    }
}
