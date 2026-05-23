package com.riprod.hexcode.core.common.triggers.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import org.joml.Vector3d;
import org.joml.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class TriggerListenerComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, TriggerListenerComponent> componentType;

    public static ComponentType<EntityStore, TriggerListenerComponent> getComponentType() {
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, TriggerListenerComponent> type) {
        componentType = type;
    }

    // last sampled position/rotation; movement tick system compares against
    // these to detect deltas and fire ON_MOVE / ON_ROTATE events. fields are
    // transient because they're per-runtime tracking, not save state.
    private transient Vector3d lastPosition;
    private transient Vector3f lastRotation;

    public TriggerListenerComponent() {
    }

    public Vector3d getLastPosition() {
        return lastPosition;
    }

    public void setLastPosition(Vector3d lastPosition) {
        this.lastPosition = lastPosition;
    }

    public Vector3f getLastRotation() {
        return lastRotation;
    }

    public void setLastRotation(Vector3f lastRotation) {
        this.lastRotation = lastRotation;
    }

    @Override
    public TriggerListenerComponent clone() {
        TriggerListenerComponent copy = new TriggerListenerComponent();
        copy.lastPosition = this.lastPosition != null ? new Vector3d(this.lastPosition) : null;
        copy.lastRotation = this.lastRotation != null ? new Vector3f(this.lastRotation) : null;
        return copy;
    }

    // pure marker — no persisted state. registered without a codec.
}
