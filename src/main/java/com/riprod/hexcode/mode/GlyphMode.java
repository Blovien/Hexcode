package com.riprod.hexcode.mode;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.loadout.Loadout;
import com.hypixel.hytale.math.vector.Vector3d;

import java.util.List;

/**
 * Represents a player's glyph mode state.
 *
 * When a player enters glyph mode:
 * - Stamina drains continuously
 * - Movement is restricted/slowed
 * - Glyphs from loadout orbit around them
 * - Player can compose hexes by dragging glyphs
 */
public class GlyphMode {
    private final Ref<EntityStore> player;
    private final Loadout loadout;
    private final CompositionState composition;

    private boolean active;
    private long modeEnteredAt;
    private float staminaDrainRate;
    private float movementSpeedMultiplier;

    // Orbital ring configuration
    private float orbitalRadius;
    private float orbitSpeed;

    // Crafting space configuration
    private float craftingSpaceDistance;

    // Currently hovered/dragging glyph
    private Glyph hoveredGlyph;
    private Glyph draggingGlyph;
    private Vector3d dragPosition;

    public GlyphMode(Ref<EntityStore> player, Loadout loadout) {
        this.player = player;
        this.loadout = loadout;
        this.composition = new CompositionState();
        this.active = false;
        this.modeEnteredAt = 0;

        // Default configuration
        this.staminaDrainRate = 5.0f;
        this.movementSpeedMultiplier = 0.5f;
        this.orbitalRadius = 2.5f;
        this.orbitSpeed = 0.3f;
        this.craftingSpaceDistance = 2.0f;
    }

    // ========== STATE ==========

    /**
     * @return The player this mode belongs to
     */
    public Ref<EntityStore> getPlayer() {
        return player;
    }

    /**
     * @return true if glyph mode is currently active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Enter glyph mode.
     */
    public void enter() {
        if (!active) {
            active = true;
            modeEnteredAt = System.currentTimeMillis();
        }
    }

    /**
     * Exit glyph mode.
     */
    public void exit() {
        if (active) {
            active = false;
            hoveredGlyph = null;
            draggingGlyph = null;
            dragPosition = null;
        }
    }

    /**
     * @return Time in milliseconds since mode was entered
     */
    public long getTimeInMode() {
        if (!active) {
            return 0;
        }
        return System.currentTimeMillis() - modeEnteredAt;
    }

    /**
     * @return Time in seconds since mode was entered
     */
    public float getTimeInModeSeconds() {
        return getTimeInMode() / 1000.0f;
    }

    // ========== LOADOUT ==========

    /**
     * @return The player's loadout
     */
    public Loadout getLoadout() {
        return loadout;
    }

    /**
     * @return Glyphs available in the loadout
     */
    public List<Glyph> getAvailableGlyphs() {
        return loadout.getGlyphs();
    }

    // ========== COMPOSITION ==========

    /**
     * @return The current composition state
     */
    public CompositionState getComposition() {
        return composition;
    }

    /**
     * Clear the current composition.
     */
    public void clearComposition() {
        composition.clear();
    }

    // ========== INTERACTION ==========

    /**
     * @return The glyph currently being hovered
     */
    public Glyph getHoveredGlyph() {
        return hoveredGlyph;
    }

    /**
     * Set the hovered glyph.
     */
    public void setHoveredGlyph(Glyph glyph) {
        this.hoveredGlyph = glyph;
    }

    /**
     * @return The glyph currently being dragged
     */
    public Glyph getDraggingGlyph() {
        return draggingGlyph;
    }

    /**
     * @return true if currently dragging a glyph
     */
    public boolean isDragging() {
        return draggingGlyph != null;
    }

    /**
     * Start dragging a glyph.
     */
    public void startDrag(Glyph glyph, Vector3d startPosition) {
        this.draggingGlyph = glyph;
        this.dragPosition = startPosition;
    }

    /**
     * Update drag position.
     */
    public void updateDrag(Vector3d position) {
        this.dragPosition = position;
    }

    /**
     * End drag.
     */
    public void endDrag() {
        this.draggingGlyph = null;
        this.dragPosition = null;
    }

    /**
     * @return Current drag position
     */
    public Vector3d getDragPosition() {
        return dragPosition;
    }

    // ========== CONFIGURATION ==========

    public float getStaminaDrainRate() {
        return staminaDrainRate;
    }

    public void setStaminaDrainRate(float rate) {
        this.staminaDrainRate = rate;
    }

    public float getMovementSpeedMultiplier() {
        return movementSpeedMultiplier;
    }

    public void setMovementSpeedMultiplier(float multiplier) {
        this.movementSpeedMultiplier = multiplier;
    }

    public float getOrbitalRadius() {
        return orbitalRadius;
    }

    public void setOrbitalRadius(float radius) {
        this.orbitalRadius = radius;
    }

    public float getOrbitSpeed() {
        return orbitSpeed;
    }

    public void setOrbitSpeed(float speed) {
        this.orbitSpeed = speed;
    }

    public float getCraftingSpaceDistance() {
        return craftingSpaceDistance;
    }

    public void setCraftingSpaceDistance(float distance) {
        this.craftingSpaceDistance = distance;
    }
}
