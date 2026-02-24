package com.riprod.hexcode.utils;

import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;

public class SphericalPosition {
    public final float yaw;
    public final float pitch;
    public final float distance;

    public SphericalPosition(float yaw, float pitch, float distance) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.distance = distance;
    }

    public static SphericalPosition zero() {
        return new SphericalPosition(0f, 0f, 0f);
    }

    public static SphericalPosition forward(float distance) {
        return new SphericalPosition(0f, 0f, distance);
    }

    public SphericalPosition withYaw(float yaw) {
        return new SphericalPosition(yaw, this.pitch, this.distance);
    }

    public SphericalPosition withPitch(float pitch) {
        return new SphericalPosition(this.yaw, pitch, this.distance);
    }

    public SphericalPosition withDistance(float distance) {
        return new SphericalPosition(this.yaw, this.pitch, distance);
    }

    public static SphericalPosition fromTransform(TransformComponent transform) {
        return new SphericalPosition(transform.getRotation().getYaw(), transform.getRotation().getPitch(), 0);
    }

    public static SphericalPosition fromHeadRotation(HeadRotation headRotation) {
        return new SphericalPosition(headRotation.getRotation().getYaw(), headRotation.getRotation().getPitch(), 0);
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
}
