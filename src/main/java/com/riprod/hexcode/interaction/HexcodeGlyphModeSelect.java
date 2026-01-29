package com.riprod.hexcode.interaction;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
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
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.meta.MetaKey;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.ChargingInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.Collector;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.casting.RotationObserver;
import com.riprod.hexcode.casting.RotationObserver.DropTarget;
import com.riprod.hexcode.casting.RotationObserver.NodeDropTarget;
import com.riprod.hexcode.entity.HexNodeEntity;
import com.riprod.hexcode.hex.HexNode;
import com.riprod.hexcode.math.GlyphRotation;
import com.riprod.hexcode.mode.GlyphMode;
import com.riprod.hexcode.mode.GlyphModeManager;
import com.riprod.hexcode.util.HexStaffUtil;
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

    // MetaKeys to track state across ticks (same pattern as HexcodeGlyphModeToggle)
    private static final MetaKey<Float> PREVIOUS_TIME = Interaction.META_REGISTRY.registerMetaObject(i -> 0.0f);
    private static final MetaKey<Boolean> DRAG_STARTED = Interaction.META_REGISTRY.registerMetaObject(i -> false);

    private final GlyphRenderer glyphRenderer;
    private final Map<UUID, TrailEffect> activeTrails;
    private final RotationObserver rotationObserver;

    // Cached component types for performance (lazy initialization)
    private static volatile ComponentType<EntityStore, TransformComponent> transformComponentType;
    private static volatile ComponentType<EntityStore, HeadRotation> headRotationComponentType;
    private static volatile ComponentType<EntityStore, MountedComponent> mountedComponentType;

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

    /** Get cached MountedComponent type (for detaching during drag) */
    private static ComponentType<EntityStore, MountedComponent> getMountedType() {
        if (mountedComponentType == null) {
            mountedComponentType = MountedComponent.getComponentType();
        }
        return mountedComponentType;
    }

    @Nonnull
    @Override
    public WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.Server; // Server controls timing (same as Toggle)
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

        // Check if we've already started dragging (persisted across ticks)
        boolean dragStarted = context.getInstanceStore().getMetaObject(DRAG_STARTED);

        // In glyph mode: handle drag/drop
        if (firstRun && !dragStarted) {
            // First tick = press, start drag
            handleDragStart(playerId, ref, context.getCommandBuffer(), mode);

            // Mark that we started dragging
            if (mode.isDragging()) {
                context.getInstanceStore().putMetaObject(DRAG_STARTED, true);
            } else {
                // Nothing was selected to drag - finish immediately
                context.getState().state = InteractionState.Finished;
                return;
            }
        }

        // Update drag position while dragging
        if (mode.isDragging()) {
            updateDragPosition(ref, store, mode);
        }

        // Guard against null client state (can happen on first tick before client syncs)
        if (context.getClientState() == null) {
            context.getState().state = InteractionState.NotFinished;
            return;
        }

        // Let parent handle charging state machine
        // This reads chargeValue from client and sets Finished when released
        super.tick0(firstRun, time, type, context, cooldownHandler);

        // If parent set Finished, handle drag end
        if (context.getState().state == InteractionState.Finished && mode.isDragging()) {
            handleDragEnd(playerId, ref, context.getCommandBuffer(), mode);
        }
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
     * <p>Unified treatment: both single glyphs and composed hexes are HexNodeEntity.
     * On drag start:
     * <ul>
     *   <li>Get hovered HexNodeEntity from mode</li>
     *   <li>Unmount from player (detach MountedComponent)</li>
     *   <li>Set entity.setDragging(true)</li>
     *   <li>Start tracking drag state</li>
     * </ul>
     *
     * @param playerId      Player's UUID
     * @param playerRef     Player entity reference
     * @param commandBuffer Command buffer for entity operations
     * @param mode          Active glyph mode session
     */
    private void handleDragStart(UUID playerId, Ref<EntityStore> playerRef,
            CommandBuffer<EntityStore> commandBuffer, GlyphMode mode) {

        // Use pre-computed hovered element from hover detection in Toggle
        HexNodeEntity targetElement = mode.getHoveredElement();

        if (targetElement == null) {
            return;
        }

        // Don't select if already being dragged
        if (targetElement.isDragging()) {
            return;
        }

        Store<EntityStore> store = commandBuffer.getStore();

        // Get element information for logging
        String elementName = targetElement.isSavedHex() ? "hex" : "glyph";
        LOGGER.atInfo().log("Player %s selected and started dragging %s '%s'",
                playerId, elementName, targetElement.getId());

        // Select this element (updates hover state and visual)
        mode.setHoveredOrbitalElement(targetElement, store);

        // Mark element as dragging
        targetElement.setDragging(true);

        // Pause orbital tracking so element can be positioned freely during drag
        targetElement.pauseOrbitalTracking(store);

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
     * Update the position of the dragged element to follow the player's look direction.
     * Called every tick while dragging to provide smooth visual feedback.
     *
     * <p>Since the element is unmounted during drag, we directly set its world position
     * based on the player's look rotation and position.
     *
     * @param playerRef Player entity reference
     * @param store     The entity store
     * @param mode      The glyph mode with drag state
     */
    private void updateDragPosition(Ref<EntityStore> playerRef, Store<EntityStore> store, GlyphMode mode) {
        HexNodeEntity draggedElement = mode.getDraggingElement();
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

        // Update the element's world position directly (unmounted during drag)
        draggedElement.updateWorldPositionDirect(store, newPosition);

        // Update GlyphMode's tracked drag rotation
        mode.updateDrag(lookRotation);
    }

    /**
     * Handle drag end with unified drop target detection.
     *
     * <p>Unified treatment: all elements are HexNodeEntity (single glyphs = leaf nodes).
     *
     * <p>Drop behavior:
     * <ul>
     *   <li>Case 1 (Hex onto Hex): Insert dragged tree into target's deepest matched node</li>
     *   <li>Case 2 (Glyph onto Hex): Same as Case 1 (glyph is just a leaf HexNode)</li>
     *   <li>Case 3 (Glyph onto Glyph): Target becomes parent, dragged becomes child</li>
     *   <li>Case 4 (Drop in empty space): Remount to player at current look rotation</li>
     * </ul>
     *
     * @param playerId      Player's UUID
     * @param playerRef     Player entity reference
     * @param commandBuffer Command buffer for entity operations
     * @param mode          Active glyph mode session
     */
    private void handleDragEnd(UUID playerId, Ref<EntityStore> playerRef, CommandBuffer<EntityStore> commandBuffer,
            GlyphMode mode) {
        if (!mode.isDragging()) {
            return;
        }

        Store<EntityStore> store = commandBuffer.getStore();
        HexNodeEntity draggedElement = mode.getDraggingElement();

        if (draggedElement == null) {
            mode.endDrag(store);
            return;
        }

        // Get current look rotation for drop
        GlyphRotation lookRotation = rotationObserver.getPlayerLookRotation(store, playerRef);
        if (lookRotation == null) {
            lookRotation = mode.getDragRotation();
        }

        // Get player position for spawn position calculation
        TransformComponent playerTransform = commandBuffer.getComponent(playerRef, getTransformType());
        Vector3d playerPos = playerTransform != null ? playerTransform.getPosition() : new Vector3d(0, 0, 0);

        // Use rotation-based drop target detection with nested node targeting
        DropTarget rawDropTarget = rotationObserver.findDropTarget(store, playerRef,
                mode.getAllOrbitalElements());

        // Cast to NodeDropTarget if available (all elements are HexNodeEntity now)
        NodeDropTarget dropTarget = (rawDropTarget instanceof NodeDropTarget)
                ? (NodeDropTarget) rawDropTarget : null;

        boolean success = false;
        String actionDesc = "";

        if (dropTarget != null && dropTarget.entity != draggedElement) {
            // Dropped onto another element
            HexNodeEntity targetEntity = dropTarget.entity;
            HexNode targetNode = dropTarget.targetNode;
            HexNode draggedNode = draggedElement.getNode();

            if (targetNode != null && draggedNode != null) {
                // Case 1/2/3: Drop onto another element - add dragged as child of target
                // Detach dragged node from its parent (if any)
                if (draggedNode.getParent() != null) {
                    draggedNode.getParent().removeChild(draggedNode);
                }

                // Add dragged node as child of target node
                targetNode.addChild(draggedNode);

                // Despawn the dragged entity (its node is now part of target's tree)
                draggedElement.despawn(commandBuffer);
                mode.getOrbitalStyle().removeElement(draggedElement);

                // Refresh target entity to show new structure
                targetEntity.refreshAndRespawn(commandBuffer, mode.getPlayer());

                // Set target as active hex
                mode.setActiveHex(targetEntity);

                success = true;
                String draggedDesc = draggedElement.isSavedHex() ? "hex" : "glyph";
                String targetDesc = targetEntity.isSavedHex() ? "hex" : "glyph";
                actionDesc = String.format("added %s '%s' to %s '%s'",
                        draggedDesc, draggedElement.getId(),
                        targetDesc, targetEntity.getId());
            }
        }

        if (!success) {
            // Case 4: Dropped in empty space - resume orbital tracking at current rotation
            if (lookRotation != null) {
                // Update the node's absolute position
                HexNode node = draggedElement.getNode();
                node.setAbsoluteYaw(lookRotation.getYaw());
                node.setAbsolutePitch(lookRotation.getPitch());
                node.recalculateLayout();

                // Resume orbital tracking at new rotation
                draggedElement.resumeOrbitalTracking(store, mode.getPlayer());

                // Set as active hex
                mode.setActiveHex(draggedElement);

                success = true;
                String elementDesc = draggedElement.isSavedHex() ? "hex" : "glyph";
                actionDesc = String.format("repositioned %s '%s' at yaw=%.1f, pitch=%.1f",
                        elementDesc, draggedElement.getId(),
                        lookRotation.getYaw(), lookRotation.getPitch());
            }
        }

        // Clear drag state
        draggedElement.setDragging(false);
        mode.endDrag(store);

        // Stop trail effect
        cleanupTrailEffect(playerId);
        glyphRenderer.stopDragTrail(store, playerRef);

        if (success) {
            LOGGER.atInfo().log("Drag completed: %s", actionDesc);
        } else {
            LOGGER.atInfo().log("Drag ended without action");
        }
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
