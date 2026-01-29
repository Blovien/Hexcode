package com.riprod.hexcode.entity;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * ECS Component for entities that orbit around a target entity (typically the player).
 *
 * <p>This component is used by the OrbitalTrackingSystem to position entities in
 * spherical coordinates (yaw/pitch) relative to a target entity. Unlike MountedComponent,
 * this provides direct control over positioning and supports pausing during drag operations.
 *
 * <p>Position calculation:
 * <pre>
 * worldPosition = targetPosition + direction(yaw, pitch) * distance
 * </pre>
 *
 * <p>Used for root HexNodeEntity positioning. Child nodes still use MountedComponent
 * for internal hierarchy.
 */
public class OrbitalPositionComponent implements Component<EntityStore> {
    private static ComponentType<EntityStore, OrbitalPositionComponent> componentType;

    /**
     * CODEC for serialization - required for ECS component registration.
     */
    public static final BuilderCodec<OrbitalPositionComponent> CODEC =
        BuilderCodec.builder(OrbitalPositionComponent.class, OrbitalPositionComponent::new)
            .append(
                new KeyedCodec<>("Yaw", Codec.FLOAT),
                (c, v) -> c.yaw = v,
                c -> c.yaw
            )
            .add()
            .append(
                new KeyedCodec<>("Pitch", Codec.FLOAT),
                (c, v) -> c.pitch = v,
                c -> c.pitch
            )
            .add()
            .append(
                new KeyedCodec<>("Distance", Codec.FLOAT),
                (c, v) -> c.distance = v,
                c -> c.distance
            )
            .add()
            .append(
                new KeyedCodec<>("Paused", Codec.BOOLEAN),
                (c, v) -> c.paused = v,
                c -> c.paused
            )
            .add()
            .build();

    /** Reference to the target entity to orbit around (typically the player) */
    private transient Ref<EntityStore> targetRef;

    /** Horizontal angle in degrees (-180 to 180) */
    private float yaw;

    /** Vertical angle in degrees (-90 to 90) */
    private float pitch;

    /** Distance from target in blocks */
    private float distance;

    /** When true, the OrbitalTrackingSystem skips this entity (used during dragging) */
    private boolean paused;

    /**
     * Default constructor required for ECS component registration.
     */
    public OrbitalPositionComponent() {
        this.targetRef = null;
        this.yaw = 0.0f;
        this.pitch = 0.0f;
        this.distance = 2.0f;
        this.paused = false;
    }

    /**
     * Create a new OrbitalPositionComponent.
     *
     * @param targetRef Reference to the entity to orbit around
     * @param yaw       Horizontal angle in degrees (-180 to 180)
     * @param pitch     Vertical angle in degrees (-90 to 90)
     * @param distance  Distance from target in blocks
     */
    public OrbitalPositionComponent(Ref<EntityStore> targetRef, float yaw, float pitch, float distance) {
        this.targetRef = targetRef;
        this.yaw = yaw;
        this.pitch = pitch;
        this.distance = distance;
        this.paused = false;
    }

    // ========== GETTERS ==========

    public Ref<EntityStore> getTargetRef() {
        return targetRef;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public float getDistance() {
        return distance;
    }

    public boolean isPaused() {
        return paused;
    }

    // ========== SETTERS ==========

    public void setTargetRef(Ref<EntityStore> targetRef) {
        this.targetRef = targetRef;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    /**
     * Update both yaw and pitch at once.
     *
     * @param yaw   New horizontal angle
     * @param pitch New vertical angle
     */
    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    // ========== STATIC ==========

    /**
     * Get the component type for registration.
     */
    public static ComponentType<EntityStore, OrbitalPositionComponent> getComponentType() {
        return componentType;
    }

    /**
     * Set the component type (called during plugin initialization).
     */
    public static void setComponentType(ComponentType<EntityStore, OrbitalPositionComponent> type) {
        componentType = type;
    }

    // ========== COMPONENT INTERFACE ==========

    @Override
    public Component<EntityStore> clone() {
        OrbitalPositionComponent copy = new OrbitalPositionComponent(targetRef, yaw, pitch, distance);
        copy.paused = this.paused;
        return copy;
    }

    @Override
    public String toString() {
        return String.format("OrbitalPositionComponent[yaw=%.1f, pitch=%.1f, dist=%.1f, paused=%s]",
                yaw, pitch, distance, paused);
    }
}
