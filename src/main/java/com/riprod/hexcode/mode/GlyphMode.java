package com.riprod.hexcode.mode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.builtin.hytalegenerator.fields.FastNoiseLite.Vector3;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.Axis;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.casting.styles.BaseGlyphStyle;
import com.riprod.hexcode.casting.styles.RingGlyphStyle;
import com.riprod.hexcode.data.GlyphInstance;
import com.riprod.hexcode.data.HexBookData;
import com.riprod.hexcode.entity.HexNodeEntity;
import com.riprod.hexcode.hex.HexNode;
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

    // Active hex tracking - the hex that will be cast (unified: glyphs are HexNodes
    // with no children)
    private HexNodeEntity activeHex;

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

    // Currently dragging element (unified: HexNodeEntity handles both glyphs and
    // hexes)
    private HexNodeEntity draggingElement;
    private GlyphRotation dragRotation; // Current rotation during drag

    // Continuous hover detection from client sync data
    private HexNodeEntity hoveredElement; // Currently hovered via client sync

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
            draggingElement = null;
            dragRotation = null;
            hoveredElement = null;
            hoveredElement = null;
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

            clearHoverState(store);
            despawnOrbitalGlyphs(commandBuffer);
            active = false;
            draggingElement = null;
            dragRotation = null;
            hoveredElement = null;
            hoveredElement = null;
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
     *
     * <p>
     * All elements are now unified as HexNodeEntity:
     * - Single glyphs are HexNodes with no children
     * - Saved hexes are HexNodes with children (tree structures)
     */
    private void spawnOrbitalGlyphs(@Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // Clear any existing elements from style
        orbitalStyle.clearElements();

        // Get player position
        Vector3d playerPosition = getPlayerPosition(commandBuffer);

        // Get player rotation
        GlyphRotation playerRotation = getPlayerHeadRotation(commandBuffer);

        // Get glyphs from book data if available, otherwise from loadout
        List<GlyphInstance> glyphs = getAvailableGlyphsFromBook();

        // Get saved hexes from book (already as HexNode trees)
        List<HexNode> savedHexes = getSavedHexes();

        // Calculate total entities for spawn position distribution
        int totalEntities = glyphs.size() + savedHexes.size();

        int index = 0;
        int glyphCount = 0;
        int hexCount = 0;

        // Spawn single glyphs as HexNodes with no children
        for (GlyphInstance glyph : glyphs) {
            // Skip invalid glyphs
            if (!glyph.isValid()) {
                LOGGER.atWarning().log("Skipping invalid glyph: %s", glyph.getGlyphId());
                continue;
            }

            // Create HexNode for single glyph (no children = leaf node)
            HexNode node = new HexNode(glyph);
            node.setBaseMargin(GlyphRotation.BASE_TOLERANCE);

            // Get spawn rotation from style
            GlyphRotation rotation = orbitalStyle.getSpawnRotation(playerRotation.getYaw(), index, totalEntities);

            // Set absolute position from rotation
            node.setAbsoluteYaw(rotation.getYaw());
            node.setAbsolutePitch(rotation.getPitch());

            // Recalculate layout (sets scale, angularRadius for leaf node)
            node.recalculateLayout();

            // Calculate spawn position from rotation
            Vector3d spawnPos = rotation.toWorldPosition(playerPosition);

            // Create unified HexNodeEntity
            HexNodeEntity orbitalEntity = new HexNodeEntity(node, player);
            orbitalEntity.spawn(commandBuffer, spawnPos, player);
            orbitalStyle.addElement(orbitalEntity);

            index++;
            glyphCount++;
        }

        // Spawn saved hex HexNodes (composed hexes with children)
        for (HexNode savedHexNode : savedHexes) {
            // Get spawn rotation from style
            GlyphRotation rotation = orbitalStyle.getSpawnRotation(playerRotation.getYaw(), index, totalEntities);

            // Set absolute position from rotation and recalculate layout
            savedHexNode.setAbsoluteYaw(rotation.getYaw());
            savedHexNode.setAbsolutePitch(rotation.getPitch());
            savedHexNode.recalculateLayout();

            // Calculate spawn position from rotation
            Vector3d spawnPos = rotation.toWorldPosition(playerPosition);

            // Create unified HexNodeEntity
            HexNodeEntity orbitalEntity = new HexNodeEntity(savedHexNode, player);
            orbitalEntity.spawn(commandBuffer, spawnPos, player);
            orbitalStyle.addElement(orbitalEntity);

            index++;
            hexCount++;
        }

        // Trigger mode enter effects (magic wheel, particles, etc)
        orbitalStyle.onModeEnter(commandBuffer, playerPosition);

        LOGGER.atInfo().log("Spawned %d orbital entities (%d single glyphs, %d saved hexes)",
                orbitalStyle.getElementCount(), glyphCount, hexCount);
    }

    /**
     * Get the player's current look rotation.
     *
     * @param store     The entity store
     * @param playerRef The player entity reference
     * @return The player's look rotation, or null if unavailable
     */
    private GlyphRotation getPlayerHeadRotation(CommandBuffer<EntityStore> commandBuffer) {
        HeadRotation headRotation = commandBuffer.getComponent(player, HeadRotation.getComponentType());
        if (headRotation == null) {
            return null;
        }

        return GlyphRotation.fromDirection(headRotation.getDirection());
    }

    /**
     * Get the player's current look rotation.
     *
     * @param store     The entity store
     * @param playerRef The player entity reference
     * @return The player's look rotation, or null if unavailable
     */
    private GlyphRotation getPlayerHeadRotation(Store<EntityStore> store) {
        HeadRotation headRotation = store.getComponent(player, HeadRotation.getComponentType());
        if (headRotation == null) {
            return null;
        }

        return GlyphRotation.fromDirection(headRotation.getDirection());
    }

    /**
     * Despawn all orbital glyph entities and saved hex entities.
     * Uses CommandBuffer for deferred entity removal during system ticks.
     */
    private void despawnOrbitalGlyphs(@Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // Trigger mode exit effects first (cleanup magic wheel, particles, etc)
        orbitalStyle.onModeExit(commandBuffer);

        // Despawn all elements from the style
        for (HexNodeEntity element : orbitalStyle.getElements()) {
            element.despawn(commandBuffer);
        }
        orbitalStyle.clearElements();

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
        for (HexNodeEntity element : orbitalStyle.getElements()) {
            element.updatePositionFromPlayer(store, playerPosition);
        }

        // Update visual effects (particles, etc)
        orbitalStyle.updateEffects(store, orbitalStyle.getElements(), dt);
    }

    /**
     * Get all orbital elements (unified: both single glyphs and saved hexes are
     * HexNodeEntity).
     *
     * @return List of all HexNodeEntity elements managed by the style
     */
    public List<HexNodeEntity> getAllOrbitalElements() {
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
            List<HexNodeEntity> elements = orbitalStyle.getElements();
            for (HexNodeEntity element : elements) {
                style.addElement(element);
            }
            this.orbitalStyle = style;
        }
    }

    // ==================== CONTINUOUS HOVER DETECTION ====================

    /**
     * Get the currently hovered element (from client sync data).
     *
     * @return The hovered element, or null if nothing is hovered
     */
    @Nullable
    public HexNodeEntity getHoveredElement() {
        return hoveredElement;
    }

    /**
     * Update the hovered element from client sync data processing.
     * Called from HexcodeGlyphModeToggle.tick0().
     *
     * @param store The entity store for visual updates
     */
    public void updateHoveredElement(Store<EntityStore> store, @Nullable HexNode targetNode) {
        if (hoveredElement != null) {
            hoveredElement.setHovered(targetNode);
            hoveredElement.updateHoverVisual(store);
        }

        if (targetNode == null) {
            // Clear hover
            hoveredElement = null;
            return;
        }
    }

    /**
     * Update the drop target element during drag operations.
     * Called from HexcodeGlyphModeToggle.tick0() while dragging.
     *
     * @param newTarget The new drop target element, or null to clear
     * @param store     The entity store for visual updates
     */
    public void updateHoveredElement(HexNodeEntity newTarget, Store<EntityStore> store, @Nullable HexNode targetNode) {
        if (hoveredElement != null && hoveredElement.equals(newTarget)
                && (targetNode == null || hoveredElement.getHoveredNode().equals(targetNode))) {
            return;
        }

        // Clear old target highlight
        updateHoveredElement(store, null);

        // Set new target
        hoveredElement = newTarget;
        updateHoveredElement(store, targetNode);
    }

    /**
     * Clear all hover state (called when exiting glyph mode).
     *
     * @param store The entity store for visual updates
     */
    public void clearHoverState(Store<EntityStore> store) {
        updateHoveredElement(store, null);
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
     * <p>
     * With unified HexNode treatment (Phase 9), book data directly stores HexNode
     * roots.
     *
     * @return List of saved hex root nodes from book, or empty list if no book data
     */
    public List<HexNode> getSavedHexes() {
        if (bookData == null) {
            return Collections.emptyList();
        }
        return bookData.getSavedHexes();
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
     * <p>
     * Note: Unified treatment means even single glyphs are HexNodeEntity.
     *
     * @return The active HexNodeEntity, or null if none selected
     */
    @Nullable
    public HexNodeEntity getActiveHex() {
        return activeHex;
    }

    /**
     * Set the active hex that will be cast.
     * Called when a hex is interacted with (dragged, dropped onto, etc).
     *
     * @param hex The hex to set as active (HexNodeEntity for both glyphs and hexes)
     */
    public void setActiveHex(@Nullable HexNodeEntity hex) {
        this.activeHex = hex;
    }

    /**
     * Get the HexNode data structure from the active hex for casting.
     *
     * @return The HexNode root to cast, or null if no active hex
     */
    @Nullable
    public HexNode getHexToCast() {
        return activeHex != null ? activeHex.getNode() : null;
    }

    /**
     * Clear the active hex selection.
     */
    public void clearActiveHex() {
        this.activeHex = null;
    }

    // ========== INTERACTION ==========

    /**
     * Get the GlyphInstance from the currently hovered element.
     * Returns the leaf node's value if hovering a single glyph,
     * or null if hovering a composed hex or nothing.
     *
     * @return The GlyphInstance from the hovered leaf node, or null
     */
    @Nullable
    public GlyphInstance getHoveredGlyph() {
        if (hoveredElement == null) {
            return null;
        }
        HexNode node = hoveredElement.getNode();
        // Only return glyph if it's a leaf node (no children = single glyph)
        if (!node.hasChildren()) {
            return node.getValue();
        }
        return null;
    }

    /**
     * Get the GlyphInstance from the currently dragged element.
     * Returns the leaf node's value if dragging a single glyph,
     * or null if dragging a composed hex or nothing.
     *
     * @return The GlyphInstance from the dragged leaf node, or null
     */
    @Nullable
    public GlyphInstance getDraggingGlyph() {
        if (draggingElement == null) {
            return null;
        }
        HexNode node = draggingElement.getNode();
        // Only return glyph if it's a leaf node (no children = single glyph)
        if (!node.hasChildren()) {
            return node.getValue();
        }
        return null;
    }

    /**
     * @return The element currently being dragged (unified: HexNodeEntity for both
     *         glyphs and hexes)
     */
    @Nullable
    public HexNodeEntity getDraggingElement() {
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
     * @param element       The element being dragged (HexNodeEntity)
     * @param startRotation Initial drag rotation (player's look direction)
     */
    public void startDragElement(HexNodeEntity element, GlyphRotation startRotation) {
        this.draggingElement = element;
        this.dragRotation = startRotation;
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

        this.draggingElement = null;
        this.dragRotation = null;
    }

    /**
     * End drag with Store access.
     * Note: HexNodeEntity manages its own state, no ECS sync needed.
     *
     * @param store The entity store (for consistency with interface)
     */
    public void endDrag(Store<EntityStore> store) {
        if (draggingElement != null && draggingElement.isDragging()) {
            draggingElement.setDragging(false);
            LOGGER.atInfo().log("endDrag: Cleared dragging state on element '%s'",
                    draggingElement.getId());
        }

        this.draggingElement = null;
        this.dragRotation = null;
    }

    /**
     * @return Current drag rotation
     */
    public GlyphRotation getDragRotation() {
        return dragRotation;
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
