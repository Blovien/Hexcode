package com.riprod.hexcode.core.common.triggers;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;

public class TriggerListenerComponent {

    private static ComponentType<EntityStore, TriggerListenerComponent> componentType;

    public static ComponentType<EntityStore, TriggerListenerComponent> getComponentType() {
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, TriggerListenerComponent> type) {
        componentType = type;
    }

    private int moveCount;
    private int rotateCount;
    private Vector3d lastPosition;
    private Vector3f lastRotation;

    public TriggerListenerComponent() {
    }

    public int getMoveCount() {
        return moveCount;
    }

    public void setMoveCount(int moveCount) {
        this.moveCount = moveCount;
    }

    public int incrementMove() {
        return ++moveCount;
    }

    public int decrementMove() {
        moveCount = Math.max(0, moveCount - 1);
        return moveCount;
    }

    public int getRotateCount() {
        return rotateCount;
    }

    public void setRotateCount(int rotateCount) {
        this.rotateCount = rotateCount;
    }

    public int incrementRotate() {
        return ++rotateCount;
    }

    public int decrementRotate() {
        rotateCount = Math.max(0, rotateCount - 1);
        return rotateCount;
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

    public boolean hasAnyListener() {
        return moveCount > 0 || rotateCount > 0;
    }

    public static final BuilderCodec<TriggerListenerComponent> CODEC = BuilderCodec
            .builder(TriggerListenerComponent.class, TriggerListenerComponent::new)
            .append(new KeyedCodec<>("MoveCount", Codec.INTEGER),
                    (c, v) -> c.moveCount = v,
                    c -> c.moveCount)
            .add()
            .append(new KeyedCodec<>("RotateCount", Codec.INTEGER),
                    (c, v) -> c.rotateCount = v,
                    c -> c.rotateCount)
            .add()
            .build();
}
