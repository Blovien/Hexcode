package com.riprod.hexcode.mode;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.data.GlyphInstance;
import com.riprod.hexcode.data.HexBookData;
import com.riprod.hexcode.entity.OrbitalGlyphEntity;
import com.riprod.hexcode.entity.OrbitalHexEntity;
import com.riprod.hexcode.execution.HexExecutor;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.util.RaycastUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
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
    private final CompositionState composition;
    private HexBookData bookData;  // Data from the held Hex Book

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
    private GlyphInstance hoveredGlyph;
    private GlyphInstance draggingGlyph;
    private Vector3d dragPosition;

    // Orbital glyph entities
    private List<OrbitalGlyphEntity> orbitalEntities;
    private List<OrbitalHexEntity> orbitalSavedHexEntities;
    private OrbitalGlyphEntity hoveredOrbitalEntity;
    private OrbitalHexEntity hoveredSavedHexEntity;

    public GlyphMode(Ref<EntityStore> player) {
        this(player, null);
    }

    public GlyphMode(Ref<EntityStore> player, @Nullable HexBookData bookData) {
        this.player = player;
        this.bookData = bookData;
        this.composition = new CompositionState();
        this.active = false;
        this.modeEnteredAt = 0;

        // Default configuration
        this.staminaDrainRate = 5.0f;
        this.movementSpeedMultiplier = 0.5f;
        this.orbitalRadius = 2.5f;
        this.orbitSpeed = 0.3f;
        this.craftingSpaceDistance = 2.0f;

        // Initialize orbital entities lists
        this.orbitalEntities = new ArrayList<>();
        this.orbitalSavedHexEntities = new ArrayList<>();
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
     * @param commandBuffer The command buffer for deferred entity operations
     */
    public void enter(CommandBuffer<EntityStore> commandBuffer) {
        if (!active) {
            active = true;
            modeEnteredAt = System.currentTimeMillis();
            spawnOrbitalGlyphs(commandBuffer);
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
     * @param commandBuffer The command buffer for deferred entity operations
     */
    public void exit(CommandBuffer<EntityStore> commandBuffer) {
        if (active) {
            despawnOrbitalGlyphs(commandBuffer);
            active = false;
            hoveredGlyph = null;
            draggingGlyph = null;
            dragPosition = null;
            hoveredOrbitalEntity = null;
            LOGGER.atInfo().log("Glyph mode exited, despawned orbital glyphs");
        }
    }

    /**
     * Spawn orbital glyph entities for all glyphs in the loadout or book.
     * Also spawns saved hexes from the book as orbital entities.
     */
    private void spawnOrbitalGlyphs(CommandBuffer<EntityStore> commandBuffer) {
        // Clear any existing entities
        orbitalEntities.clear();
        orbitalSavedHexEntities.clear();

        // Get player position (read-only operation, safe to use store)
        Vector3d playerPosition = getPlayerPosition(commandBuffer.getStore());

        // Get glyphs from book data if available, otherwise from loadout
        List<GlyphInstance> glyphs = getAvailableGlyphsFromBook();

        // Get saved hexes from book
        List<Hex> savedHexes = getSavedHexes();

        // Calculate total entities for angle distribution
        int totalEntities = glyphs.size() + savedHexes.size();
        float angleStep = (float) (2 * Math.PI / Math.max(1, totalEntities));

        int index = 0;

        // Spawn glyph orbitals
        for (GlyphInstance glyph : glyphs) {
            float initialAngle = angleStep * index;
            OrbitalGlyphEntity orbitalEntity = new OrbitalGlyphEntity(glyph, player, initialAngle);
            orbitalEntity.spawn(commandBuffer, playerPosition);
            orbitalEntities.add(orbitalEntity);
            index++;
        }

        // Spawn saved hex orbitals
        for (Hex savedHex : savedHexes) {
            float initialAngle = angleStep * index;
            OrbitalHexEntity orbitalEntity = new OrbitalHexEntity(savedHex, player, initialAngle);
            orbitalEntity.spawn(commandBuffer, playerPosition, null);
            orbitalSavedHexEntities.add(orbitalEntity);
            index++;
        }

        LOGGER.atInfo().log("Spawned %d orbital entities (%d glyphs, %d saved hexes)",
                orbitalEntities.size() + orbitalSavedHexEntities.size(),
                orbitalEntities.size(), orbitalSavedHexEntities.size());
    }

    /**
     * Despawn all orbital glyph entities and saved hex entities.
     */
    private void despawnOrbitalGlyphs(CommandBuffer<EntityStore> commandBuffer) {
        for (OrbitalGlyphEntity entity : orbitalEntities) {
            entity.despawn(commandBuffer);
        }
        orbitalEntities.clear();

        for (OrbitalHexEntity entity : orbitalSavedHexEntities) {
            entity.despawn(commandBuffer);
        }
        orbitalSavedHexEntities.clear();

        LOGGER.atInfo().log("Despawned all orbital entities");
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
     * Update all orbital glyphs and saved hexes (called each tick).
     *
     * @param store The entity store
     * @param dt    Delta time in seconds
     */
    public void updateOrbitalGlyphs(Store<EntityStore> store, float dt) {
        if (!active) {
            return;
        }

        Vector3d playerPosition = getPlayerPosition(store);

        // Update glyph orbitals
        for (OrbitalGlyphEntity entity : orbitalEntities) {
            entity.update(dt);
            entity.updateWorldPosition(store, playerPosition);
        }

        // Update saved hex orbitals
        for (OrbitalHexEntity entity : orbitalSavedHexEntities) {
            entity.update(dt);
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

    /**
     * Get glyphs available for casting from the book's data.
     * This provides glyphs stored in the Hex Book.
     *
     * @return List of glyphs from book, or empty list if no book data
     */
    public List<GlyphInstance> getAvailableGlyphsFromBook() {
        if (bookData == null) {
            return Collections.emptyList();
        }

        List<GlyphInstance> glyphs = new ArrayList<>();

        for (GlyphInstance glyph : bookData.getAllGlyphData()) {
            if (glyph != null) {
                glyphs.add(glyph);
            }
        }

        return glyphs;
    }

    /**
     * Get accuracy for a glyph from book data.
     *
     * @param glyphId The glyph ID
     * @return Accuracy from book data, or 0.75 default
     */
    public float getGlyphAccuracy(String glyphId) {
        if (bookData == null) {
            return 0.75f;  // Default
        }
        return bookData.getAccuracy(glyphId);
    }

    /**
     * Get saved hexes that can be cast like glyphs.
     *
     * @return List of saved hexes from book, or empty list if no book data
     */
    public List<Hex> getSavedHexes() {
        if (bookData == null) {
            return Collections.emptyList();
        }
        return bookData.getSavedHexes();
    }

    /**
     * Get the orbital saved hex entities.
     */
    public List<OrbitalHexEntity> getOrbitalSavedHexEntities() {
        return orbitalSavedHexEntities;
    }

    /**
     * Set the book data.
     */
    public void setBookData(HexBookData bookData) {
        this.bookData = bookData;
    }

    /**
     * Get the book data.
     */
    @Nullable
    public HexBookData getBookData() {
        return bookData;
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
    public GlyphInstance getHoveredGlyph() {
        return hoveredGlyph;
    }

    /**
     * Set the hovered glyph.
     */
    public void setHoveredGlyph(GlyphInstance glyph) {
        this.hoveredGlyph = glyph;
    }

    /**
     * @return The glyph currently being dragged
     */
    public GlyphInstance getDraggingGlyph() {
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
    public void startDrag(GlyphInstance glyph, Vector3d startPosition) {
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
}
