package com.riprod.hexcode.casting.styles;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.math.GlyphRotation;

/**
 * Abstract base class for glyph styles.
 *
 * Styles define how orbital elements (glyphs and hexes) are initially spawned
 * around a player. After spawning, elements are positioned relative to the player
 * and managed by the elements themselves, not the style.
 *
 * Responsibilities:
 * - Calculate spawn positions for new elements
 * - Manage visual effects (magic wheel, particles, AOE indicators)
 * - Handle mode enter/exit lifecycle events
 *
 * Elements manage their own:
 * - Player-relative positioning (following the player)
 * - Facing the player
 * - Drag state and position capture
 */
public abstract class BaseGlyphStyle {

    /** All orbital elements managed by this style */
    protected final List<OrbitalElement> elements;

    public BaseGlyphStyle() {
        this.elements = new ArrayList<>();
    }

    // --- Spawn Rotation Calculation ---

    /**
     * Calculate the spawn rotation for a new element.
     *
     * <p>Subclasses should override this method to provide rotation-based positioning.
     * The default implementation converts from the deprecated getSpawnPosition().
     *
     * @param index Element index (0-based)
     * @param total Total number of elements being spawned
     * @return GlyphRotation for where element should spawn
     */
    public GlyphRotation getSpawnRotation(int index, int total) {
        // Default implementation: convert from position (requires playerPosition)
        // Subclasses should override this for proper rotation-based positioning
        return new GlyphRotation(-9.0f, (360.0f / Math.max(1, total)) * index);
    }

    /**
     * Calculate the spawn position for a new element.
     *
     * @param index          Element index (0-based)
     * @param total          Total number of elements being spawned
     * @param playerPosition Player's current position
     * @return World position where element should spawn
     * @deprecated Use {@link #getSpawnRotation(int, int)} instead.
     *             Rotation-based positioning replaces world position calculation.
     */
    @Deprecated
    public abstract Vector3d getSpawnPosition(int index, int total, Vector3d playerPosition);

    // --- Mode Lifecycle ---

    /**
     * Called when glyph mode is entered.
     * Spawn visual assets (magic wheel, particles, AOE indicators, etc).
     *
     * @param commandBuffer  The command buffer for entity operations
     * @param playerPosition The player's position
     */
    public abstract void onModeEnter(CommandBuffer<EntityStore> commandBuffer, Vector3d playerPosition);

    /**
     * Called when glyph mode is exited.
     * Despawn visual assets.
     *
     * @param commandBuffer The command buffer for entity operations
     */
    public abstract void onModeExit(CommandBuffer<EntityStore> commandBuffer);

    // --- Visual Effects ---

    /**
     * Update visual effects each tick (particles, AOE indicators).
     *
     * @param store    The entity store
     * @param elements All orbital elements (for particle attachment)
     * @param dt       Delta time since last tick
     */
    public abstract void updateEffects(Store<EntityStore> store, List<OrbitalElement> elements, float dt);

    // --- Element Management ---

    /**
     * Add an element to this style.
     *
     * @param element The element to add
     */
    public void addElement(OrbitalElement element) {
        if (!elements.contains(element)) {
            elements.add(element);
        }
    }

    /**
     * Remove an element from this style.
     *
     * @param element The element to remove
     */
    public void removeElement(OrbitalElement element) {
        elements.remove(element);
    }

    /**
     * Clear all elements from this style.
     */
    public void clearElements() {
        elements.clear();
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
}
