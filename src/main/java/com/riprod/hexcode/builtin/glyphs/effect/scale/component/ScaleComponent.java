package com.riprod.hexcode.builtin.glyphs.effect.scale.component;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
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

    private float originalScale = 1.0f;
    private Box originalBoundingBox;
    private float remainingDuration;

    public ScaleComponent() {
    }

    public ScaleComponent(float originalScale, Box originalBoundingBox, float remainingDuration) {
        this.originalScale = originalScale;
        this.originalBoundingBox = originalBoundingBox;
        this.remainingDuration = remainingDuration;
    }

    public float getOriginalScale() {
        return originalScale;
    }

    public Box getOriginalBoundingBox() {
        return originalBoundingBox;
    }

    public float getRemainingDuration() {
        return remainingDuration;
    }

    public boolean tick(float dt) {
        remainingDuration -= dt;
        return remainingDuration <= 0;
    }

    @Nonnull
    @Override
    public ScaleComponent clone() {
        return new ScaleComponent(originalScale,
                originalBoundingBox != null ? new Box(originalBoundingBox) : null,
                remainingDuration);
    }
}
