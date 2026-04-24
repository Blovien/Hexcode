package com.riprod.hexcode.builtin.glyphs.scale;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.construct.state.ConstructState;

public class ScaleState implements ConstructState {

    private float originalScale = 1.0f;
    @Nullable
    private Box originalBoundingBox;
    private boolean hadEntityScaleBefore;
    @Nullable
    private Ref<EntityStore> visualRef;

    public ScaleState() {
    }

    public ScaleState(float originalScale, @Nullable Box originalBoundingBox,
            boolean hadEntityScaleBefore, @Nullable Ref<EntityStore> visualRef) {
        this.originalScale = originalScale;
        this.originalBoundingBox = originalBoundingBox;
        this.hadEntityScaleBefore = hadEntityScaleBefore;
        this.visualRef = visualRef;
    }

    public float getOriginalScale() {
        return originalScale;
    }

    @Nullable
    public Box getOriginalBoundingBox() {
        return originalBoundingBox;
    }

    public boolean hadEntityScaleBefore() {
        return hadEntityScaleBefore;
    }

    @Nullable
    public Ref<EntityStore> getVisualRef() {
        return visualRef;
    }

    @Override
    public ScaleState copy() {
        return new ScaleState(originalScale,
                originalBoundingBox != null ? new Box(originalBoundingBox) : null,
                hadEntityScaleBefore, visualRef);
    }
}
