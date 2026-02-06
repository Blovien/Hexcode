package com.riprod.hexcode.utils;

public class SphericalPosition {
    public final float yaw;
    public final float pitch;
    public final double distance;

    public SphericalPosition(float yaw, float pitch, double distance) {
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

    public SphericalPosition withDistance(double distance) {
        return new SphericalPosition(this.yaw, this.pitch, distance);
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public double getDistance() {
        return distance;
    }
}
