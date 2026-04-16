package com.riprod.hexcode.builtin.glyphs.effect.halt.component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
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
    @Nullable
    private PhysicsValues originalPhysicsValues;

    public HaltProjectileComponent() {
    }

    public HaltProjectileComponent(float duration, @Nullable PhysicsValues originalPhysicsValues) {
        this.remainingDuration = duration;
        this.originalPhysicsValues = originalPhysicsValues;
    }

    public boolean tick(float dt) {
        remainingDuration -= dt;
        return remainingDuration <= 0;
    }

    public float getRemainingDuration() {
        return remainingDuration;
    }

    @Nullable
    public PhysicsValues getOriginalPhysicsValues() {
        return originalPhysicsValues;
    }

    @Nonnull
    @Override
    public HaltProjectileComponent clone() {
        PhysicsValues cloned = originalPhysicsValues != null
                ? new PhysicsValues(originalPhysicsValues) : null;
        return new HaltProjectileComponent(remainingDuration, cloned);
    }
}
