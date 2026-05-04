package com.riprod.hexcode.builtin.glyphs.scale;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.construct.state.ConstructState;

public class ScaleState implements ConstructState {

    private float appliedMagnitude = 1.0f;
    @Nullable
    private Ref<EntityStore> visualRef;
    private float remainingSeconds;
    @Nullable
    private String modelAssetId;

    public ScaleState() {
    }

    public ScaleState(float appliedMagnitude, @Nullable Ref<EntityStore> visualRef,
            float remainingSeconds, @Nullable String modelAssetId) {
        this.appliedMagnitude = appliedMagnitude;
        this.visualRef = visualRef;
        this.remainingSeconds = remainingSeconds;
        this.modelAssetId = modelAssetId;
    }

    public float getAppliedMagnitude() {
        return appliedMagnitude;
    }

    @Nullable
    public Ref<EntityStore> getVisualRef() {
        return visualRef;
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
        return new ScaleState(appliedMagnitude, visualRef, remainingSeconds, modelAssetId);
    }
}
