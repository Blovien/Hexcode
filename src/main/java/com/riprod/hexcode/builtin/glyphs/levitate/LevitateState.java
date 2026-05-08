package com.riprod.hexcode.builtin.glyphs.levitate;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.riprod.hexcode.core.common.construct.state.ConstructState;
import com.riprod.hexcode.core.state.execution.component.HexColors;

public class LevitateState implements ConstructState {

    private float intensity;
    private float remainingDuration;
    private float tickAccum;
    @Nullable
    private HexColors colors;
    @Nullable
    private PhysicsValues originalPhysicsValues;
    private List<String> nextGlyphIds;

    public LevitateState() {
        this.nextGlyphIds = new ArrayList<>();
    }

    public LevitateState(float intensity, float remainingDuration, @Nullable HexColors colors,
            @Nullable PhysicsValues originalPhysicsValues, List<String> nextGlyphIds) {
        this.intensity = intensity;
        this.remainingDuration = remainingDuration;
        this.colors = colors;
        this.originalPhysicsValues = originalPhysicsValues;
        this.nextGlyphIds = nextGlyphIds != null ? nextGlyphIds : new ArrayList<>();
    }

    public float getTickAccum() {
        return tickAccum;
    }

    public void setTickAccum(float tickAccum) {
        this.tickAccum = tickAccum;
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

    public List<String> getNextGlyphIds() {
        return nextGlyphIds;
    }

    public void setNextGlyphIds(List<String> ids) {
        this.nextGlyphIds = ids != null ? ids : new ArrayList<>();
    }

    @Override
    public LevitateState copy() {
        PhysicsValues clonedPhysics = originalPhysicsValues != null
                ? new PhysicsValues(originalPhysicsValues) : null;
        LevitateState c = new LevitateState(intensity, remainingDuration, colors, clonedPhysics,
                new ArrayList<>(nextGlyphIds));
        c.tickAccum = this.tickAccum;
        return c;
    }
}
