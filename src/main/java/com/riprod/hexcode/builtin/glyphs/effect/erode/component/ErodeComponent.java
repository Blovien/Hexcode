package com.riprod.hexcode.builtin.glyphs.effect.erode.component;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ErodeComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, ErodeComponent> componentType;

    public static ComponentType<EntityStore, ErodeComponent> getComponentType() {
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, ErodeComponent> type) {
        componentType = type;
    }

    private float vulnerabilityMultiplier;
    private float remainingDuration;

    public ErodeComponent() {
    }

    public ErodeComponent(float vulnerabilityMultiplier, float remainingDuration) {
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

    public boolean tick(float dt) {
        remainingDuration -= dt;
        return remainingDuration <= 0;
    }

    @Nonnull
    @Override
    public ErodeComponent clone() {
        return new ErodeComponent(vulnerabilityMultiplier, remainingDuration);
    }
}
