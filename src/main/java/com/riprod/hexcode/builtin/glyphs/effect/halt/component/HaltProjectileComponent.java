package com.riprod.hexcode.builtin.glyphs.effect.halt.component;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class HaltProjectileComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, HaltProjectileComponent> componentType;

    public static ComponentType<EntityStore, HaltProjectileComponent> getComponentType() {
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, HaltProjectileComponent> type) {
        componentType = type;
    }

    private float remainingDuration;

    public HaltProjectileComponent() {
    }

    public HaltProjectileComponent(float duration) {
        this.remainingDuration = duration;
    }

    public boolean tick(float dt) {
        remainingDuration -= dt;
        return remainingDuration <= 0;
    }

    @Nonnull
    @Override
    public HaltProjectileComponent clone() {
        return new HaltProjectileComponent(remainingDuration);
    }
}
