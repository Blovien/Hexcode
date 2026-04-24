package com.riprod.hexcode.builtin.glyphs.shatter.component;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ShatterComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, ShatterComponent> componentType;

    private Ref<EntityStore> casterRef;
    private double maxDistance;
    private Vector3d spawnPosition;

    public ShatterComponent() {
    }

    public ShatterComponent(Ref<EntityStore> casterRef, double maxDistance, Vector3d spawnPosition) {
        this.casterRef = casterRef;
        this.maxDistance = maxDistance;
        this.spawnPosition = spawnPosition;
    }

    public static void setComponentType(ComponentType<EntityStore, ShatterComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, ShatterComponent> getComponentType() {
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
    public ShatterComponent clone() {
        ShatterComponent copy = new ShatterComponent();
        copy.casterRef = this.casterRef;
        copy.maxDistance = this.maxDistance;
        copy.spawnPosition = this.spawnPosition != null ? new Vector3d(this.spawnPosition) : null;
        return copy;
    }
}
