package com.riprod.hexcode.builtin.glyphs.halt;

import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.riprod.hexcode.core.common.construct.state.ConstructState;

public class HaltState implements ConstructState {

    private float remainingDuration;
    @Nullable
    private PhysicsValues originalPhysicsValues;

    public HaltState() {
    }

    public HaltState(float remainingDuration, @Nullable PhysicsValues originalPhysicsValues) {
        this.remainingDuration = remainingDuration;
        this.originalPhysicsValues = originalPhysicsValues;
    }

    public float getRemainingDuration() {
        return remainingDuration;
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
    public HaltState copy() {
        PhysicsValues cloned = originalPhysicsValues != null
                ? new PhysicsValues(originalPhysicsValues) : null;
        return new HaltState(remainingDuration, cloned);
    }
}
