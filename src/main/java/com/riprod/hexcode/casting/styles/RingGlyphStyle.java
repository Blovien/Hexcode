package com.riprod.hexcode.casting.styles;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Ring-style orbital positioning.
 *
 * Elements are arranged in a horizontal circle around the player,
 * rotating at a configurable speed. Elements can be excluded from
 * the rotation (e.g., when being dragged) and smoothly reintegrated.
 */
public class RingGlyphStyle extends BaseGlyphStyle {

    /** Radius of the orbital ring from the player */
    private float radius;

    /** Height offset above the player's position */
    private float height;

    /** Whether to maintain fixed element spacing when elements are excluded */
    private boolean maintainSpacing;

    /** Default radius */
    public static final float DEFAULT_RADIUS = 2.0f;

    /** Default height (chest level) */
    public static final float DEFAULT_HEIGHT = 1.0f;

    /** Default rotation speed (radians per second) */
    public static final float DEFAULT_ROTATION_SPEED = 0.3f;

    /** Assumed ticks per second for dt conversion (Hytale typically runs at 20 TPS) */
    private static final float TICKS_PER_SECOND = 20.0f;

    public RingGlyphStyle() {
        super();
        this.radius = DEFAULT_RADIUS;
        this.height = DEFAULT_HEIGHT;
        this.rotationSpeed = DEFAULT_ROTATION_SPEED;
        this.maintainSpacing = true;
    }

    /**
     * Create a ring style with custom parameters.
     *
     * @param radius        Ring radius from player
     * @param height        Height offset above player
     * @param rotationSpeed Rotation speed in radians per second
     */
    public RingGlyphStyle(float radius, float height, float rotationSpeed) {
        super();
        this.radius = radius;
        this.height = height;
        this.rotationSpeed = rotationSpeed;
        this.maintainSpacing = true;
    }

    @Override
    public void tick(Store<EntityStore> store, float dt) {
        // Convert dt to seconds if it's raw tick count (dt=1 per tick)
        // Hytale ECS typically provides dt as tick count, not seconds
        float dtSeconds = dt / TICKS_PER_SECOND;

        // Update the global rotation angle (rotationSpeed is in radians per second)
        rotationAngle += rotationSpeed * dtSeconds;

        // Keep angle in range [0, 2π)
        if (rotationAngle >= 2.0 * Math.PI) {
            rotationAngle -= (float) (2.0 * Math.PI);
        }

        // Update positions for all active elements
        int activeIndex = 0;
        int activeCount = getActiveElementCount();

        if (activeCount == 0) {
            return;
        }

        float angleStep = (float) (2.0 * Math.PI / activeCount);

        for (OrbitalElement element : elements) {
            if (!excludedElements.contains(element)) {
                // Calculate this element's angle
                float elementAngle = rotationAngle + (angleStep * activeIndex);
                element.setOrbitAngle(elementAngle);

                // Calculate and set position
                Vector3d position = calculateRingPosition(elementAngle);
                element.updateWorldPositionDirect(store, position);

                activeIndex++;
            }
            // Excluded elements keep their current position (dragging handles their position)
        }
    }

    @Override
    public Vector3d getPositionForElement(int index, int total) {
        if (total == 0) {
            return new Vector3d(centerPosition.x, centerPosition.y + height, centerPosition.z);
        }

        float angleStep = (float) (2.0 * Math.PI / total);
        float elementAngle = rotationAngle + (angleStep * index);

        return calculateRingPosition(elementAngle);
    }

    @Override
    public void resetElement(OrbitalElement element, Store<EntityStore> store) {
        // Include the element back in the orbit
        includeInOrbit(element);

        // Clear dragging state
        element.setDragging(false);

        // The next tick will position it correctly
    }

    /**
     * Calculate the world position for a given angle on the ring.
     *
     * @param angle The angle in radians
     * @return The world position
     */
    private Vector3d calculateRingPosition(float angle) {
        double x = centerPosition.x + radius * Math.cos(angle);
        double y = centerPosition.y + height;
        double z = centerPosition.z + radius * Math.sin(angle);

        return new Vector3d(x, y, z);
    }

    // --- Configuration ---

    /**
     * @return The ring radius
     */
    public float getRadius() {
        return radius;
    }

    /**
     * Set the ring radius.
     *
     * @param radius New radius
     */
    public void setRadius(float radius) {
        this.radius = radius;
    }

    /**
     * @return The height offset
     */
    public float getHeight() {
        return height;
    }

    /**
     * Set the height offset.
     *
     * @param height New height offset
     */
    public void setHeight(float height) {
        this.height = height;
    }

    /**
     * @return Whether spacing is maintained when elements are excluded
     */
    public boolean isMaintainSpacing() {
        return maintainSpacing;
    }

    /**
     * Set whether to maintain fixed spacing when elements are excluded.
     * If true, remaining elements spread to fill gaps.
     * If false, elements keep their absolute angles.
     *
     * @param maintainSpacing true to maintain even spacing
     */
    public void setMaintainSpacing(boolean maintainSpacing) {
        this.maintainSpacing = maintainSpacing;
    }

    /**
     * Start dragging an element. Removes it from orbital rotation.
     *
     * @param element The element being dragged
     */
    public void startDrag(OrbitalElement element) {
        element.setDragging(true);
        excludeFromOrbit(element);
    }

    /**
     * End dragging an element. If no valid drop target, returns to orbit.
     *
     * @param element The element that was being dragged
     * @param store   The entity store
     * @param dropped true if dropped on a valid target, false to return to orbit
     */
    public void endDrag(OrbitalElement element, Store<EntityStore> store, boolean dropped) {
        element.setDragging(false);

        if (!dropped) {
            // Return to orbit
            resetElement(element, store);
        }
        // If dropped, the element stays excluded (it's now part of a composition)
    }

    /**
     * Update the position of a dragged element.
     *
     * @param element  The element being dragged
     * @param position The new drag position
     * @param store    The entity store
     */
    public void updateDragPosition(OrbitalElement element, Vector3d position, Store<EntityStore> store) {
        if (element.isDragging()) {
            element.updateWorldPositionDirect(store, position);
        }
    }
}
