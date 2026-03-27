package com.riprod.hexcode.builtin.glyphs.effect.levitate.component;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
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
    private PhysicsValues originalPhysicsValues;

    public LevitateComponent() {
    }

    public LevitateComponent(float intensity, float remainingDuration, HexColors colors,
            PhysicsValues originalPhysicsValues) {
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

    public HexColors getColors() {
        return colors;
    }

    public void setColors(HexColors colors) {
        this.colors = colors;
    }

    public PhysicsValues getOriginalPhysicsValues() {
        return originalPhysicsValues;
    }

    public void setOriginalPhysicsValues(PhysicsValues originalPhysicsValues) {
        this.originalPhysicsValues = originalPhysicsValues;
    }

    public boolean tick(float dt) {
        remainingDuration -= dt;
        return remainingDuration <= 0;
    }

    @Nonnull
    @Override
    public LevitateComponent clone() {
        PhysicsValues clonedPhysics = originalPhysicsValues != null
                ? new PhysicsValues(originalPhysicsValues) : null;
        return new LevitateComponent(intensity, remainingDuration, colors, clonedPhysics);
    }
}
