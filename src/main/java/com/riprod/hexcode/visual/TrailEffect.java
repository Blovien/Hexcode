package com.riprod.hexcode.visual;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Rune glow trail particles that follow dragged glyphs.
 */
public class TrailEffect {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final List<TrailPoint> points;
    private final int maxPoints;
    private final float pointLifetime;
    private final int color;
    private boolean active;

    public TrailEffect(int color, int maxPoints, float pointLifetime) {
        this.points = new ArrayList<>();
        this.maxPoints = maxPoints;
        this.pointLifetime = pointLifetime;
        this.color = color;
        this.active = false;
    }

    /**
     * Start the trail effect.
     */
    public void start() {
        active = true;
        points.clear();
        LOGGER.atInfo().log("Starting trail effect");
    }

    /**
     * Stop the trail effect.
     */
    public void stop() {
        active = false;
        LOGGER.atInfo().log("Stopping trail effect");
    }

    /**
     * @return true if the trail is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Add a new point to the trail.
     *
     * @param position The position of the new point
     */
    public void addPoint(Vector3d position) {
        if (!active) {
            return;
        }

        TrailPoint point = new TrailPoint(position, System.currentTimeMillis());
        points.add(point);

        // Remove old points
        while (points.size() > maxPoints) {
            points.remove(0);
        }
    }

    /**
     * Update the trail, removing expired points.
     *
     * @param dt Delta time in seconds
     */
    public void update(float dt) {
        if (!active) {
            return;
        }

        long now = System.currentTimeMillis();
        long expireTime = (long) (pointLifetime * 1000);

        points.removeIf(point -> now - point.createdAt > expireTime);

        // TODO: Update particle positions/alpha based on age
    }

    /**
     * @return List of current trail points
     */
    public List<TrailPoint> getPoints() {
        return new ArrayList<>(points);
    }

    /**
     * @return Trail color
     */
    public int getColor() {
        return color;
    }

    /**
     * Represents a single point in the trail.
     */
    public static class TrailPoint {
        public final Vector3d position;
        public final long createdAt;

        public TrailPoint(Vector3d position, long createdAt) {
            this.position = new Vector3d(position);
            this.createdAt = createdAt;
        }

        /**
         * Get the alpha value based on age.
         */
        public float getAlpha(float lifetime) {
            long age = System.currentTimeMillis() - createdAt;
            float ageSeconds = age / 1000.0f;
            return Math.max(0, 1.0f - (ageSeconds / lifetime));
        }
    }
}
