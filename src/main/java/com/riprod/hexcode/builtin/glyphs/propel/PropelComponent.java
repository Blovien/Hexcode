package com.riprod.hexcode.builtin.glyphs.propel;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.components.ExecutionContext;

public class PropelComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, PropelComponent> componentType;

    private Ref<EntityStore> hexEntityRef;
    private ExecutionContext executionContext;
    private int outputSlot;
    private Ref<EntityStore> casterRef;
    private double maxDistance;
    private Vector3d spawnPosition;

    public PropelComponent() {
    }

    public PropelComponent(Ref<EntityStore> hexEntityRef, ExecutionContext executionContext,
            int outputSlot, Ref<EntityStore> casterRef, double maxDistance, Vector3d spawnPosition) {
        this.hexEntityRef = hexEntityRef;
        this.executionContext = executionContext;
        this.outputSlot = outputSlot;
        this.casterRef = casterRef;
        this.maxDistance = maxDistance;
        this.spawnPosition = spawnPosition;
    }

    public static void setComponentType(ComponentType<EntityStore, PropelComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, PropelComponent> getComponentType() {
        return componentType;
    }

    public Ref<EntityStore> getHexEntityRef() {
        return hexEntityRef;
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public int getOutputSlot() {
        return outputSlot;
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
    public PropelComponent clone() {
        PropelComponent copy = new PropelComponent();
        copy.hexEntityRef = this.hexEntityRef;
        copy.executionContext = this.executionContext != null ? this.executionContext.copy() : null;
        copy.outputSlot = this.outputSlot;
        copy.casterRef = this.casterRef;
        copy.maxDistance = this.maxDistance;
        copy.spawnPosition = this.spawnPosition != null ? new Vector3d(this.spawnPosition) : null;
        return copy;
    }
}
