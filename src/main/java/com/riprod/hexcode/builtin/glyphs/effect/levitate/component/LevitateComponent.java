package com.riprod.hexcode.builtin.glyphs.effect.levitate.component;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;

public class LevitateComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, LevitateComponent> componentType;

    public static ComponentType<EntityStore, LevitateComponent> getComponentType() {
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, LevitateComponent> type) {
        componentType = type;
    }

    private float intensity;
    private float remainingDuration;
    private HexColors colors;

    public LevitateComponent() {
    }

    public LevitateComponent(float intensity, float remainingDuration, HexColors colors) {
        this.intensity = intensity;
        this.remainingDuration = remainingDuration;
        this.colors = colors;
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

    public HexColors getColors() {
        return colors;
    }

    public void setColors(HexColors colors) {
        this.colors = colors;
    }

    public boolean tick(float dt) {
        remainingDuration -= dt;
        return remainingDuration <= 0;
    }

    @Nonnull
    @Override
    public LevitateComponent clone() {
        return new LevitateComponent(intensity, remainingDuration, colors);
    }
}
