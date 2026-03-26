package com.riprod.hexcode.builtin.glyphs.effect.fortify.component;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class FortifyComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, FortifyComponent> componentType;

    public static ComponentType<EntityStore, FortifyComponent> getComponentType() {
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, FortifyComponent> type) {
        componentType = type;
    }

    private float damageReduction;
    private float remainingDuration;

    public FortifyComponent() {
    }

    public FortifyComponent(float damageReduction, float remainingDuration) {
        this.damageReduction = damageReduction;
        this.remainingDuration = remainingDuration;
    }

    public float getDamageReduction() {
        return damageReduction;
    }

    public void setDamageReduction(float damageReduction) {
        this.damageReduction = damageReduction;
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
    public FortifyComponent clone() {
        return new FortifyComponent(damageReduction, remainingDuration);
    }
}
