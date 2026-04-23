package com.riprod.hexcode.builtin.glyphs.scale.component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ScaleComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, ScaleComponent> componentType;

    public static ComponentType<EntityStore, ScaleComponent> getComponentType() {
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, ScaleComponent> type) {
        componentType = type;
    }

    private Ref<EntityStore> targetRef;
    private float originalScale = 1.0f;
    @Nullable
    private Box originalBoundingBox;
    private boolean hadEntityScaleBefore;

    public ScaleComponent() {
    }

    public ScaleComponent(Ref<EntityStore> targetRef, float originalScale,
            @Nullable Box originalBoundingBox, boolean hadEntityScaleBefore) {
        this.targetRef = targetRef;
        this.originalScale = originalScale;
        this.originalBoundingBox = originalBoundingBox;
        this.hadEntityScaleBefore = hadEntityScaleBefore;
    }

    public Ref<EntityStore> getTargetRef() {
        return targetRef;
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

    @Nonnull
    @Override
    public ScaleComponent clone() {
        return new ScaleComponent(targetRef, originalScale,
                originalBoundingBox != null ? new Box(originalBoundingBox) : null,
                hadEntityScaleBefore);
    }
}
