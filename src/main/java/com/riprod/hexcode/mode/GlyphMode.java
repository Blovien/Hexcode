package com.riprod.hexcode.mode;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.data.GlyphInstance;
import com.riprod.hexcode.data.HexBookData;
import com.riprod.hexcode.data.WorldHexDataStore;
import com.riprod.hexcode.entity.OrbitalGlyphEntity;
import com.riprod.hexcode.entity.OrbitalHexEntity;
import com.riprod.hexcode.execution.HexExecutor;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.util.HexBookMetadata;
import com.riprod.hexcode.util.HexBookMetadata.BookUUIDResult;
import com.riprod.hexcode.util.HexStaffUtil;
import com.riprod.hexcode.util.InventoryUtil;
import com.riprod.hexcode.util.RaycastUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
    private final UUID playerId;  // Player's UUID for entity component ownership
    private final CompositionState composition;
    private HexBookData bookData;  // Data from the held Hex Book

    // Book tracking for swap detection
    private UUID currentBookUUID;     // UUID of the book that activated glyph mode
    private ItemStack currentBookStack;  // Reference to track book swaps

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

    public GlyphMode(Ref<EntityStore> player, @Nonnull UUID playerId) {
        this(player, playerId, null);
    }

    public GlyphMode(Ref<EntityStore> player, @Nonnull UUID playerId, @Nullable HexBookData bookData) {
        this.player = player;
        this.playerId = playerId;
        this.bookData = bookData;
        this.composition = new CompositionState();
        this.active = false;
        this.modeEnteredAt = 0;

        // Default configuration
        this.staminaDrainRate = 5.0f;
        this.movementSpeedMultiplier = 0.5f;
        this.orbitalRadius = 2.5f;
        this.orbitSpeed = 0.05f;  // Radians per second (full rotation ~125 seconds)
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
     * @return The player's UUID
     */
    @Nonnull
    public UUID getPlayerId() {
        return playerId;
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
     * Enter glyph mode with entity spawning using CommandBuffer.
     * Must be used when calling from within a system tick.
     *
     * @param commandBuffer The command buffer for deferred entity operations
     */
    public void enter(@Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (!active) {
            active = true;
            modeEnteredAt = System.currentTimeMillis();
            spawnOrbitalGlyphs(commandBuffer);
            LOGGER.atInfo().log("Glyph mode entered with %d orbital glyphs", orbitalEntities.size());
        }
    }

    /**
     * Enter glyph mode with book context for composition loading.
     *
     * <p>When entering glyph mode, any existing queued hex from the book
     * is loaded into the composition state. This allows players to resume
     * composing a spell they started earlier.
     *
     * <p><b>NOTE:</b> This method uses WorldHexDataStore for queued hex storage,
     * not ItemStack metadata. The book UUID is stored in ItemStack metadata
     * (set once), while the frequently-changing queued hex is stored in world files.
     *
     * @param commandBuffer The command buffer for deferred entity operations
     * @param bookStack The Hex Book item stack that activated glyph mode
     * @param world The world context for WorldHexDataStore access
     * @param inventory The player's inventory (for updating ItemStack if UUID created)
     */
    public void enter(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull ItemStack bookStack,
                      @Nonnull World world, @Nonnull Inventory inventory) {
        if (!active) {
            active = true;
            modeEnteredAt = System.currentTimeMillis();

            // Get or create book UUID (may require inventory update)
            BookUUIDResult uuidResult = HexBookMetadata.getOrCreateBookUUID(bookStack);
            if (uuidResult.wasCreated()) {
                // ItemStack was modified - update inventory
                InventoryUtil.updateOffhandItem(inventory, uuidResult.stack());
                LOGGER.atInfo().log("Created new UUID %s for book", uuidResult.uuid());
            }
            this.currentBookUUID = uuidResult.uuid();
            this.currentBookStack = uuidResult.stack();

            // Load existing queued hex from world storage
            Hex existingHex = WorldHexDataStore.get().getQueuedHex(world, currentBookUUID);
            if (existingHex != null && !existingHex.isEmpty()) {
                composition.loadFromHex(existingHex);
                LOGGER.atInfo().log("Loaded queued hex from world storage: %s", existingHex.toString());
            }

            spawnOrbitalGlyphs(commandBuffer);
            LOGGER.atInfo().log("Glyph mode entered with book %s, %d orbital glyphs",
                    currentBookUUID, orbitalEntities.size());
        }
    }

    /**
     * Enter glyph mode with book context (legacy compatibility).
     *
     * @deprecated Use {@link #enter(CommandBuffer, ItemStack, World, Inventory)} instead
     * @param commandBuffer The command buffer for deferred entity operations
     * @param bookStack The Hex Book item stack that activated glyph mode
     */
    @Deprecated
    public void enter(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull ItemStack bookStack) {
        if (!active) {
            active = true;
            modeEnteredAt = System.currentTimeMillis();

            // Track which book we're using (limited without World/Inventory)
            BookUUIDResult uuidResult = HexBookMetadata.getOrCreateBookUUID(bookStack);
            this.currentBookUUID = uuidResult.uuid();
            this.currentBookStack = uuidResult.stack();
            // NOTE: Cannot update inventory or load from world storage without World/Inventory

            spawnOrbitalGlyphs(commandBuffer);
            LOGGER.atWarning().log("Glyph mode entered without World context - no hex loading");
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
     * Exit glyph mode with entity cleanup using CommandBuffer.
     * Must be used when calling from within a system tick.
     *
     * @param commandBuffer The command buffer for deferred entity operations
     */
    public void exit(@Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (active) {
            despawnOrbitalGlyphs(commandBuffer);
            active = false;
            hoveredGlyph = null;
            draggingGlyph = null;
            dragPosition = null;
            hoveredOrbitalEntity = null;
            currentBookUUID = null;
            currentBookStack = null;
            LOGGER.atInfo().log("Glyph mode exited, despawned orbital glyphs");
        }
    }

    /**
     * Exit glyph mode with book context for composition saving.
     *
     * <p>When exiting glyph mode, the current composition is saved to
     * WorldHexDataStore using the book's UUID. This allows players to cast
     * the spell later or resume composing it.
     *
     * <p><b>NOTE:</b> Composition is saved to world files, not ItemStack metadata.
     *
     * @param commandBuffer The command buffer for deferred entity operations
     * @param world The world context for WorldHexDataStore access
     */
    public void exit(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull World world) {
        if (active) {
            // Save composition to world storage using book UUID
            if (currentBookUUID != null) {
                Hex hex = composition.getHex();
                WorldHexDataStore.get().setQueuedHex(world, currentBookUUID, hex);
                if (!hex.isEmpty()) {
                    LOGGER.atInfo().log("Saved composition to world storage for book %s: %s",
                            currentBookUUID, hex.toString());
                }
            }

            despawnOrbitalGlyphs(commandBuffer);
            active = false;
            hoveredGlyph = null;
            draggingGlyph = null;
            dragPosition = null;
            hoveredOrbitalEntity = null;
            currentBookUUID = null;
            currentBookStack = null;
            LOGGER.atInfo().log("Glyph mode exited with world save, despawned orbital glyphs");
        }
    }

    /**
     * Exit glyph mode with book context (legacy compatibility).
     *
     * @deprecated Use {@link #exit(CommandBuffer, World)} instead
     * @param commandBuffer The command buffer for deferred entity operations
     * @param bookStack The Hex Book item stack (ignored - use World instead)
     */
    @Deprecated
    public void exit(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nullable ItemStack bookStack) {
        if (active) {
            // Cannot save without World context - just log warning
            LOGGER.atWarning().log("Exiting glyph mode without World context - composition not saved");

            despawnOrbitalGlyphs(commandBuffer);
            active = false;
            hoveredGlyph = null;
            draggingGlyph = null;
            dragPosition = null;
            hoveredOrbitalEntity = null;
            currentBookUUID = null;
            currentBookStack = null;
            LOGGER.atInfo().log("Glyph mode exited (no save), despawned orbital glyphs");
        }
    }

    // ==================== BOOK TRACKING ====================

    /**
     * Get the UUID of the book that activated glyph mode.
     *
     * @return The book's UUID, or null if not set
     */
    @Nullable
    public UUID getCurrentBookUUID() {
        return currentBookUUID;
    }

    /**
     * Set the current book being used.
     *
     * <p><b>NOTE:</b> This method may create a new ItemStack if the book UUID
     * needs to be generated. If inventory updates are needed, use
     * {@link #setCurrentBook(ItemStack, Inventory)} instead.
     *
     * @param bookStack The book item stack
     * @return The (possibly new) ItemStack with UUID metadata
     */
    @Nonnull
    public ItemStack setCurrentBook(@Nonnull ItemStack bookStack) {
        BookUUIDResult result = HexBookMetadata.getOrCreateBookUUID(bookStack);
        this.currentBookStack = result.stack();
        this.currentBookUUID = result.uuid();
        return result.stack();
    }

    /**
     * Set the current book being used, updating inventory if needed.
     *
     * @param bookStack The book item stack
     * @param inventory The player's inventory (for updating if UUID created)
     */
    public void setCurrentBook(@Nonnull ItemStack bookStack, @Nonnull Inventory inventory) {
        BookUUIDResult result = HexBookMetadata.getOrCreateBookUUID(bookStack);
        this.currentBookStack = result.stack();
        this.currentBookUUID = result.uuid();
        if (result.wasCreated()) {
            InventoryUtil.updateOffhandItem(inventory, result.stack());
        }
    }

    /**
     * Check if the player swapped to a different book.
     *
     * <p>This is used to detect when glyph mode should exit because
     * the player changed their offhand book.
     *
     * @param currentOffhand The current item in the offhand slot
     * @return true if the book changed (including removal)
     */
    public boolean hasBookChanged(@Nullable ItemStack currentOffhand) {
        if (currentBookUUID == null) {
            return false; // No book was tracked
        }

        if (currentOffhand == null || !HexStaffUtil.isHexBook(currentOffhand)) {
            return true; // Book removed entirely
        }

        // Check if book has a UUID already - if not, it's a different book
        UUID currentUUID = HexBookMetadata.getBookUUID(currentOffhand);
        if (currentUUID == null) {
            // Book has no UUID yet - it's a new/different book
            return true;
        }
        return !currentUUID.equals(currentBookUUID);
    }

    /**
     * Get the current book item stack.
     *
     * @return The book stack, or null if not tracking
     */
    @Nullable
    public ItemStack getCurrentBookStack() {
        return currentBookStack;
    }

    /**
     * Spawn orbital glyph entities for all glyphs in the loadout or book.
     * Also spawns saved hexes from the book as orbital entities.
     * Uses CommandBuffer for deferred entity creation during system ticks.
     */
    private void spawnOrbitalGlyphs(@Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // Clear any existing entities
        orbitalEntities.clear();
        orbitalSavedHexEntities.clear();

        // Get player position (read-only operation, safe to use commandBuffer)
        Vector3d playerPosition = getPlayerPosition(commandBuffer);

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
            // Skip invalid glyphs
            if (!glyph.isValid()) {
                LOGGER.atWarning().log("Skipping invalid glyph: %s", glyph.getGlyphId());
                continue;
            }
            float initialAngle = angleStep * index;
            OrbitalGlyphEntity orbitalEntity = new OrbitalGlyphEntity(glyph, player, initialAngle);
            orbitalEntity.spawn(commandBuffer, playerPosition, playerId);
            orbitalEntities.add(orbitalEntity);
            index++;
        }

        // Spawn saved hex orbitals
        for (Hex savedHex : savedHexes) {
            float initialAngle = angleStep * index;
            OrbitalHexEntity orbitalEntity = new OrbitalHexEntity(savedHex, player, initialAngle);
            orbitalEntity.spawn(commandBuffer, playerPosition, playerId);
            orbitalSavedHexEntities.add(orbitalEntity);
            index++;
        }

        LOGGER.atInfo().log("Spawned %d orbital entities (%d glyphs, %d saved hexes)",
                orbitalEntities.size() + orbitalSavedHexEntities.size(),
                orbitalEntities.size(), orbitalSavedHexEntities.size());
    }

    /**
     * Despawn all orbital glyph entities and saved hex entities.
     * Uses CommandBuffer for deferred entity removal during system ticks.
     */
    private void despawnOrbitalGlyphs(@Nonnull CommandBuffer<EntityStore> commandBuffer) {
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
     * Get the player's current position using CommandBuffer.
     */
    private Vector3d getPlayerPosition(@Nonnull CommandBuffer<EntityStore> commandBuffer) {
        TransformComponent transform = commandBuffer.getComponent(player, TransformComponent.getComponentType());
        if (transform != null) {
            return new Vector3d(transform.getPosition());
        }
        return new Vector3d(0, 0, 0);
    }

    /**
     * Get the player's current position using Store.
     * For use outside of system ticks.
     */
    private Vector3d getPlayerPosition(@Nonnull Store<EntityStore> store) {
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
}
