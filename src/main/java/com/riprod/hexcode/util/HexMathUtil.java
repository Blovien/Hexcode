package com.riprod.hexcode.util;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;

/**
 * Math utilities for Hexcode.
 */
public class HexMathUtil {

    public HexMathUtil() {

    }

    /**
     * Calculate a position on an orbital ring around a center point.
     *
     * @param center The center position
     * @param radius The orbital radius
     * @param angle  The angle in radians (0 = front, PI/2 = right)
     * @param height The height offset from center
     * @return Position on the orbital ring
     */
    public static Vector3d calculateOrbitalPosition(Vector3d center, float radius, float angle, float height) {
        double x = center.x + radius * Math.sin(angle);
        double y = center.y + height;
        double z = center.z + radius * Math.cos(angle);
        return new Vector3d(x, y, z);
    }

    /**
     * Calculate the angle between two positions around a center.
     *
     * @param center   The center point
     * @param position The position to calculate angle for
     * @return Angle in radians
     */
    public static float calculateAngle(Vector3d center, Vector3d position) {
        double dx = position.x - center.x;
        double dz = position.z - center.z;
        return (float) Math.atan2(dx, dz);
    }

    public static Vector3d mul(Vector3d vec, float scalar) {
        return new Vector3d(vec.x * scalar, vec.y * scalar, vec.z * scalar);
    }

    /**
     * Subtract vector b from vector a.
     */
    public static Vector3d sub(Vector3d a, Vector3d b) {
        return new Vector3d(a.x - b.x, a.y - b.y, a.z - b.z);
    }

    /**
     * Get the distance between two points.
     */
    public static double distance(Vector3d a, Vector3d b) {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double dz = b.z - a.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Get the distance squared between two points (faster, no sqrt).
     */
    public static double distanceSquared(Vector3d a, Vector3d b) {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double dz = b.z - a.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Normalize a vector (in place).
     */
    public static Vector3d normalize(Vector3d v) {
        return v.normalize();
    }

    /**
     * Scale a vector by a factor.
     */
    public static Vector3d scale(Vector3d v, double factor) {
        return mul(v, (float) factor);
    }

    /**
     * Calculate direction from one point to another.
     */
    public static Vector3d direction(Vector3d from, Vector3d to) {
        Vector3d dir = sub(to, from);
        return dir.normalize();
    }

    /**
     * Check if a point is within a cone.
     *
     * @param origin       The cone origin
     * @param direction    The cone direction (normalized)
     * @param point        The point to check
     * @param range        The cone range
     * @param angleDegrees The cone angle in degrees
     * @return true if point is within the cone
     */
    public static boolean isInCone(Vector3d origin, Vector3d direction, Vector3d point,
            float range, float angleDegrees) {
        Vector3d toPoint = sub(point, origin);
        double distance = toPoint.length();

        // Check range
        if (distance > range) {
            return false;
        }

        // Check angle
        toPoint.normalize();
        double dot = direction.dot(toPoint);
        double angleRad = Math.acos(dot);
        double maxAngleRad = Math.toRadians(angleDegrees / 2.0);

        return angleRad <= maxAngleRad;
    }

    /**
     * Check if a point is within a sphere.
     *
     * @param center The sphere center
     * @param radius The sphere radius
     * @param point  The point to check
     * @return true if point is within the sphere
     */
    public static boolean isInSphere(Vector3d center, float radius, Vector3d point) {
        return distanceSquared(center, point) <= radius * radius;
    }

    /**
     * Linear interpolation between two values.
     */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * Linear interpolation between two vectors.
     */
    public static Vector3d lerp(Vector3d a, Vector3d b, float t) {
        return new Vector3d(
                lerp((float) a.x, (float) b.x, t),
                lerp((float) a.y, (float) b.y, t),
                lerp((float) a.z, (float) b.z, t));
    }

    /**
     * Clamp a value between min and max.
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Convert Vector3f to Vector3d.
     */
    public static Vector3d toVector3d(Vector3f v) {
        return new Vector3d(v.x, v.y, v.z);
    }

    /**
     * Convert Vector3d to Vector3f.
     */
    public static Vector3f toVector3f(Vector3d v) {
        return new Vector3f((float) v.x, (float) v.y, (float) v.z);
    }
}
