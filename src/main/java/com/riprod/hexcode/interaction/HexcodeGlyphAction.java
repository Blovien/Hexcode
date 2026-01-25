package com.riprod.hexcode.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.interaction.IInteractionSimulationHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.Collector;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.data.GlyphInstance;
import com.riprod.hexcode.data.WorldHexDataStore;
import com.riprod.hexcode.entity.OrbitalGlyphComponent;
import com.riprod.hexcode.entity.OrbitalGlyphEntity;
import com.riprod.hexcode.execution.HexExecutor;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.hex.HexNode;
import com.riprod.hexcode.mode.CompositionState;
import com.riprod.hexcode.mode.CraftingSpace;
import com.riprod.hexcode.mode.GlyphMode;
import com.riprod.hexcode.mode.GlyphModeManager;
import com.riprod.hexcode.util.HexBookMetadata;
import com.riprod.hexcode.util.HexMathUtil;
import com.riprod.hexcode.util.HexStaffUtil;
import com.riprod.hexcode.util.RaycastUtil;
import com.riprod.hexcode.visual.GlyphRenderer;
import com.riprod.hexcode.visual.TrailEffect;
import com.hypixel.hytale.server.core.util.TargetUtil;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles Primary (left-click) interaction for Hex Staff.
 *
 * When in Glyph Mode:
 * - Initiates glyph drag on first tick (press)
 * - Completes glyph drop when interaction finishes (release)
 *
 * When not in Glyph Mode:
 * - Casts the composed hex if one exists
 */
public class HexcodeGlyphAction extends Interaction {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Selection range for raycast glyph detection */
    private static final float SELECTION_RANGE = 10.0f;

    private final HexExecutor hexExecutor;
    private final GlyphRenderer glyphRenderer;
    private final Map<UUID, TrailEffect> activeTrails;

    // Cached component types for performance (lazy initialization)
    private static volatile ComponentType<EntityStore, TransformComponent> transformComponentType;
    private static volatile ComponentType<EntityStore, HeadRotation> headRotationComponentType;
    private static volatile ComponentType<EntityStore, OrbitalGlyphComponent> orbitalGlyphComponentType;

    public static final BuilderCodec<HexcodeGlyphAction> CODEC = BuilderCodec.builder(
            HexcodeGlyphAction.class,
            HexcodeGlyphAction::new,
            Interaction.ABSTRACT_CODEC).build();

    public HexcodeGlyphAction() {
        this.hexExecutor = new HexExecutor();
        this.glyphRenderer = new GlyphRenderer();
        this.activeTrails = new ConcurrentHashMap<>();
    }

    public HexcodeGlyphAction(String id) {
        super(id);
        this.hexExecutor = new HexExecutor();
        this.glyphRenderer = new GlyphRenderer();
        this.activeTrails = new ConcurrentHashMap<>();
    }

    /** Get cached TransformComponent type */
    private static ComponentType<EntityStore, TransformComponent> getTransformType() {
        if (transformComponentType == null) {
            transformComponentType = TransformComponent.getComponentType();
        }
        return transformComponentType;
    }

    /** Get cached HeadRotation type (for look direction) */
    private static ComponentType<EntityStore, HeadRotation> getHeadRotationType() {
        if (headRotationComponentType == null) {
            headRotationComponentType = HeadRotation.getComponentType();
        }
        return headRotationComponentType;
    }

    /** Get cached OrbitalGlyphComponent type */
    private static ComponentType<EntityStore, OrbitalGlyphComponent> getOrbitalGlyphType() {
        if (orbitalGlyphComponentType == null) {
            orbitalGlyphComponentType = OrbitalGlyphComponent.getComponentType();
        }
        return orbitalGlyphComponentType;
    }

    @Nonnull
    @Override
    public WaitForDataFrom getWaitForDataFrom() {
        // Using None for now - proper client prediction requires tick0() to read context.getClientState()
        return WaitForDataFrom.None;
    }

    @Override
    protected void tick0(
            boolean firstRun,
            float time,
            @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        if (!ref.isValid()) {
            context.getState().state = InteractionState.Failed;
            LOGGER.atInfo().log("HexcodeGlyphAction: Invalid entity reference");
            return;
        }

        Store<EntityStore> store = ref.getStore();

        // Get player from entity
        Player player = context.getCommandBuffer().getComponent(ref, Player.getComponentType());
        if (player == null) {
            LOGGER.atInfo().log("HexcodeGlyphAction: No Player component found");
            context.getState().state = InteractionState.Failed;
            return;
        }

        // Get player UUID
        UUIDComponent uuidComponent = context.getCommandBuffer().getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            LOGGER.atInfo().log("HexcodeGlyphAction: No UUID component found");
            context.getState().state = InteractionState.Failed;
            return;
        }
        UUID playerId = uuidComponent.getUuid();

        // Check equipment requirements
        Inventory inventory = player.getInventory();
        if (!HexStaffUtil.hasHexcodeEquipment(inventory)) {
            // Missing required equipment
            LOGGER.atInfo().log("HexcodeGlyphAction: Wrong Equipment");

            return;
        }

        // Get glyph mode state
        GlyphModeManager modeManager = GlyphModeManager.getInstance();
        GlyphMode mode = modeManager.getSession(playerId);
        boolean inGlyphMode = mode != null && mode.isActive();

        if (inGlyphMode) {
            // In glyph mode: handle drag/drop
            if (firstRun) {
                // First tick = press, start drag
                handleDragStart(playerId, ref, context.getCommandBuffer(), mode);
            }
            // Keep interaction running while dragging
            if (mode.isDragging()) {
                LOGGER.atInfo().log("HexcodeGlyphAction: Dragging in progress");
                context.getState().state = InteractionState.NotFinished;
            }
        } else {
            LOGGER.atInfo().log("HexcodeGlyphAction: Not in glyph mode");
            // Not in glyph mode: cast hex on first tick
            if (firstRun) {
                LOGGER.atInfo().log("HexcodeGlyphAction: Casting hex on first tick");

                // Get world context for WorldHexDataStore
                World world = context.getCommandBuffer().getExternalData().getWorld();

                // First try to get hex from WorldHexDataStore (per-book queued spell)
                ItemStack bookStack = HexStaffUtil.getHexBookFromOffhand(inventory);
                if (bookStack != null && world != null) {
                    // Get book UUID (required for world storage lookup)
                    UUID bookUuid = HexBookMetadata.getBookUUID(bookStack);
                    if (bookUuid != null) {
                        // Get queued hex from world storage
                        Hex hexFromBook = WorldHexDataStore.get().getQueuedHex(world, bookUuid);
                        if (hexFromBook != null && !hexFromBook.isEmpty()) {
                            castHexFromBook(playerId, ref, store, hexFromBook, world, bookUuid);
                            LOGGER.atInfo().log("Player %s cast hex from book %s via Primary", playerId, bookUuid);
                            return;
                        }
                    } else {
                        LOGGER.atInfo().log("Book has no UUID - never used in glyph mode");
                    }
                }

                // Fallback: try composition from active mode session
                if (mode != null) {
                    CompositionState composition = mode.getComposition();
                    if (composition != null && !composition.isEmpty()) {
                        castHex(playerId, ref, store, mode);
                        LOGGER.atInfo().log("Player %s cast hex via Primary", playerId);
                    } else {
                        LOGGER.atInfo().log("No hex composed to cast");
                    }
                } else {
                    LOGGER.atInfo().log("No hex queued in book and no glyph mode session");
                }
            }
        }
    }

    @Override
    public void handle(
            @Nonnull Ref<EntityStore> ref,
            boolean firstRun,
            float time,
            @Nonnull InteractionType type,
            @Nonnull InteractionContext context) {
        super.handle(ref, firstRun, time, type, context);

        // Only end drag when interaction is finishing (mouse released)
        // If state is NotFinished, the interaction is still running - don't end drag yet
        if (context.getState().state == InteractionState.NotFinished) {
            return;
        }

        // Interaction is ending (mouse released or failed)
        if (!ref.isValid()) {
            return;
        }

        Store<EntityStore> store = ref.getStore();

        UUIDComponent uuidComponent = context.getCommandBuffer().getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return;
        }
        UUID playerId = uuidComponent.getUuid();

        GlyphModeManager modeManager = GlyphModeManager.getInstance();
        GlyphMode mode = modeManager.getSession(playerId);

        if (mode != null && mode.isDragging()) {
            handleDragEnd(playerId, ref, store, mode);
        }
    }

    @Override
    protected void simulateTick0(
            boolean firstRun,
            float time,
            @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler) {
        // TODO: Implement proper client-side prediction
        // Requires tick0() to read context.getClientState() and coordinate with this method
        // See ChargingInteraction for reference implementation
    }

    @Override
    public boolean walk(@Nonnull Collector collector, @Nonnull InteractionContext context) {
        // No child interactions
        return false;
    }

    @Nonnull
    @Override
    protected com.hypixel.hytale.protocol.Interaction generatePacket() {
        return new com.hypixel.hytale.protocol.SimpleInteraction();
    }

    @Override
    public boolean needsRemoteSync() {
        // TODO: Enable when client prediction is properly implemented
        return false;
    }

    /**
     * Handle glyph selection and drag start.
     * Left-click finds the orbital glyph the player is looking at,
     * selects it, and starts dragging in one action.
     *
     * <p>Uses Hytale's native {@link TargetUtil#getTargetEntity} for entity targeting,
     * which leverages the engine's spatial queries and ray-AABB intersection with BoundingBox.
     *
     * <p>Synchronizes drag state between OrbitalGlyphEntity (Java object)
     * and OrbitalGlyphComponent (ECS component) for proper system integration.
     *
     * @param playerId Player's UUID
     * @param playerRef Player entity reference
     * @param commandBuffer Command buffer (implements ComponentAccessor for TargetUtil)
     * @param mode Active glyph mode session
     */
    private void handleDragStart(UUID playerId, Ref<EntityStore> playerRef,
                                  CommandBuffer<EntityStore> commandBuffer, GlyphMode mode) {
        // Get orbital entities list for validation
        List<OrbitalGlyphEntity> orbitalEntities = mode.getOrbitalEntities();

        if (orbitalEntities == null || orbitalEntities.isEmpty()) {
            LOGGER.atWarning().log("Player %s has no orbital glyphs to select (list is empty)", playerId);
            return;
        }

        LOGGER.atInfo().log("Player %s attempting to select from %d orbital glyphs", playerId, orbitalEntities.size());

        // Use native Hytale targeting - leverages engine's spatial queries and BoundingBox hit detection
        // CommandBuffer implements ComponentAccessor which TargetUtil requires
        Ref<EntityStore> targetEntityRef = TargetUtil.getTargetEntity(playerRef, SELECTION_RANGE, commandBuffer);

        if (targetEntityRef == null || !targetEntityRef.isValid()) {
            LOGGER.atInfo().log("Player %s clicked but TargetUtil found no entity in range (%.1f blocks)", playerId, SELECTION_RANGE);
            return;
        }

        // Check if the targeted entity is one of our orbital glyphs
        OrbitalGlyphEntity targetGlyph = findOrbitalEntityByRef(orbitalEntities, targetEntityRef);

        if (targetGlyph == null) {
            // Entity found but it's not an orbital glyph - check if it has OrbitalGlyphComponent
            OrbitalGlyphComponent comp = commandBuffer.getComponent(targetEntityRef, getOrbitalGlyphType());
            if (comp != null) {
                LOGGER.atWarning().log("Player %s targeted entity with OrbitalGlyphComponent but not in orbitalEntities list", playerId);
            } else {
                LOGGER.atInfo().log("Player %s targeted an entity but it's not an orbital glyph", playerId);
            }
            return;
        }

        // Don't select if already being dragged
        if (targetGlyph.isDragging()) {
            return;
        }

        GlyphInstance glyphInstance = targetGlyph.getGlyph();
        String glyphName = glyphInstance.getGlyph().getDisplayName();

        LOGGER.atInfo().log("Player %s selected and started dragging glyph '%s'", playerId, glyphName);

        // Select this glyph (updates hover state and visual)
        Store<EntityStore> store = commandBuffer.getStore();
        mode.setHoveredOrbitalEntity(targetGlyph, store);

        // Mark the Java object as being dragged
        targetGlyph.setDragging(true);

        // Synchronize with ECS component for system tick updates
        syncDragStateToComponent(store, targetGlyph, true);

        // Get drag start position using player transform and head rotation
        TransformComponent transform = commandBuffer.getComponent(playerRef, getTransformType());
        HeadRotation headRotation = commandBuffer.getComponent(playerRef, getHeadRotationType());
        Vector3d startPos = (transform != null && headRotation != null)
                ? RaycastUtil.getPointAlongLookRay(transform, headRotation, 2.5f)
                : new Vector3d(0, 0, 0);

        // Start dragging in GlyphMode
        mode.startDrag(glyphInstance, startPos);

        // Start trail effect
        int color = glyphInstance.getGlyph().getVisual().getColor();
        TrailEffect trail = new TrailEffect(color, 20, 0.5f);
        trail.start();
        activeTrails.put(playerId, trail);

        // Start drag trail visual
        glyphRenderer.startDragTrail(store, playerRef, color);
    }

    /**
     * Find the OrbitalGlyphEntity that corresponds to a given entity reference.
     *
     * @param orbitalEntities List of orbital entities to search
     * @param targetRef Entity reference to find
     * @return The matching OrbitalGlyphEntity, or null if not found
     */
    private OrbitalGlyphEntity findOrbitalEntityByRef(List<OrbitalGlyphEntity> orbitalEntities, Ref<EntityStore> targetRef) {
        for (int i = 0, size = orbitalEntities.size(); i < size; i++) {
            OrbitalGlyphEntity entity = orbitalEntities.get(i);
            Ref<EntityStore> entityRef = entity.getEntityRef();
            if (entityRef != null && entityRef.equals(targetRef)) {
                return entity;
            }
        }
        return null;
    }

    /**
     * Synchronize drag state from OrbitalGlyphEntity to OrbitalGlyphComponent.
     * This ensures the ECS system knows when a glyph is being dragged.
     */
    private void syncDragStateToComponent(Store<EntityStore> store, OrbitalGlyphEntity entity, boolean isDragging) {
        Ref<EntityStore> entityRef = entity.getEntityRef();
        if (entityRef == null) {
            return;
        }

        ComponentType<EntityStore, OrbitalGlyphComponent> orbitalType = getOrbitalGlyphType();
        if (orbitalType == null) {
            return;
        }

        OrbitalGlyphComponent component = store.getComponent(entityRef, orbitalType);
        if (component != null) {
            component.setDragging(isDragging);
        }
    }

    /**
     * Handle glyph drag end with gaze-based targeting.
     * Uses raycasting to determine what the player is looking at
     * and performs the appropriate drop action.
     *
     * <p>Synchronizes drag state back to both OrbitalGlyphEntity and
     * OrbitalGlyphComponent to ensure proper cleanup.
     */
    private void handleDragEnd(UUID playerId, Ref<EntityStore> playerRef, Store<EntityStore> store, GlyphMode mode) {
        if (!mode.isDragging()) {
            return;
        }

        GlyphInstance draggingGlyph = mode.getDraggingGlyph();
        CompositionState composition = mode.getComposition();

        // Clear dragging state on the orbital entity and sync to ECS component
        OrbitalGlyphEntity hoveredEntity = mode.getHoveredOrbitalEntity();
        if (hoveredEntity != null) {
            hoveredEntity.setDragging(false);
            syncDragStateToComponent(store, hoveredEntity, false);
            LOGGER.atInfo().log("Player %s ended drag of glyph '%s'",
                    playerId, hoveredEntity.getGlyph().getGlyph().getDisplayName());
        }

        // Get player transform and head rotation for gaze calculation
        TransformComponent transform = RaycastUtil.getPlayerTransform(store, playerRef);
        HeadRotation headRotation = RaycastUtil.getPlayerHeadRotation(store, playerRef);
        if (transform == null || headRotation == null) {
            LOGGER.atWarning().log("Could not get player transform/head rotation for drop");
            mode.endDrag();
            cleanupTrailEffect(playerId);
            return;
        }

        // Get player eye position and look direction (uses HeadRotation for accurate direction)
        Vector3d eyePos = RaycastUtil.getPlayerEyePosition(transform);
        Vector3d lookDir = RaycastUtil.getPlayerLookDirection(headRotation);

        // Create crafting space for this player's current view
        CraftingSpace craftingSpace = new CraftingSpace(
                transform.getPosition(),
                lookDir,
                mode.getCraftingSpaceDistance());

        // Determine drop action based on gaze
        CraftingSpace.DropResult dropResult = craftingSpace.determineDropActionWithGaze(
                eyePos,
                lookDir,
                draggingGlyph.getGlyph(),
                composition.getHex().getRoot());

        // Execute the drop action
        boolean success = false;
        String actionDesc = "";

        switch (dropResult.getAction()) {
            case PLACE_AS_ROOT:
                success = composition.placeRoot(draggingGlyph);
                actionDesc = "placed as root";
                break;

            case WRAP_NODE:
                if (dropResult.getTargetNode() != null) {
                    success = composition.wrapNode(draggingGlyph, dropResult.getTargetNode());
                    actionDesc = "wrapped '" + dropResult.getTargetNode().getValue().getGlyph().getDisplayName() + "'";
                }
                break;

            case ADD_AS_SIBLING:
                if (dropResult.getTargetNode() != null) {
                    success = composition.addSibling(draggingGlyph, dropResult.getTargetNode());
                    actionDesc = "added as sibling to '"
                            + dropResult.getTargetNode().getValue().getGlyph().getDisplayName() + "'";
                }
                break;

            case INVALID:
                LOGGER.atInfo().log("Invalid drop location - not looking at crafting space");
                break;
        }

        if (success) {
            LOGGER.atInfo().log("Glyph '%s' %s in composition",
                    draggingGlyph.getGlyph().getDisplayName(), actionDesc);

            // TODO: Update visual representation of crafted glyphs
            // updateCraftedGlyphVisuals(mode, store, craftingSpace);
        } else if (dropResult.getAction() != CraftingSpace.DropAction.INVALID) {
            LOGGER.atInfo().log("Failed to add glyph '%s' - %s",
                    draggingGlyph.getGlyph().getDisplayName(), actionDesc);
        }

        // End dragging
        mode.endDrag();

        // Stop trail effect
        cleanupTrailEffect(playerId);
        glyphRenderer.stopDragTrail(store, playerRef);
    }

    /**
     * Cast the composed hex.
     */
    private void castHex(UUID playerId, Ref<EntityStore> playerRef, Store<EntityStore> store, GlyphMode mode) {
        CompositionState composition = mode.getComposition();
        Hex hex = composition.getHex();

        if (hex.isEmpty()) {
            LOGGER.atInfo().log("Failed to cast hex - empty or invalid composition");
            return;
        }

        // Calculate mana cost by traversing the hex tree
        float totalManaCost = calculateHexManaCost(hex);
        float playerMana = getPlayerMana(store, playerRef);

        // Validate mana before casting (75% minimum threshold)
        float minRequiredMana = totalManaCost * 0.75f;
        if (playerMana < minRequiredMana) {
            LOGGER.atInfo().log("Failed to cast hex - insufficient mana (have: %.0f, need: %.0f, minimum: %.0f)",
                    playerMana, totalManaCost, minRequiredMana);
            return;
        }

        // Get cast origin and direction (use HeadRotation for accurate look direction)
        TransformComponent transform = store.getComponent(playerRef, getTransformType());
        HeadRotation headRotation = store.getComponent(playerRef, getHeadRotationType());
        if (transform == null || headRotation == null) {
            return;
        }
        Vector3d direction = RaycastUtil.getPlayerLookDirection(headRotation);

        // Consume mana - use actual cost or all remaining mana if below 100%
        float manaToConsume = Math.min(totalManaCost, playerMana);
        consumePlayerMana(playerRef, manaToConsume);
        LOGGER.atInfo().log("Consumed %.0f mana for hex (total cost: %.0f, had: %.0f)",
                manaToConsume, totalManaCost, playerMana);

        // Execute the hex
        HexExecutor.ExecutionResult result = hexExecutor.execute(hex, playerRef, store, null, direction);
        if (result.isSuccess()) {
            LOGGER.atInfo().log("Hex cast successfully");
        } else {
            LOGGER.atWarning().log("Hex execution failed: %s", result.getMessage());
        }

        // Clear composition
        composition.clear();
    }

    /**
     * Cast a hex from WorldHexDataStore.
     *
     * <p>
     * Used when player casts outside glyph mode - the hex is read from
     * the world storage using the book's UUID.
     *
     * @param playerId  The player's UUID
     * @param playerRef The player entity reference
     * @param store     The entity store
     * @param hex       The hex to cast (from WorldHexDataStore)
     * @param world     The world context (for clearing after cast)
     * @param bookUuid  The book's UUID (for clearing after cast)
     */
    private void castHexFromBook(UUID playerId, Ref<EntityStore> playerRef, Store<EntityStore> store,
            Hex hex, World world, UUID bookUuid) {
        if (hex.isEmpty()) {
            LOGGER.atInfo().log("Failed to cast hex from book - empty or invalid hex");
            return;
        }

        // Calculate mana cost by traversing the hex tree
        float totalManaCost = calculateHexManaCost(hex);
        float playerMana = getPlayerMana(store, playerRef);

        // Validate mana before casting (75% minimum threshold)
        float minRequiredMana = totalManaCost * 0.75f;
        if (playerMana < minRequiredMana) {
            LOGGER.atInfo().log("Failed to cast hex - insufficient mana (have: %.0f, need: %.0f, minimum: %.0f)",
                    playerMana, totalManaCost, minRequiredMana);
            return;
        }

        // Get cast origin and direction (use HeadRotation for accurate look direction)
        TransformComponent transform = store.getComponent(playerRef, getTransformType());
        HeadRotation headRotation = store.getComponent(playerRef, getHeadRotationType());
        if (transform == null || headRotation == null) {
            return;
        }
        Vector3d direction = RaycastUtil.getPlayerLookDirection(headRotation);

        // Consume mana - use actual cost or all remaining mana if below 100%
        float manaToConsume = Math.min(totalManaCost, playerMana);
        consumePlayerMana(playerRef, manaToConsume);
        LOGGER.atInfo().log("Consumed %.0f mana for hex from book (total cost: %.0f, had: %.0f)",
                manaToConsume, totalManaCost, playerMana);

        // Execute the hex
        HexExecutor.ExecutionResult result = hexExecutor.execute(hex, playerRef, store, null, direction);
        if (result.isSuccess()) {
            LOGGER.atInfo().log("Hex from book cast successfully");
            // Optionally clear the book's queued hex after casting
            // Comment out if you want to keep the hex for repeated casting
            // WorldHexDataStore.get().clearQueuedHex(world, bookUuid);
        } else {
            LOGGER.atWarning().log("Hex from book execution failed: %s", result.getMessage());
        }
    }

    /**
     * Calculate the total mana cost of a hex by traversing the tree.
     *
     * @param hex The hex to calculate cost for
     * @return Total mana cost (sum of all glyph costs)
     */
    private float calculateHexManaCost(Hex hex) {
        if (hex.isEmpty()) {
            return 0f;
        }
        return calculateNodeManaCost(hex.getRoot());
    }

    /**
     * Recursively calculate mana cost for a node and its children.
     */
    private float calculateNodeManaCost(HexNode node) {
        float cost = 0f;

        GlyphInstance glyphInstance = node.getValue();
        if (glyphInstance != null && glyphInstance.isValid()) {
            // Get base mana cost from glyph asset definition
            cost += glyphInstance.getGlyph().getAssetDefinition().getBaseManaCost();
        }

        // Add children costs
        for (HexNode child : node.getChildren()) {
            cost += calculateNodeManaCost(child);
        }

        return cost;
    }

    /**
     * Check if a position is within the crafting space.
     */
    private boolean isInCraftingSpace(GlyphMode mode, Vector3d position, Store<EntityStore> store) {
        if (position == null) {
            return false;
        }

        Ref<EntityStore> playerRef = mode.getPlayer();
        TransformComponent transform = store.getComponent(playerRef, getTransformType());
        HeadRotation headRotation = store.getComponent(playerRef, getHeadRotationType());
        if (transform == null || headRotation == null) {
            return true; // Fallback
        }

        Vector3d playerPos = transform.getPosition();
        float craftingSpaceDistance = mode.getCraftingSpaceDistance();

        // Calculate crafting space center (in front of player) using HeadRotation for look direction
        Vector3d lookDir = RaycastUtil.getPlayerLookDirection(headRotation);
        Vector3d craftingCenter = new Vector3d(
                playerPos.x + lookDir.x * craftingSpaceDistance,
                playerPos.y + 1.5 + lookDir.y * craftingSpaceDistance,
                playerPos.z + lookDir.z * craftingSpaceDistance);

        // Check if position is within crafting space bounds (1.5 block radius)
        double distSq = HexMathUtil.distanceSquared(position, craftingCenter);
        return distSq <= 2.25; // 1.5^2 = 2.25
    }

    /**
     * Get player's current mana.
     */
    private float getPlayerMana(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        EntityStatMap playerStats = store.getComponent(playerRef, EntityStatMap.getComponentType());
        if (playerStats == null) {
            return 0;
        }

        int manaIndex = DefaultEntityStatTypes.getMana();
        EntityStatValue manaValue = playerStats.get(manaIndex);
        if (manaValue == null) {
            return 0;
        }
        return manaValue.get();
    }

    /**
     * Consume player mana.
     */
    private void consumePlayerMana(Ref<EntityStore> playerRef, float amount) {
        Store<EntityStore> store = playerRef.getStore();

        EntityStatMap playerStats = store.getComponent(playerRef, EntityStatMap.getComponentType());
        if (playerStats == null) {
            return;
        }

        int manaIndex = DefaultEntityStatTypes.getMana();
        playerStats.addStatValue(manaIndex, -amount);
        LOGGER.atInfo().log("Consumed %.0f mana from player", amount);
    }

    /**
     * Clean up trail effect for a player.
     */
    private void cleanupTrailEffect(UUID playerId) {
        TrailEffect trail = activeTrails.remove(playerId);
        if (trail != null) {
            trail.stop();
        }
    }
}
