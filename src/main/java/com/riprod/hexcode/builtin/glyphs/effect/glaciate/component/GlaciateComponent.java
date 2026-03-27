package com.riprod.hexcode.builtin.glyphs.effect.glaciate.component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class GlaciateComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, GlaciateComponent> componentType;

    private float lifetime;
    private float damageRadius;
    private float damageMultiplier;
    private Set<UUID> hitEntities;
    private List<String> firstBranchIds;
    private boolean firedFirstBranch;

    public GlaciateComponent() {
    }

    public GlaciateComponent(float lifetime, float damageRadius, float damageMultiplier,
            List<String> firstBranchIds) {
        this.lifetime = lifetime;
        this.damageRadius = damageRadius;
        this.damageMultiplier = damageMultiplier;
        this.hitEntities = new HashSet<>();
        this.firstBranchIds = firstBranchIds;
        this.firedFirstBranch = false;
    }

    public static void setComponentType(ComponentType<EntityStore, GlaciateComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, GlaciateComponent> getComponentType() {
        return componentType;
    }

    public float getLifetime() {
        return lifetime;
    }

    public void decrementLifetime(float dt) {
        this.lifetime -= dt;
    }

    public float getDamageRadius() {
        return damageRadius;
    }

    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    public Set<UUID> getHitEntities() {
        if (hitEntities == null) hitEntities = new HashSet<>();
        return hitEntities;
    }

    public List<String> getFirstBranchIds() {
        return firstBranchIds;
    }

    public boolean firedFirstBranch() {
        return firedFirstBranch;
    }

    public void markFirstBranchFired() {
        this.firedFirstBranch = true;
    }

    @Nonnull
    @Override
    public GlaciateComponent clone() {
        GlaciateComponent copy = new GlaciateComponent();
        copy.lifetime = this.lifetime;
        copy.damageRadius = this.damageRadius;
        copy.damageMultiplier = this.damageMultiplier;
        copy.hitEntities = this.hitEntities != null ? new HashSet<>(this.hitEntities) : new HashSet<>();
        copy.firstBranchIds = this.firstBranchIds;
        copy.firedFirstBranch = this.firedFirstBranch;
        return copy;
    }
}
