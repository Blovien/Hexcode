package com.riprod.hexcode.builtin.glyphs.scale;

import java.util.UUID;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.construct.state.ConstructState;

public class ScaleState implements ConstructState {

    private UUID constructId;
    private float appliedMagnitude = 1.0f;
    @Nullable
    private Ref<EntityStore> visualRef;
    private float remainingSeconds;
    @Nullable
    private String modelAssetId;

    public ScaleState() {
        this.constructId = UUID.randomUUID();
    }

    public ScaleState(UUID constructId, float appliedMagnitude, @Nullable Ref<EntityStore> visualRef,
            float remainingSeconds, @Nullable String modelAssetId) {
        this.constructId = constructId;
        this.appliedMagnitude = appliedMagnitude;
        this.visualRef = visualRef;
        this.remainingSeconds = remainingSeconds;
        this.modelAssetId = modelAssetId;
    }

    public UUID getConstructId() {
        return constructId;
    }

    public float getAppliedMagnitude() {
        return appliedMagnitude;
    }

    @Nullable
    public Ref<EntityStore> getVisualRef() {
        return visualRef;
    }

    public void setVisualRef(@Nullable Ref<EntityStore> visualRef) {
        this.visualRef = visualRef;
    }

    public void setAppliedMagnitude(float magnitude) {
        this.appliedMagnitude = magnitude;
    }

    public void setModelAssetId(@Nullable String assetId) {
        this.modelAssetId = assetId;
    }

    public void setRemainingSeconds(float seconds) {
        this.remainingSeconds = seconds;
    }

    @Nullable
    public String getModelAssetId() {
        return modelAssetId;
    }

    public void tick(float dt) {
        remainingSeconds -= dt;
    }

    public boolean isExpired() {
        return remainingSeconds <= 0f;
    }

    @Override
    public ScaleState copy() {
        return new ScaleState(constructId, appliedMagnitude, visualRef, remainingSeconds, modelAssetId);
    }
}
