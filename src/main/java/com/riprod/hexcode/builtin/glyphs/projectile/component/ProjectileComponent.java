package com.riprod.hexcode.builtin.glyphs.projectile.component;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ProjectileComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, ProjectileComponent> componentType;

    private Ref<EntityStore> casterRef;
    private double maxDistance;
    private Vector3d spawnPosition;

    public ProjectileComponent() {
    }

    public ProjectileComponent(Ref<EntityStore> casterRef, double maxDistance, Vector3d spawnPosition) {
        this.casterRef = casterRef;
        this.maxDistance = maxDistance;
        this.spawnPosition = spawnPosition;
    }

    public static void setComponentType(ComponentType<EntityStore, ProjectileComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, ProjectileComponent> getComponentType() {
        return componentType;
    }

    public Ref<EntityStore> getCasterRef() {
        return casterRef;
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public Vector3d getSpawnPosition() {
        return spawnPosition;
    }

    @Nonnull
    @Override
    public ProjectileComponent clone() {
        ProjectileComponent copy = new ProjectileComponent();
        copy.casterRef = this.casterRef;
        copy.maxDistance = this.maxDistance;
        copy.spawnPosition = this.spawnPosition != null ? new Vector3d(this.spawnPosition) : null;
        return copy;
    }
}
