package com.riprod.hexcode.builtin.glyphs.effect.glaciate.component;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class GlaciateComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, GlaciateComponent> componentType;

    private GlaciateState state;
    private float persistDuration;
    private float persistTimer;
    private float damageRadius;
    private float baseDamage;

    public GlaciateComponent() {
    }

    public GlaciateComponent(float persistDuration, float damageRadius, float baseDamage) {
        this.state = GlaciateState.FALLING;
        this.persistDuration = persistDuration;
        this.persistTimer = persistDuration;
        this.damageRadius = damageRadius;
        this.baseDamage = baseDamage;
    }

    public static void setComponentType(ComponentType<EntityStore, GlaciateComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, GlaciateComponent> getComponentType() {
        return componentType;
    }

    public GlaciateState getState() {
        return state;
    }

    public void setState(GlaciateState state) {
        this.state = state;
    }

    public float getPersistDuration() {
        return persistDuration;
    }

    public float getPersistTimer() {
        return persistTimer;
    }

    public void decrementPersistTimer(float dt) {
        this.persistTimer -= dt;
    }

    public float getDamageRadius() {
        return damageRadius;
    }

    public float getBaseDamage() {
        return baseDamage;
    }

    @Nonnull
    @Override
    public GlaciateComponent clone() {
        GlaciateComponent copy = new GlaciateComponent();
        copy.state = this.state;
        copy.persistDuration = this.persistDuration;
        copy.persistTimer = this.persistTimer;
        copy.damageRadius = this.damageRadius;
        copy.baseDamage = this.baseDamage;
        return copy;
    }
}
