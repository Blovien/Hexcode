package com.riprod.hexcode.mode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.casting.styles.BaseGlyphStyle;
import com.riprod.hexcode.casting.styles.OrbitalElement;
import com.riprod.hexcode.casting.styles.RingGlyphStyle;
import com.riprod.hexcode.data.GlyphInstance;
import com.riprod.hexcode.data.HexBookData;
import com.riprod.hexcode.data.WorldHexDataStore;
import com.riprod.hexcode.entity.GlyphEntity;
import com.riprod.hexcode.entity.HexEntity;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.util.HexBookMetadata;
import com.riprod.hexcode.util.HexBookMetadata.BookUUIDResult;
import com.riprod.hexcode.util.HexStaffUtil;
import com.riprod.hexcode.util.InventoryUtil;

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
    private final UUID playerId; // Player's UUID for entity component ownership
    private final CompositionState composition;
    private HexBookData bookData; // Data from the held Hex Book

    // Book tracking for swap detection
    private UUID currentBookUUID; // UUID of the book that activated glyph mode
    private ItemStack currentBookStack; // Reference to track book swaps

    private boolean active;
    private long modeEnteredAt;
    private float staminaDrainRate;
    private float movementSpeedMultiplier;

    // Composition configuration
    private float maxCompositionRange;
    private float dragDistance;

    // Currently hovered/dragging glyph
    private GlyphInstance hoveredGlyph;
    private GlyphInstance draggingGlyph;
    private OrbitalElement draggingElement;  // Track any dragged element (glyph or hex)
    private Vector3d dragPosition;

    // Orbital glyph entities
    private List<HexEntity> orbitalEntities;
    private List<HexEntity> orbitalSavedHexEntities;
    private HexEntity hoveredOrbitalEntity;

    // Orbital style system
    private BaseGlyphStyle orbitalStyle;

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
        this.maxCompositionRange = 10.0f;
        this.dragDistance = 2.5f;

        // Initialize orbital entities lists
        this.orbitalEntities = new ArrayList<>();
        this.orbitalSavedHexEntities = new ArrayList<>();

        // Initialize orbital style with default ring configuration
        this.orbitalStyle = new RingGlyphStyle();
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
     * <p>
     * When entering glyph mode, any existing queued hex from the book
     * is loaded into the composition state. This allows players to resume
     * composing a spell they started earlier.
     *
     * <p>
     * <b>NOTE:</b> This method uses WorldHexDataStore for queued hex storage,
     * not ItemStack metadata. The book UUID is stored in ItemStack metadata
     * (set once), while the frequently-changing queued hex is stored in world
     * files.
     *
     * @param commandBuffer The command buffer for deferred entity operations
     * @param bookStack     The Hex Book item stack that activated glyph mode
     * @param world         The world context for WorldHexDataStore access
     * @param inventory     The player's inventory (for updating ItemStack if UUID
     *                      created)
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
     * @deprecated Use {@link #enter(CommandBuffer, ItemStack, World, Inventory)}
     *             instead
     * @param commandBuffer The command buffer for deferred entity operations
     * @param bookStack     The Hex Book item stack that activated glyph mode
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
            // NOTE: Cannot update inventory or load from world storage without
            // World/Inventory

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
            LOGGER.atInfo().log("Glyph mode exited, despawned orbital glyphs and composed entities");
        }
    }

    /**
     * Exit glyph mode with book context for composition saving.
     *
     * <p>
     * When exiting glyph mode, the current composition is saved to
     * WorldHexDataStore using the book's UUID. This allows players to cast
     * the spell later or resume composing it.
     *
     * <p>
     * <b>NOTE:</b> Composition is saved to world files, not ItemStack metadata.
     *
     * @param commandBuffer The command buffer for deferred entity operations
     * @param world         The world context for WorldHexDataStore access
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
            LOGGER.atInfo().log("Glyph mode exited with world save, despawned all entities");
        }
    }

    /**
     * Exit glyph mode with book context (legacy compatibility).
     *
     * @deprecated Use {@link #exit(CommandBuffer, World)} instead
     * @param commandBuffer The command buffer for deferred entity operations
     * @param bookStack     The Hex Book item stack (ignored - use World instead)
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
            LOGGER.atInfo().log("Glyph mode exited (no save), despawned all entities");
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
     * <p>
     * <b>NOTE:</b> This method may create a new ItemStack if the book UUID
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
     * <p>
     * This is used to detect when glyph mode should exit because
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
        // Clear any existing entities and style
        orbitalEntities.clear();
        orbitalSavedHexEntities.clear();
        orbitalStyle.clearElements();

        // Get player position (read-only operation, safe to use commandBuffer)
        Vector3d playerPosition = getPlayerPosition(commandBuffer);
        orbitalStyle.setCenter(playerPosition);

        // Get glyphs from book data if available, otherwise from loadout
        List<GlyphInstance> glyphs = getAvailableGlyphsFromBook();

        // Get saved hexes from book
        List<Hex> savedHexes = getSavedHexes();

        // Calculate total entities for angle distribution
        int totalEntities = glyphs.size() + savedHexes.size();
        float angleStep = (float) (2 * Math.PI / Math.max(1, totalEntities));

        int index = 0;

        // Spawn glyph orbitals (single glyphs use GlyphEntity)
        for (GlyphInstance glyph : glyphs) {
            // Skip invalid glyphs
            if (!glyph.isValid()) {
                LOGGER.atWarning().log("Skipping invalid glyph: %s", glyph.getGlyphId());
                continue;
            }
            float initialAngle = angleStep * index;
            GlyphEntity orbitalEntity = new GlyphEntity(glyph, player, initialAngle);
            orbitalEntity.spawn(commandBuffer, playerPosition, playerId);
            orbitalStyle.addElement(orbitalEntity);
            index++;
        }

        // Spawn saved hex orbitals (composed hexes use HexEntity)
        for (Hex savedHex : savedHexes) {
            float initialAngle = angleStep * index;
            HexEntity orbitalEntity = new HexEntity(savedHex, player, initialAngle);
            orbitalEntity.spawn(commandBuffer, playerPosition, playerId);
            orbitalSavedHexEntities.add(orbitalEntity);
            orbitalStyle.addElement(orbitalEntity);
            index++;
        }

        LOGGER.atInfo().log("Spawned %d orbital entities (%d glyphs, %d saved hexes)",
                orbitalStyle.getElementCount(),
                orbitalStyle.getElementCount() - orbitalSavedHexEntities.size(),
                orbitalSavedHexEntities.size());
    }

    /**
     * Despawn all orbital glyph entities and saved hex entities.
     * Uses CommandBuffer for deferred entity removal during system ticks.
     */
    private void despawnOrbitalGlyphs(@Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // Despawn all elements from the style
        for (OrbitalElement element : orbitalStyle.getElements()) {
            element.despawn(commandBuffer);
        }
        orbitalStyle.clearElements();

        orbitalEntities.clear();
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

        // Update center position to follow player
        Vector3d playerPosition = getPlayerPosition(store);
        orbitalStyle.setCenter(playerPosition);

        // Delegate orbital updates to the style system
        orbitalStyle.tick(store, dt);
    }

    /**
     * Get the list of orbital entities.
     * 
     * @deprecated Use {@link #getOrbitalStyle()} instead for unified element
     *             management
     */
    @Deprecated
    public List<HexEntity> getOrbitalEntities() {
        return orbitalEntities;
    }

    /**
     * Get all orbital elements (both glyphs and saved hexes).
     *
     * @return List of all orbital elements managed by the style
     */
    public List<OrbitalElement> getAllOrbitalElements() {
        return orbitalStyle.getElements();
    }

    /**
     * Get the orbital style managing element positions.
     *
     * @return The current orbital style
     */
    public BaseGlyphStyle getOrbitalStyle() {
        return orbitalStyle;
    }

    /**
     * Set a new orbital style.
     *
     * @param style The new style to use
     */
    public void setOrbitalStyle(BaseGlyphStyle style) {
        if (style != null) {
            // Transfer elements from old style to new style
            List<OrbitalElement> elements = orbitalStyle.getElements();
            for (OrbitalElement element : elements) {
                style.addElement(element);
            }
            this.orbitalStyle = style;
        }
    }

    /**
     * Set the hovered orbital entity.
     * 
     * @deprecated Use {@link #setHoveredOrbitalElement(OrbitalElement, Store)}
     *             instead
     */
    @Deprecated
    public void setHoveredOrbitalEntity(HexEntity entity, Store<EntityStore> store) {
        setHoveredOrbitalElement(entity, store);
    }

    /**
     * Set the hovered orbital element (glyph or hex).
     *
     * @param element The element being hovered, or null to clear
     * @param store   The entity store
     */
    public void setHoveredOrbitalElement(OrbitalElement element, Store<EntityStore> store) {
        // Clear previous hover
        if (hoveredOrbitalEntity != null && hoveredOrbitalEntity != element) {
            hoveredOrbitalEntity.setHovered(false);
            hoveredOrbitalEntity.updateHoverVisual(store);
        }

        if (element instanceof HexEntity) {
            hoveredOrbitalEntity = (HexEntity) element;
        } else {
            hoveredOrbitalEntity = null;
        }

        if (element != null) {
            element.setHovered(true);
            element.updateHoverVisual(store);
            // Extract GlyphInstance from element if it's a GlyphEntity
            if (element instanceof GlyphEntity) {
                hoveredGlyph = ((GlyphEntity) element).getGlyph();
            } else {
                hoveredGlyph = null;
            }
        } else {
            hoveredGlyph = null;
        }
    }

    /**
     * Get the hovered orbital entity.
     * 
     * @deprecated Use orbital style's element tracking instead
     */
    @Deprecated
    public HexEntity getHoveredOrbitalEntity() {
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
            return 0.75f; // Default
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
    public List<HexEntity> getOrbitalSavedHexEntities() {
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
     * @return The glyph currently being dragged (null if dragging a hex)
     */
    public GlyphInstance getDraggingGlyph() {
        return draggingGlyph;
    }

    /**
     * @return The element currently being dragged (glyph or hex)
     */
    public OrbitalElement getDraggingElement() {
        return draggingElement;
    }

    /**
     * @return true if currently dragging any element
     */
    public boolean isDragging() {
        return draggingElement != null;
    }

    /**
     * Start dragging an element.
     *
     * @param element The element being dragged
     * @param startPosition Initial drag position
     */
    public void startDragElement(OrbitalElement element, Vector3d startPosition) {
        this.draggingElement = element;
        this.dragPosition = startPosition;
        // Also set draggingGlyph if it's a GlyphEntity for backward compatibility
        if (element instanceof GlyphEntity) {
            this.draggingGlyph = ((GlyphEntity) element).getGlyph();
        } else {
            this.draggingGlyph = null;
        }
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
        this.draggingElement = null;
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

    public float getMaxCompositionRange() {
        return maxCompositionRange;
    }

    public void setMaxCompositionRange(float range) {
        this.maxCompositionRange = range;
    }

    public float getDragDistance() {
        return dragDistance;
    }

    public void setDragDistance(float distance) {
        this.dragDistance = distance;
    }
}
