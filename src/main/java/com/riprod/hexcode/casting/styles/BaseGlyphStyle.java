package com.riprod.hexcode.casting.styles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Abstract base class for glyph orbital styles.
 *
 * Styles define how orbital elements (glyphs and hexes) are positioned and
 * animated around a player. Different styles can create different visual
 * arrangements (ring, grid, spiral, etc).
 *
 * Implementations should:
 * - Override tick() to update element positions
 * - Override getPositionForElement() to calculate positions
 * - Override resetElement() to animate elements back to orbit
 */
public abstract class BaseGlyphStyle {

    /** The center position (player position) */
    protected Vector3d centerPosition;

    /** All orbital elements managed by this style */
    protected final List<OrbitalElement> elements;

    /** Elements currently excluded from orbital updates (being dragged or placed) */
    protected final Set<OrbitalElement> excludedElements;

    /** Current rotation angle for the entire style */
    protected float rotationAngle;

    /** Rotation speed in radians per second */
    protected float rotationSpeed;

    public BaseGlyphStyle() {
        this.elements = new ArrayList<>();
        this.excludedElements = new HashSet<>();
        this.centerPosition = new Vector3d(0, 0, 0);
        this.rotationAngle = 0.0f;
        this.rotationSpeed = 0.5f; // Default rotation speed
    }

    /**
     * Update the orbital style. Called every tick.
     *
     * @param store The entity store
     * @param dt    Delta time since last tick
     */
    public abstract void tick(Store<EntityStore> store, float dt);

    /**
     * Calculate the position for an element at a given index.
     *
     * @param index The element's index in the orbital
     * @param total Total number of elements in orbit
     * @return The calculated world position
     */
    public abstract Vector3d getPositionForElement(int index, int total);

    /**
     * Reset an element back to its orbital position.
     * Called when a drag is cancelled or an element should return to orbit.
     *
     * @param element The element to reset
     * @param store   The entity store
     */
    public abstract void resetElement(OrbitalElement element, Store<EntityStore> store);

    public abstract void startDrag(OrbitalElement element);

    // --- Element Management ---

    /**
     * Add an element to this style's orbital.
     *
     * @param element The element to add
     */
    public void addElement(OrbitalElement element) {
        if (!elements.contains(element)) {
            elements.add(element);
            recalculateAngles();
        }
    }

    /**
     * Remove an element from this style's orbital.
     *
     * @param element The element to remove
     */
    public void removeElement(OrbitalElement element) {
        elements.remove(element);
        excludedElements.remove(element);
        recalculateAngles();
    }

    /**
     * Clear all elements from this style.
     */
    public void clearElements() {
        elements.clear();
        excludedElements.clear();
    }

    /**
     * @return All elements in this style
     */
    public List<OrbitalElement> getElements() {
        return elements;
    }

    /**
     * @return Number of elements in this style
     */
    public int getElementCount() {
        return elements.size();
    }

    /**
     * Get the number of elements currently in orbit (not excluded).
     *
     * @return Number of elements in active orbit
     */
    public int getActiveElementCount() {
        return elements.size() - excludedElements.size();
    }

    // --- Exclusion (Dragging/Placing) ---

    /**
     * Exclude an element from orbital updates.
     * Used when dragging or after placing an element.
     *
     * @param element The element to exclude
     */
    public void excludeFromOrbit(OrbitalElement element) {
        excludedElements.add(element);
        element.setExcludedFromOrbit(true);
        recalculateAngles();
    }

    /**
     * Include an element back in orbital updates.
     *
     * @param element The element to include
     */
    public void includeInOrbit(OrbitalElement element) {
        excludedElements.remove(element);
        element.setExcludedFromOrbit(false);
        recalculateAngles();
    }

    /**
     * Check if an element is currently in orbit (not excluded).
     *
     * @param element The element to check
     * @return true if the element is in orbital rotation
     */
    public boolean isElementInOrbit(OrbitalElement element) {
        return elements.contains(element) && !excludedElements.contains(element);
    }

    // --- Center Position ---

    /**
     * Set the center position (player position).
     *
     * @param position The new center position
     */
    public void setCenter(Vector3d position) {
        this.centerPosition = position;
    }

    /**
     * @return The current center position
     */
    public Vector3d getCenter() {
        return centerPosition;
    }

    // --- Rotation ---

    /**
     * @return Current rotation angle in radians
     */
    public float getRotationAngle() {
        return rotationAngle;
    }

    /**
     * Set the rotation angle.
     *
     * @param angle Angle in radians
     */
    public void setRotationAngle(float angle) {
        this.rotationAngle = angle;
    }

    /**
     * @return Rotation speed in radians per second
     */
    public float getRotationSpeed() {
        return rotationSpeed;
    }

    /**
     * Set the rotation speed.
     *
     * @param speed Speed in radians per second
     */
    public void setRotationSpeed(float speed) {
        this.rotationSpeed = speed;
    }

    // --- Helper Methods ---

    /**
     * Recalculate element angles after adding/removing/excluding elements.
     * Override in subclasses for custom angle distribution.
     */
    protected void recalculateAngles() {
        // Default implementation: evenly distribute active elements
        int activeCount = getActiveElementCount();
        if (activeCount == 0) {
            return;
        }

        float angleStep = (float) (2.0 * Math.PI / activeCount);
        int activeIndex = 0;

        for (OrbitalElement element : elements) {
            if (!excludedElements.contains(element)) {
                element.setOrbitAngle(rotationAngle + (angleStep * activeIndex));
                activeIndex++;
            }
        }
    }

    /**
     * Update positions of all active (non-excluded) elements.
     *
     * @param store The entity store
     */
    protected void updateActiveElementPositions(Store<EntityStore> store) {
        int activeIndex = 0;
        int activeCount = getActiveElementCount();

        for (OrbitalElement element : elements) {
            if (!excludedElements.contains(element)) {
                Vector3d position = getPositionForElement(activeIndex, activeCount);
                element.updateWorldPosition(store, centerPosition);
                activeIndex++;
            }
        }
    }

    /**
     * Find the element at a given index in the active (non-excluded) elements.
     *
     * @param activeIndex Index within active elements
     * @return The element, or null if not found
     */
    protected OrbitalElement getActiveElementAt(int activeIndex) {
        int currentIndex = 0;
        for (OrbitalElement element : elements) {
            if (!excludedElements.contains(element)) {
                if (currentIndex == activeIndex) {
                    return element;
                }
                currentIndex++;
            }
        }
        return null;
    }
}
