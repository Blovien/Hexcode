package com.riprod.hexcode.mode;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.entity.OrbitalGlyphEntity;
import com.riprod.hexcode.execution.HexExecutor;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.loadout.Loadout;
import com.riprod.hexcode.util.RaycastUtil;

import java.util.ArrayList;
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
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

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

    // Orbital glyph entities
    private List<OrbitalGlyphEntity> orbitalEntities;
    private OrbitalGlyphEntity hoveredOrbitalEntity;

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

        // Initialize orbital entities list
        this.orbitalEntities = new ArrayList<>();
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
     * Enter glyph mode and spawn orbital glyphs.
     */
    public void enter() {
        if (!active) {
            active = true;
            modeEnteredAt = System.currentTimeMillis();
            LOGGER.atInfo().log("Glyph mode entered");
        }
    }

    /**
     * Enter glyph mode with entity spawning.
     *
     * @param store The entity store for spawning orbital entities
     */
    public void enter(Store<EntityStore> store) {
        if (!active) {
            active = true;
            modeEnteredAt = System.currentTimeMillis();
            spawnOrbitalGlyphs(store);
            LOGGER.atInfo().log("Glyph mode entered with %d orbital glyphs", orbitalEntities.size());
        }
    }

    /**
     * Exit glyph mode and despawn orbital glyphs.
     */
    public void exit() {
        if (active) {
            active = false;
            hoveredGlyph = null;
            draggingGlyph = null;
            dragPosition = null;
            hoveredOrbitalEntity = null;
            LOGGER.atInfo().log("Glyph mode exited");
        }
    }

    /**
     * Exit glyph mode with entity cleanup.
     *
     * @param store The entity store for despawning orbital entities
     */
    public void exit(Store<EntityStore> store) {
        if (active) {
            despawnOrbitalGlyphs(store);
            active = false;
            hoveredGlyph = null;
            draggingGlyph = null;
            dragPosition = null;
            hoveredOrbitalEntity = null;
            LOGGER.atInfo().log("Glyph mode exited, despawned orbital glyphs");
        }
    }

    /**
     * Spawn orbital glyph entities for all glyphs in the loadout.
     */
    private void spawnOrbitalGlyphs(Store<EntityStore> store) {
        // Clear any existing entities
        orbitalEntities.clear();

        // Get player position
        Vector3d playerPosition = getPlayerPosition(store);

        List<Glyph> glyphs = loadout.getGlyphs();
        int glyphCount = glyphs.size();
        float angleStep = (float) (2 * Math.PI / Math.max(1, glyphCount));

        for (int i = 0; i < glyphCount; i++) {
            Glyph glyph = glyphs.get(i);
            float initialAngle = angleStep * i;

            OrbitalGlyphEntity orbitalEntity = new OrbitalGlyphEntity(glyph, player, initialAngle);
            orbitalEntity.spawn(store, playerPosition);
            orbitalEntities.add(orbitalEntity);
        }

        LOGGER.atInfo().log("Spawned %d orbital glyph entities", orbitalEntities.size());
    }

    /**
     * Despawn all orbital glyph entities.
     */
    private void despawnOrbitalGlyphs(Store<EntityStore> store) {
        for (OrbitalGlyphEntity entity : orbitalEntities) {
            entity.despawn(store);
        }
        orbitalEntities.clear();
        LOGGER.atInfo().log("Despawned all orbital glyph entities");
    }

    /**
     * Get the player's current position.
     */
    private Vector3d getPlayerPosition(Store<EntityStore> store) {
        TransformComponent transform = store.getComponent(player, TransformComponent.getComponentType());
        if (transform != null) {
            return new Vector3d(transform.getPosition());
        }
        return new Vector3d(0, 0, 0);
    }

    /**
     * Update all orbital glyphs (called each tick).
     *
     * @param store The entity store
     * @param dt    Delta time in seconds
     */
    public void updateOrbitalGlyphs(Store<EntityStore> store, float dt) {
        if (!active || orbitalEntities.isEmpty()) {
            return;
        }

        Vector3d playerPosition = getPlayerPosition(store);

        for (OrbitalGlyphEntity entity : orbitalEntities) {
            // Update orbital angle
            entity.update(dt);

            // Update world position
            entity.updateWorldPosition(store, playerPosition);
        }
    }

    /**
     * Get the list of orbital entities.
     */
    public List<OrbitalGlyphEntity> getOrbitalEntities() {
        return orbitalEntities;
    }

    /**
     * Set the hovered orbital entity.
     */
    public void setHoveredOrbitalEntity(OrbitalGlyphEntity entity, Store<EntityStore> store) {
        // Clear previous hover
        if (hoveredOrbitalEntity != null && hoveredOrbitalEntity != entity) {
            hoveredOrbitalEntity.setHovered(false);
            hoveredOrbitalEntity.updateHoverVisual(store);
        }

        hoveredOrbitalEntity = entity;

        if (entity != null) {
            entity.setHovered(true);
            entity.updateHoverVisual(store);
            hoveredGlyph = entity.getGlyph();
        } else {
            hoveredGlyph = null;
        }
    }

    /**
     * Get the hovered orbital entity.
     */
    public OrbitalGlyphEntity getHoveredOrbitalEntity() {
        return hoveredOrbitalEntity;
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

    // ========== CASTING ==========

    /**
     * Force cast the current composition (ignores mana cost).
     *
     * @param store     The entity store
     * @param casterRef Reference to the caster entity
     */
    public void forceCast(Store<EntityStore> store, Ref<EntityStore> casterRef) {
        if (composition.isEmpty()) {
            LOGGER.atWarning().log("Cannot force cast - composition is empty");
            return;
        }

        Hex hex = composition.getHex();
        if (!hex.isValid()) {
            LOGGER.atWarning().log("Cannot force cast - hex is not valid");
            return;
        }

        TransformComponent transform = store.getComponent(casterRef, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }
        Vector3d direction = RaycastUtil.getPlayerLookDirection(transform);

        // Execute the hex
        HexExecutor executor = new HexExecutor();
        executor.execute(hex, casterRef, store, null, direction);
        LOGGER.atInfo().log("Force cast executed successfully");

        // Clear composition after cast
        clearComposition();
    }

    /**
     * Update the loadout (replaces current loadout).
     *
     * @param newLoadout The new loadout to use
     * @param store      The entity store (to update orbital entities)
     */
    public void updateLoadout(Loadout newLoadout, Store<EntityStore> store) {
        // Despawn current orbital glyphs
        despawnOrbitalGlyphs(store);

        // Update loadout reference (need to copy glyphs since loadout field is final)
        this.loadout.clear();
        for (Glyph glyph : newLoadout.getGlyphs()) {
            this.loadout.addGlyph(glyph.getId());
        }

        // Respawn with new loadout
        if (active) {
            spawnOrbitalGlyphs(store);
        }
    }
}
