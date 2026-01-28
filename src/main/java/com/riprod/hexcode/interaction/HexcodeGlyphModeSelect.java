package com.riprod.hexcode.interaction;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.ChargingInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.Collector;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.casting.RotationObserver;
import com.riprod.hexcode.casting.RotationObserver.DropTarget;
import com.riprod.hexcode.casting.RotationObserver.HexDropTarget;
import com.riprod.hexcode.casting.styles.OrbitalElement;
import com.riprod.hexcode.casting.styles.OrbitalElement.ElementState;
import com.riprod.hexcode.data.GlyphInstance;
import com.riprod.hexcode.entity.GlyphComponent;
import com.riprod.hexcode.entity.GlyphEntity;
import com.riprod.hexcode.entity.HexEntity;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.hex.HexNode;
import com.riprod.hexcode.math.GlyphRotation;
import com.riprod.hexcode.mode.GlyphMode;
import com.riprod.hexcode.mode.GlyphModeManager;
import com.riprod.hexcode.util.HexStaffUtil;
import com.riprod.hexcode.util.RaycastUtil;
import com.riprod.hexcode.util.RotationMath;
import com.riprod.hexcode.visual.GlyphRenderer;
import com.riprod.hexcode.visual.TrailEffect;

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
public class HexcodeGlyphModeSelect extends ChargingInteraction {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final float CHARGING_HELD = -1.0F; // Still holding
    private static final float CHARGING_CANCELED = -2.0F; // Cancelled

    private final GlyphRenderer glyphRenderer;
    private final Map<UUID, TrailEffect> activeTrails;
    private final RotationObserver rotationObserver;

    // Cached component types for performance (lazy initialization)
    private static volatile ComponentType<EntityStore, TransformComponent> transformComponentType;
    private static volatile ComponentType<EntityStore, HeadRotation> headRotationComponentType;
    private static volatile ComponentType<EntityStore, GlyphComponent> orbitalGlyphComponentType;

    public static final BuilderCodec<HexcodeGlyphModeSelect> CODEC = BuilderCodec.builder(
            HexcodeGlyphModeSelect.class,
            HexcodeGlyphModeSelect::new,
            ChargingInteraction.ABSTRACT_CODEC)
            .appendInherited(
                    new KeyedCodec<>("AllowIndefiniteHold", Codec.BOOLEAN),
                    (i, s) -> i.allowIndefiniteHold = s,
                    i -> i.allowIndefiniteHold,
                    (i, p) -> i.allowIndefiniteHold = p.allowIndefiniteHold)
            .add()
            .appendInherited(
                    new KeyedCodec<>("DisplayProgress", Codec.BOOLEAN),
                    (i, s) -> i.displayProgress = s,
                    i -> i.displayProgress,
                    (i, p) -> i.displayProgress = p.displayProgress)
            .add()
            .build();

    public HexcodeGlyphModeSelect() {
        this.glyphRenderer = new GlyphRenderer();
        this.activeTrails = new ConcurrentHashMap<>();
        this.rotationObserver = new RotationObserver();
        this.allowIndefiniteHold = true; // Enable indefinite hold for dragging
        this.displayProgress = false; // No progress bar needed
    }

    public HexcodeGlyphModeSelect(String id) {
        this.glyphRenderer = new GlyphRenderer();
        this.activeTrails = new ConcurrentHashMap<>();
        this.rotationObserver = new RotationObserver();
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
    private static ComponentType<EntityStore, GlyphComponent> getOrbitalGlyphType() {
        if (orbitalGlyphComponentType == null) {
            orbitalGlyphComponentType = GlyphComponent.getComponentType();
        }
        return orbitalGlyphComponentType;
    }

    @Nonnull
    @Override
    public WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.Client; // Wait for client to report release
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
        GlyphModeManager modeManager = GlyphModeManager.getInstance();

        if (!HexStaffUtil.hasHexcodeEquipment(inventory)) {
            // Missing required equipment
            LOGGER.atInfo().log("HexcodeGlyphAction: Wrong Equipment");
            context.getState().state = InteractionState.Failed;
            // exit glyph mode
            modeManager.removeSession(playerId);
            return;
        }

        // Get glyph mode state
        GlyphMode mode = modeManager.getSession(playerId);
        boolean inGlyphMode = mode != null && mode.isActive();

        if (!inGlyphMode) {
            // Not in glyph mode - finish immediately
            context.getState().state = InteractionState.Finished;
            return;
        }

        // In glyph mode: handle drag/drop
        if (firstRun) {
            // First tick = press, start drag
            handleDragStart(playerId, ref, context.getCommandBuffer(), mode);

            // If we started dragging, keep running
            if (mode.isDragging()) {
                context.getState().state = InteractionState.NotFinished;
                return;
            }

            // Nothing was selected to drag - finish immediately
            context.getState().state = InteractionState.Finished;
            return;
        }

        if (syncData.chargeValue == CHARGING_CANCELED || syncData.chargeValue != CHARGING_HELD) {
            // Interaction was released or cancelled
            handleDragEnd(playerId, ref, context.getCommandBuffer(), mode);
            context.getState().state = InteractionState.Finished;
            return;
        }

        // Update drag position while holding
        if (mode.isDragging()) {
            updateDragPosition(ref, store, mode);
        }
        context.getState().state = InteractionState.NotFinished;
    }

    @Override
    protected void simulateTick0(
            boolean firstRun,
            float time,
            @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler) {

        // Let parent handle isCharging() detection and state sync
        // This is CRITICAL - without this, chargeValue is never set properly
        // and the server sees a non-HELD value, immediately ending the drag
        super.simulateTick0(firstRun, time, type, context, cooldownHandler);
    }

    @Override
    public boolean walk(@Nonnull Collector collector, @Nonnull InteractionContext context) {
        // No child interactions
        return false;
    }

    @Nonnull
    @Override
    protected com.hypixel.hytale.protocol.Interaction generatePacket() {
        return new com.hypixel.hytale.protocol.ChargingInteraction();
    }

    @Override
    public boolean needsRemoteSync() {
        // Enable sync so client state is sent to server
        return true;
    }

    /**
     * Handle glyph selection and drag start.
     * Uses the pre-computed hovered element from sync data processing
     * in HexcodeGlyphModeToggle.
     *
     * <p>
     * Integrates with the orbital style system to exclude dragged elements
     * from orbital rotation updates.
     *
     * @param playerId      Player's UUID
     * @param playerRef     Player entity reference
     * @param commandBuffer Command buffer (implements ComponentAccessor for
     *                      TargetUtil)
     * @param mode          Active glyph mode session
     */
    private void handleDragStart(UUID playerId, Ref<EntityStore> playerRef,
            CommandBuffer<EntityStore> commandBuffer, GlyphMode mode) {

        // Use pre-computed hovered element from hover detection in Toggle
        OrbitalElement targetElement = mode.getHoveredElement();

        if (targetElement == null) {
            return;
        }

        // Don't select if already being dragged
        if (targetElement.isDragging()) {
            return;
        }

        Store<EntityStore> store = commandBuffer.getStore();

        // Get glyph information for logging
        String elementName = targetElement.isSavedHex() ? "hex" : "glyph";
        LOGGER.atInfo().log("Player %s selected and started dragging %s '%s'",
                playerId, elementName, targetElement.getId());

        // Select this element (updates hover state and visual)
        mode.setHoveredOrbitalElement(targetElement, store);

        // Mark element as dragging
        targetElement.setDragging(true);

        // Synchronize with ECS component for system tick updates
        syncDragStateToComponent(store, targetElement, true);

        // Get drag start rotation using player head rotation
        GlyphRotation startRotation = rotationObserver.getPlayerLookRotation(store, playerRef);
        if (startRotation == null) {
            startRotation = targetElement.getRotation();
        }

        // Start dragging in GlyphMode - track the element being dragged
        mode.startDragElement(targetElement, startRotation);

        // Start trail effect with element's visual color
        int color = targetElement.getVisual().getColor();
        TrailEffect trail = new TrailEffect(color, 20, 0.5f);
        trail.start();
        activeTrails.put(playerId, trail);

        // Start drag trail visual
        glyphRenderer.startDragTrail(store, playerRef, color);
    }

    /**
     * Update the rotation of the dragged element to follow the player's look
     * direction.
     * Called every tick while dragging to provide smooth visual feedback.
     *
     * @param playerRef Player entity reference
     * @param store     The entity store
     * @param mode      The glyph mode with drag state
     */
    private void updateDragPosition(Ref<EntityStore> playerRef, Store<EntityStore> store, GlyphMode mode) {
        OrbitalElement draggedElement = mode.getDraggingElement();
        if (draggedElement == null) {
            LOGGER.atWarning().log("updateDragPosition: no dragged element");
            return;
        }

        // Get current look rotation
        GlyphRotation lookRotation = rotationObserver.getPlayerLookRotation(store, playerRef);
        if (lookRotation == null) {
            LOGGER.atWarning().log("updateDragPosition: could not get look rotation");
            return;
        }

        // Get player position for world position calculation
        TransformComponent transform = store.getComponent(playerRef, getTransformType());
        if (transform == null) {
            return;
        }
        Vector3d playerPos = transform.getPosition();

        // Calculate world position from look rotation
        Vector3d newPosition = lookRotation.toWorldPosition(playerPos);

        // Update the element's world position
        draggedElement.updateWorldPositionDirect(store, newPosition);

        // Update GlyphMode's tracked drag rotation
        mode.updateDrag(lookRotation);
    }

    /**
     * Synchronize drag state from OrbitalElement to GlyphComponent.
     * This ensures the ECS system knows when a glyph/hex is being dragged.
     */
    private void syncDragStateToComponent(Store<EntityStore> store, OrbitalElement element, boolean isDragging) {
        if (element == null) {
            return;
        }
        Ref<EntityStore> entityRef = element.getEntityRef();
        if (entityRef == null) {
            return;
        }

        ComponentType<EntityStore, GlyphComponent> orbitalType = getOrbitalGlyphType();
        if (orbitalType == null) {
            return;
        }

        GlyphComponent component = store.getComponent(entityRef, orbitalType);
        if (component != null) {
            component.setDragging(isDragging);
        }
    }

    /**
     * Handle glyph drag end with drop target detection.
     *
     * <p>
     * Uses the pre-computed drop target from sync data processing.
     * For HexEntity targets, queries tier info at release time.
     *
     * <p>
     * Drop behavior:
     * <ul>
     * <li>Drop on hex (tier-based): Add glyph as child of that tier's node, consume
     * glyph</li>
     * <li>Drop on single glyph: Target wraps dragged (B[A]), both consumed, new hex
     * spawned</li>
     * <li>Drop in empty space: Convert glyph to single-glyph hex</li>
     * </ul>
     *
     * <p>
     * Glyphs are consumed when added to hexes. The last hex interacted with becomes
     * the active hex that will be cast.
     */
    private void handleDragEnd(UUID playerId, Ref<EntityStore> playerRef, CommandBuffer<EntityStore> commandBuffer,
            GlyphMode mode) {
        if (!mode.isDragging()) {
            return;
        }

        Store<EntityStore> store = commandBuffer.getStore();
        OrbitalElement draggedElement = mode.getDraggingElement();

        // Get current look rotation for drop
        GlyphRotation lookRotation = rotationObserver.getPlayerLookRotation(store, playerRef);
        if (lookRotation == null) {
            lookRotation = mode.getDragRotation();
        }

        // Use rotation-based drop target detection
        DropTarget dropTarget = rotationObserver.findDropTarget(store, playerRef, mode.getAllOrbitalElements());

        // Get player position for spawn position calculation
        TransformComponent playerTransform = commandBuffer.getComponent(playerRef, getTransformType());
        Vector3d playerPos = playerTransform != null ? playerTransform.getPosition() : new Vector3d(0, 0, 0);

        // Get current drag rotation for spawning new entities
        GlyphRotation spawnRotation = mode.getDragRotation();
        if (spawnRotation == null) {
            spawnRotation = lookRotation != null ? lookRotation : new GlyphRotation(-9.0f, 0);
        }

        // Calculate spawn position from rotation
        Vector3d spawnPos = spawnRotation.toWorldPosition(playerPos);

        if (dropTarget != null && draggingGlyph != null && draggedElement instanceof GlyphEntity) {
            GlyphEntity draggedGlyphEntity = (GlyphEntity) draggedElement;
            OrbitalElement dropTargetElement = dropTarget.target;

            if (dropTarget instanceof HexDropTarget) {
                // Case A: Dropped onto a hex - add as child of targeted tier's node
                HexDropTarget hexTarget = (HexDropTarget) dropTarget;
                HexEntity targetHex = (HexEntity) hexTarget.target;
                HexNode targetNode = hexTarget.targetNode;

                if (targetNode != null) {
                    // Create new HexNode for dragged glyph
                    HexNode newNode = new HexNode(draggingGlyph);

                    // Add as child of targeted tier's node
                    targetNode.addChild(newNode);

                    // Consume dragged glyph - despawn and remove from orbital style
                    draggedGlyphEntity.despawn(commandBuffer);
                    mode.getOrbitalStyle().removeElement(draggedGlyphEntity);

                    // Refresh hex visuals to show new child
                    targetHex.refreshVisuals(commandBuffer);

                    // Set as active hex (most recent interaction)
                    mode.setActiveHex(targetHex);

                    success = true;
                    actionDesc = String.format("inserted at tier %d of hex '%s'",
                            hexTarget.tierLevel, targetHex.getId());
                } else {
                    actionDesc = "invalid hex target node";
                }
            } else if (dropTargetElement instanceof GlyphEntity) {
                // Case B: Dropped onto another glyph - target wraps dragged (B[A])
                GlyphEntity targetGlyphEntity = (GlyphEntity) dropTargetElement;
                GlyphInstance targetGlyph = targetGlyphEntity.getGlyph();

                if (canWrap(targetGlyph.getGlyph(), draggingGlyph.getGlyph())) {
                    // Create new hex with target as root, dragged as child
                    Hex newHex = new Hex();
                    HexNode rootNode = new HexNode(targetGlyph); // B is root
                    HexNode childNode = new HexNode(draggingGlyph); // A is child
                    rootNode.addChild(childNode);
                    newHex.setRoot(rootNode);

                    // Despawn both original GlyphEntities
                    draggedGlyphEntity.despawn(commandBuffer);
                    targetGlyphEntity.despawn(commandBuffer);
                    mode.getOrbitalStyle().removeElement(draggedGlyphEntity);
                    mode.getOrbitalStyle().removeElement(targetGlyphEntity);

                    // Spawn new HexEntity at the drop target's rotation
                    GlyphRotation targetRotation = targetGlyphEntity.getRotation();
                    HexEntity newHexEntity = new HexEntity(newHex, mode.getPlayer(), targetRotation);
                    newHexEntity.spawn(commandBuffer, spawnPos, playerId);
                    mode.getOrbitalStyle().addElement(newHexEntity);

                    // Set as active hex
                    mode.setActiveHex(newHexEntity);

                    success = true;
                    actionDesc = String.format("created hex %s[%s]",
                            targetGlyph.getGlyph().getDisplayName(),
                            draggingGlyph.getGlyph().getDisplayName());
                } else {
                    LOGGER.atInfo().log("Cannot wrap: %s cannot wrap %s",
                            targetGlyph.getGlyph().getDisplayName(),
                            draggingGlyph.getGlyph().getDisplayName());
                    actionDesc = "incompatible wrap";
                }
            } else if (dropTargetElement instanceof HexEntity) {
                // Dropped onto hex without tier info (fallback - insert at root)
                HexEntity hexEntity = (HexEntity) dropTargetElement;
                HexNode rootNode = hexEntity.getNodeAtTier(0);
                if (rootNode != null) {
                    HexNode newNode = new HexNode(draggingGlyph);
                    rootNode.addChild(newNode);

                    // Consume dragged glyph
                    draggedGlyphEntity.despawn(commandBuffer);
                    mode.getOrbitalStyle().removeElement(draggedGlyphEntity);

                    // Refresh and set as active
                    hexEntity.refreshVisuals(commandBuffer);
                    mode.setActiveHex(hexEntity);

                    success = true;
                    actionDesc = "inserted at root of hex '" + hexEntity.getId() + "'";
                }
            }
        } else if (draggingGlyph != null && draggedElement instanceof GlyphEntity) {
            // Case C: Dropped in empty space - convert to single-glyph hex
            GlyphEntity draggedGlyphEntity = (GlyphEntity) draggedElement;

            // Create single-glyph hex
            Hex newHex = new Hex();
            newHex.setRoot(new HexNode(draggingGlyph));

            // Despawn the GlyphEntity
            draggedGlyphEntity.despawn(commandBuffer);
            mode.getOrbitalStyle().removeElement(draggedGlyphEntity);

            // Spawn new HexEntity at the current look rotation
            HexEntity newHexEntity = new HexEntity(newHex, mode.getPlayer(), spawnRotation);
            newHexEntity.spawn(commandBuffer, spawnPos, playerId);
            mode.getOrbitalStyle().addElement(newHexEntity);

            // Set as active hex
            mode.setActiveHex(newHexEntity);

            success = true;
            actionDesc = "created single-glyph hex";
        } else if (draggedElement instanceof HexEntity) {
            // Dragging a hex (not a glyph) - update its rotation
            HexEntity draggedHex = (HexEntity) draggedElement;

            LOGGER.atInfo().log("Hex drag end - entityRef valid: %s, lookRotation: %s",
                    draggedHex.getEntityRef() != null && draggedHex.getEntityRef().isValid(),
                    lookRotation != null ? lookRotation.toString() : "null");

            // Update the hex's world position to the current look position
            if (spawnPos != null) {
                draggedHex.updateWorldPositionDirect(store, spawnPos);
            }

            // Capture the new rotation from look direction
            if (lookRotation != null) {
                draggedHex.captureRotationFromLook(store, lookRotation);
            }

            // Set as active hex (interaction = selection)
            mode.setActiveHex(draggedHex);

            // Log the final rotation
            GlyphRotation finalRotation = draggedHex.getRotation();
            LOGGER.atInfo().log("Hex '%s' final rotation: %s",
                    draggedHex.getId(), finalRotation);

            success = true;
            actionDesc = String.format("repositioned hex '%s'", draggedHex.getId());
        }

        if (success) {
            LOGGER.atInfo().log("Drag completed: %s", actionDesc);
        } else if (!actionDesc.isEmpty()) {
            LOGGER.atInfo().log("Drag failed: %s", actionDesc);
        }
    }

    /**
     * Check if a wrapper glyph can wrap a target glyph based on role rules.
     *
     * <p>
     * Wrapping rules:
     * <ul>
     * <li>EFFECT glyphs cannot wrap anything - they are always leaves</li>
     * <li>MODIFIER glyphs can wrap any glyph (they modify their child)</li>
     * <li>SELECT glyphs can wrap any glyph (they target their children)</li>
     * </ul>
     *
     * @param wrapper The glyph that will wrap (become outer shell)
     * @param target  The glyph that will be wrapped (become inner)
     * @return true if the wrap is valid
     */
    private boolean canWrap(Glyph wrapper, Glyph target) {
        if (wrapper == null || target == null) {
            return false;
        }

        // MODIFIERs and SELECTs can wrap anything
        return true;
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
