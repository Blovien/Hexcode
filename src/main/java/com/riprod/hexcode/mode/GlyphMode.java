package com.riprod.hexcode.mode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.casting.styles.BaseGlyphStyle;
import com.riprod.hexcode.casting.styles.OrbitalElement;
import com.riprod.hexcode.casting.styles.OrbitalElement.ElementState;
import com.riprod.hexcode.casting.styles.RingGlyphStyle;
import com.riprod.hexcode.entity.GlyphComponent;
import com.riprod.hexcode.data.GlyphInstance;
import com.riprod.hexcode.data.HexBookData;
import com.riprod.hexcode.entity.GlyphEntity;
import com.riprod.hexcode.entity.HexEntity;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.math.GlyphRotation;
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
 * - Glyphs from loadout appear around them
 * - Player can compose hexes by dragging glyphs
 *
 * Glyphs are positioned relative to the player and move with them.
 * When dragged, glyphs become independent and can be repositioned.
 */
public class GlyphMode {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Ref<EntityStore> player;
    private final UUID playerId; // Player's UUID for entity component ownership
    private HexBookData bookData; // Data from the held Hex Book

    // Active hex tracking - the hex that will be cast
    private HexEntity activeHex;

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
    private OrbitalElement draggingElement; // Track any dragged element (glyph or hex)
    private GlyphRotation dragRotation; // Current rotation during drag

    // Continuous hover detection from client sync data
    private OrbitalElement hoveredElement; // Currently hovered via client sync
    private OrbitalElement dropTargetElement; // Drop target during drag

    // Orbital glyph entities
    private List<HexEntity> orbitalEntities;
    private List<HexEntity> orbitalSavedHexEntities;
    private HexEntity hoveredOrbitalEntity;

    // Orbital style system
    private BaseGlyphStyle orbitalStyle;

    public GlyphMode(Ref<EntityStore> player, UUID playerId, HexBookData bookData) {
        this.player = player;
        this.playerId = playerId;
        this.bookData = bookData;
        this.activeHex = null;
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
     * @param inventory     The player's inventory (for updating ItemStack if UUID
     *                      created)
     */
    public void enter(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull ItemStack bookStack,
            @Nonnull Inventory inventory) {
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

            spawnOrbitalGlyphs(commandBuffer);
            LOGGER.atInfo().log("Glyph mode entered with book %s, %d orbital glyphs",
                    currentBookUUID, orbitalStyle.getElementCount());
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
            draggingElement = null;
            dragRotation = null;
            hoveredOrbitalEntity = null;
            hoveredElement = null;
            dropTargetElement = null;
            activeHex = null;
            LOGGER.atInfo().log("Glyph mode exited");
        }
    }

    /**
     * Exit glyph mode
     *
     * @param commandBuffer The command buffer for deferred entity operations
     */
    public void exit(@Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (active) {
            // Note: Hexes composed during glyph mode are temporary and discarded on exit.
            // The activeHex is only valid for casting during the mode session.
            // Saved hexes in the book are separate and persist.

            Store<EntityStore> store = commandBuffer.getStore();

            // End any active drag first
            if (isDragging()) {
                endDrag(store);
            }

            // Validate and cleanup before despawning
            validateAndCleanupElements(store);

            clearHoverState(store);
            despawnOrbitalGlyphs(commandBuffer);
            active = false;
            hoveredGlyph = null;
            draggingGlyph = null;
            draggingElement = null;
            dragRotation = null;
            hoveredOrbitalEntity = null;
            hoveredElement = null;
            dropTargetElement = null;
            activeHex = null;
            currentBookUUID = null;
            currentBookStack = null;
            LOGGER.atInfo().log("Glyph mode exited with validation cleanup (composed hexes discarded)");
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

        // Get player position
        Vector3d playerPosition = getPlayerPosition(commandBuffer);

        // Get glyphs from book data if available, otherwise from loadout
        List<GlyphInstance> glyphs = getAvailableGlyphsFromBook();

        // Get saved hexes from book
        List<Hex> savedHexes = getSavedHexes();

        // Calculate total entities for spawn position distribution
        int totalEntities = glyphs.size() + savedHexes.size();

        int index = 0;

        // Spawn glyph orbitals (single glyphs use GlyphEntity)
        for (GlyphInstance glyph : glyphs) {
            // Skip invalid glyphs
            if (!glyph.isValid()) {
                LOGGER.atWarning().log("Skipping invalid glyph: %s", glyph.getGlyphId());
                continue;
            }

            // Get spawn rotation from style
            GlyphRotation rotation = orbitalStyle.getSpawnRotation(index, totalEntities);

            // Calculate spawn position from rotation
            Vector3d spawnPos = rotation.toWorldPosition(playerPosition);

            GlyphEntity orbitalEntity = new GlyphEntity(glyph, player, rotation);
            orbitalEntity.spawn(commandBuffer, spawnPos, playerId);
            orbitalStyle.addElement(orbitalEntity);
            index++;
        }

        // Spawn saved hex orbitals (composed hexes use HexEntity)
        for (Hex savedHex : savedHexes) {
            // Get spawn rotation from style
            GlyphRotation rotation = orbitalStyle.getSpawnRotation(index, totalEntities);

            // Calculate spawn position from rotation
            Vector3d spawnPos = rotation.toWorldPosition(playerPosition);

            HexEntity orbitalEntity = new HexEntity(savedHex, player, rotation);
            orbitalEntity.spawn(commandBuffer, spawnPos, playerId);
            orbitalSavedHexEntities.add(orbitalEntity);
            orbitalStyle.addElement(orbitalEntity);
            index++;
        }

        // Trigger mode enter effects (magic wheel, particles, etc)
        orbitalStyle.onModeEnter(commandBuffer, playerPosition);

        LOGGER.atInfo().log("Spawned %d orbital entities (%d glyphs, %d saved hexes)",
                orbitalStyle.getElementCount(),
                orbitalStyle.getElementCount() - orbitalSavedHexEntities.size(),
                orbitalSavedHexEntities.size());
    }

    /**
     * Validate all elements are in clean state before despawning.
     * Forces cleanup of any elements stuck in invalid states.
     *
     * @param store The entity store for component access
     */
    private void validateAndCleanupElements(Store<EntityStore> store) {
        int cleanedCount = 0;
        for (OrbitalElement element : orbitalStyle.getElements()) {
            ElementState state = element.getState();

            // Check for stuck dragging state
            if (state == ElementState.DRAGGING) {
                LOGGER.atWarning().log("Element '%s' still dragging on exit - forcing cleanup",
                        element.getId());
                element.setDragging(false);
                cleanedCount++;
            }

            // Check for error state
            if (state == ElementState.ERROR) {
                LOGGER.atWarning().log("Element '%s' in ERROR state on exit - forcing to CONSUMED",
                        element.getId());
                element.transitionTo(ElementState.CONSUMED);
                cleanedCount++;
            }

            // Sync ECS component state
            Ref<EntityStore> entityRef = element.getEntityRef();
            if (entityRef != null && entityRef.isValid() && GlyphComponent.getComponentType() != null) {
                GlyphComponent comp = store.getComponent(entityRef, GlyphComponent.getComponentType());
                if (comp != null && comp.isDragging()) {
                    comp.setDragging(false);
                    LOGGER.atWarning().log("GlyphComponent for '%s' had stale drag state",
                            element.getId());
                    cleanedCount++;
                }
            }
        }

        if (cleanedCount > 0) {
            LOGGER.atInfo().log("Cleaned up %d elements with invalid state during exit", cleanedCount);
        }
    }

    /**
     * Despawn all orbital glyph entities and saved hex entities.
     * Uses CommandBuffer for deferred entity removal during system ticks.
     */
    private void despawnOrbitalGlyphs(@Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // Trigger mode exit effects first (cleanup magic wheel, particles, etc)
        orbitalStyle.onModeExit(commandBuffer);

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
     * Elements are positioned relative to player and face the player.
     *
     * @param store The entity store
     * @param dt    Delta time in seconds
     */
    public void updateOrbitalGlyphs(Store<EntityStore> store, float dt) {
        if (!active) {
            return;
        }

        // Get current player position
        Vector3d playerPosition = getPlayerPosition(store);

        // Update each element's position based on player position + their offset
        for (OrbitalElement element : orbitalStyle.getElements()) {
            element.updatePositionFromPlayer(store, playerPosition);
        }

        // Update visual effects (particles, etc)
        orbitalStyle.updateEffects(store, orbitalStyle.getElements(), dt);
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

    // ==================== CONTINUOUS HOVER DETECTION ====================

    /**
     * Get the currently hovered element (from client sync data).
     *
     * @return The hovered element, or null if nothing is hovered
     */
    @Nullable
    public OrbitalElement getHoveredElement() {
        return hoveredElement;
    }

    /**
     * Get the current drop target element (during drag).
     *
     * @return The drop target element, or null if no valid drop target
     */
    @Nullable
    public OrbitalElement getDropTargetElement() {
        return dropTargetElement;
    }

    /**
     * Update the hovered element from client sync data processing.
     * Called from HexcodeGlyphModeToggle.tick0().
     *
     * @param newHovered The new hovered element, or null to clear
     * @param store      The entity store for visual updates
     */
    public void updateHoveredElement(@Nullable OrbitalElement newHovered, Store<EntityStore> store) {
        if (hoveredElement == newHovered) {
            return;
        }

        // Clear old hover
        if (hoveredElement != null) {
            hoveredElement.setHovered(false);
            hoveredElement.updateHoverVisual(store);
        }

        // Set new hover
        hoveredElement = newHovered;
        if (hoveredElement != null) {
            hoveredElement.setHovered(true);
            hoveredElement.updateHoverVisual(store);
        }
    }

    /**
     * Update the drop target element during drag operations.
     * Called from HexcodeGlyphModeToggle.tick0() while dragging.
     *
     * @param newTarget The new drop target element, or null to clear
     * @param store     The entity store for visual updates
     */
    public void updateDropTarget(@Nullable OrbitalElement newTarget, Store<EntityStore> store) {
        if (dropTargetElement == newTarget) {
            return;
        }

        // Clear old target highlight
        if (dropTargetElement != null) {
            dropTargetElement.setHovered(false);
            dropTargetElement.updateHoverVisual(store);
        }

        // Set new target
        dropTargetElement = newTarget;
        if (dropTargetElement != null) {
            dropTargetElement.setHovered(true);
            dropTargetElement.updateHoverVisual(store);
        }
    }

    /**
     * Clear all hover state (called when exiting glyph mode).
     *
     * @param store The entity store for visual updates
     */
    public void clearHoverState(Store<EntityStore> store) {
        updateHoveredElement(null, store);
        updateDropTarget(null, store);
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

    // ========== ACTIVE HEX ==========

    /**
     * Get the active hex that will be cast.
     * The active hex is the most recently interacted hex during glyph mode.
     *
     * @return The active HexEntity, or null if none selected
     */
    @Nullable
    public HexEntity getActiveHex() {
        return activeHex;
    }

    /**
     * Set the active hex that will be cast.
     * Called when a hex is interacted with (dragged, dropped onto, etc).
     *
     * @param hex The hex to set as active
     */
    public void setActiveHex(@Nullable HexEntity hex) {
        this.activeHex = hex;
    }

    /**
     * Get the Hex data structure from the active hex for casting.
     *
     * @return The Hex to cast, or null if no active hex
     */
    @Nullable
    public Hex getHexToCast() {
        return activeHex != null ? activeHex.getHex() : null;
    }

    /**
     * Clear the active hex selection.
     */
    public void clearActiveHex() {
        this.activeHex = null;
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
     * @param element       The element being dragged
     * @param startRotation Initial drag rotation (player's look direction)
     */
    public void startDragElement(OrbitalElement element, GlyphRotation startRotation) {
        this.draggingElement = element;
        this.dragRotation = startRotation;
        // Also set draggingGlyph if it's a GlyphEntity for backward compatibility
        if (element instanceof GlyphEntity) {
            this.draggingGlyph = ((GlyphEntity) element).getGlyph();
        } else {
            this.draggingGlyph = null;
        }
    }

    /**
     * Start dragging an element (legacy compatibility).
     *
     * @param element       The element being dragged
     * @param startPosition Initial drag position
     * @deprecated Use {@link #startDragElement(OrbitalElement, GlyphRotation)}
     *             instead
     */
    @Deprecated
    public void startDragElement(OrbitalElement element, Vector3d startPosition) {
        this.draggingElement = element;
        // Use element's existing rotation since we can't convert position without
        // player context
        this.dragRotation = element.getRotation();
        if (element instanceof GlyphEntity) {
            this.draggingGlyph = ((GlyphEntity) element).getGlyph();
        } else {
            this.draggingGlyph = null;
        }
    }

    /**
     * Update drag rotation.
     *
     * @param rotation The current look rotation
     */
    public void updateDrag(GlyphRotation rotation) {
        this.dragRotation = rotation;
    }

    /**
     * Update drag position (legacy compatibility).
     *
     * @deprecated Use {@link #updateDrag(GlyphRotation)} instead
     */
    @Deprecated
    public void updateDrag(Vector3d position) {
        // Cannot convert position to rotation without player position context
        // Keep existing rotation
    }

    /**
     * End drag with explicit element state cleanup.
     * Ensures the dragged element's state is cleared even if the caller forgets.
     */
    public void endDrag() {
        // CRITICAL: Clear drag state on the element itself (safety net)
        if (draggingElement != null && draggingElement.isDragging()) {
            draggingElement.setDragging(false);
            LOGGER.atInfo().log("endDrag: Cleared dragging state on element '%s'",
                    draggingElement.getId());
        }

        this.draggingGlyph = null;
        this.draggingElement = null;
        this.dragRotation = null;
    }

    /**
     * End drag with Store access for ECS component synchronization.
     *
     * @param store The entity store for component access
     */
    public void endDrag(Store<EntityStore> store) {
        if (draggingElement != null && draggingElement.isDragging()) {
            draggingElement.setDragging(false);

            // Also sync to ECS component
            Ref<EntityStore> entityRef = draggingElement.getEntityRef();
            if (entityRef != null && entityRef.isValid() && GlyphComponent.getComponentType() != null) {
                GlyphComponent comp = store.getComponent(entityRef, GlyphComponent.getComponentType());
                if (comp != null) {
                    comp.setDragging(false);
                }
            }

            LOGGER.atInfo().log("endDrag: Cleared dragging state on element '%s' with ECS sync",
                    draggingElement.getId());
        }

        this.draggingGlyph = null;
        this.draggingElement = null;
        this.dragRotation = null;
    }

    /**
     * @return Current drag rotation
     */
    public GlyphRotation getDragRotation() {
        return dragRotation;
    }

    /**
     * @return Current drag position (calculated from rotation, approximate)
     * @deprecated Use {@link #getDragRotation()} instead
     */
    @Deprecated
    public Vector3d getDragPosition() {
        if (dragRotation == null) {
            return null;
        }
        // Return position at default distance from origin (inaccurate without player
        // context)
        return dragRotation.toWorldPosition(new Vector3d(0, 0, 0));
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
