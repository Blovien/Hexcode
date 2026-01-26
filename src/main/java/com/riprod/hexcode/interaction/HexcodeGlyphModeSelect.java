package com.riprod.hexcode.interaction;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

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
import com.hypixel.hytale.server.core.meta.DynamicMetaStore;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.IInteractionSimulationHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.ChargingInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.Collector;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.casting.IntersectionObserver;
import com.riprod.hexcode.casting.IntersectionObserver.DropTarget;
import com.riprod.hexcode.casting.IntersectionObserver.HexDropTarget;
import com.riprod.hexcode.casting.styles.BaseGlyphStyle;
import com.riprod.hexcode.casting.styles.OrbitalElement;
import com.riprod.hexcode.casting.styles.RingGlyphStyle;
import com.riprod.hexcode.data.GlyphInstance;
import com.riprod.hexcode.entity.GlyphComponent;
import com.riprod.hexcode.entity.GlyphEntity;
import com.riprod.hexcode.entity.HexEntity;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphRole;
import com.riprod.hexcode.hex.HexNode;
import com.riprod.hexcode.mode.CompositionState;
import com.riprod.hexcode.mode.GlyphMode;
import com.riprod.hexcode.mode.GlyphModeManager;
import com.riprod.hexcode.util.HexStaffUtil;
import com.riprod.hexcode.util.RaycastUtil;
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

    /** Selection range for raycast glyph detection */
    private static final float SELECTION_RANGE = 10.0f;

    private static final float CHARGING_HELD = -1.0F; // Still holding
    private static final float CHARGING_CANCELED = -2.0F; // Cancelled

    private final GlyphRenderer glyphRenderer;
    private final Map<UUID, TrailEffect> activeTrails;
    private final IntersectionObserver intersectionObserver;

    // Cached component types for performance (lazy initialization)
    private static volatile ComponentType<EntityStore, TransformComponent> transformComponentType;
    private static volatile ComponentType<EntityStore, HeadRotation> headRotationComponentType;
    private static volatile ComponentType<EntityStore, GlyphComponent> orbitalGlyphComponentType;

    public static final BuilderCodec<HexcodeGlyphModeSelect> CODEC = BuilderCodec.builder(
            HexcodeGlyphModeSelect.class,
            HexcodeGlyphModeSelect::new,
            Interaction.ABSTRACT_CODEC).build();

    public HexcodeGlyphModeSelect() {
        this.glyphRenderer = new GlyphRenderer();
        this.activeTrails = new ConcurrentHashMap<>();
        this.intersectionObserver = new IntersectionObserver();
    }

    public HexcodeGlyphModeSelect(String id) {
        this.glyphRenderer = new GlyphRenderer();
        this.activeTrails = new ConcurrentHashMap<>();
        this.intersectionObserver = new IntersectionObserver();
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
        return WaitForDataFrom.Server;
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

        InteractionSyncData syncData = context.getClientState();

        if (syncData != null) {

            if (syncData.chargeValue == CHARGING_CANCELED || syncData.chargeValue != CHARGING_HELD) {
                // Interaction was cancelled
                handleDragEnd(playerId, ref, store, mode);
                LOGGER.atInfo().log("HexcodeGlyphAction: Interaction cancelled");
                context.getState().state = InteractionState.Finished;
                return;
            }
        }
        if (inGlyphMode) {
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

            if (mode.isDragging()) {
                updateDragPosition(ref, store, mode);
            }
            context.getState().state = InteractionState.NotFinished;
            return;
        }
    }

    @Override
    protected void simulateTick0(
            boolean firstRun,
            float time,
            @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler) {

        // if the player is in Glyph Mode - Select
        GlyphModeManager modeManager = GlyphModeManager.getInstance();
        UUID playerId = context.getCommandBuffer().getComponent(context.getEntity(), UUIDComponent.getComponentType())
                .getUuid();
        GlyphMode mode = modeManager.getSession(playerId);
        boolean inGlyphMode = mode != null && mode.isActive();

        if (!inGlyphMode) {
            context.getState().state = InteractionState.Finished;
            return;
        }

        DynamicMetaStore<InteractionContext> metaStore = context.getMetaStore();

        Ref<EntityStore> target = metaStore.getMetaObject(Interaction.TARGET_ENTITY);

        // target does not exist
        if (target == null || !target.isValid()) {
            context.getState().state = InteractionState.Failed;
            LOGGER.atInfo().log("HexcodeGlyphAction Simulation: No valid target entity");
            return;
        }

        // Handle targeted glyph
        if (!target.getClass().equals(GlyphEntity.class) || !target.getClass().equals(HexEntity.class)) {
            // Not a glyph or hex entity
            context.getState().state = InteractionState.Failed;
            LOGGER.atInfo().log("HexcodeGlyphAction Simulation: Target is not a glyph or hex entity");
            return;
        }

        Player player = context.getCommandBuffer().getComponent(context.getEntity(), Player.getComponentType());

        Inventory inventory = player.getInventory();
        if (!HexStaffUtil.hasHexcodeEquipment(inventory)) {
            // Missing required equipment
            LOGGER.atInfo().log("HexcodeGlyphAction: Wrong Equipment");
            context.getState().state = InteractionState.Failed;
            // exit glyph mode
            modeManager.removeSession(playerId);
            return;
        }

        // Select and update drag position
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
     * Left-click finds the orbital glyph the player is looking at,
     * selects it, and starts dragging in one action.
     *
     * <p>
     * Uses IntersectionObserver for ray-sphere intersection targeting,
     * which handles both single glyphs and saved hexes uniformly.
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
        // Get orbital elements from the style system
        List<OrbitalElement> orbitalElements = mode.getAllOrbitalElements();

        if (orbitalElements == null || orbitalElements.isEmpty()) {
            LOGGER.atWarning().log("Player %s has no orbital elements to select", playerId);
            return;
        }

        LOGGER.atInfo().log("Player %s attempting to select from %d orbital elements", playerId,
                orbitalElements.size());

        Store<EntityStore> store = commandBuffer.getStore();

        DropTarget dropTarget = intersectionObserver.findDropTarget(store, playerRef, orbitalElements, SELECTION_RANGE);

        if (dropTarget == null) {
            LOGGER.atInfo().log("Player %s clicked but no orbital element in range", playerId);
            return;
        }

        OrbitalElement targetElement = dropTarget.target;

        // Don't select if already being dragged
        if (targetElement.isDragging()) {
            return;
        }

        // Get glyph information for logging
        String elementName = targetElement.isSavedHex() ? "hex" : "glyph";
        LOGGER.atInfo().log("Player %s selected and started dragging %s '%s'",
                playerId, elementName, targetElement.getId());

        // Select this element (updates hover state and visual)
        mode.setHoveredOrbitalElement(targetElement, store);

        // Mark element as dragging and exclude from orbital rotation
        targetElement.setDragging(true);
        BaseGlyphStyle style = mode.getOrbitalStyle();
        style.startDrag(targetElement);

        // Synchronize with ECS component for system tick updates
        syncDragStateToComponent(store, targetElement, true);

        // Get drag start position using player transform and head rotation
        TransformComponent transform = commandBuffer.getComponent(playerRef, getTransformType());
        HeadRotation headRotation = commandBuffer.getComponent(playerRef, getHeadRotationType());
        Vector3d startPos = (transform != null && headRotation != null)
                ? RaycastUtil.getPointAlongLookRay(transform, headRotation, 2.5f)
                : new Vector3d(0, 0, 0);

        // Start dragging in GlyphMode - track the element being dragged
        mode.startDragElement(targetElement, startPos);

        // Start trail effect with element's visual color
        int color = targetElement.getVisual().getColor();
        TrailEffect trail = new TrailEffect(color, 20, 0.5f);
        trail.start();
        activeTrails.put(playerId, trail);

        // Start drag trail visual
        glyphRenderer.startDragTrail(store, playerRef, color);
    }

    /**
     * Update the position of the dragged element to follow the player's look
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
            return;
        }

        // Calculate new position along look ray
        Vector3d newPosition = RaycastUtil.calculateDragPosition(store, playerRef, mode.getDragDistance());
        if (newPosition == null) {
            return;
        }

        // Update the element's world position
        draggedElement.updateWorldPositionDirect(store, newPosition);

        // Update GlyphMode's tracked drag position
        mode.updateDrag(newPosition);
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
     * Drop behavior:
     * <ul>
     * <li>Drop on single glyph: Wrap it (dragged wraps target)</li>
     * <li>Drop on hex (tier-based): Insert as child of aimed tier's node</li>
     * <li>Drop on empty space with empty composition: Place as root</li>
     * <li>Drop on empty space with existing composition: Wrap the current root</li>
     * </ul>
     *
     * <p>
     * Uses IntersectionObserver for tier-based drop detection on hexes.
     * The tier determines which HexNode becomes the parent of the dropped glyph.
     */
    private void handleDragEnd(UUID playerId, Ref<EntityStore> playerRef, Store<EntityStore> store, GlyphMode mode) {
        if (!mode.isDragging()) {
            return;
        }

        GlyphInstance draggingGlyph = mode.getDraggingGlyph();
        CompositionState composition = mode.getComposition();
        BaseGlyphStyle style = mode.getOrbitalStyle();

        // Get all orbital elements for targeting
        List<OrbitalElement> orbitalElements = mode.getAllOrbitalElements();

        // Find what the player is aiming at using IntersectionObserver
        DropTarget dropTarget = intersectionObserver.findDropTarget(store, playerRef, orbitalElements, SELECTION_RANGE);

        boolean success = false;
        String actionDesc = "";

        if (dropTarget != null) {
            OrbitalElement targetElement = dropTarget.target;

            if (dropTarget instanceof HexDropTarget) {
                // Dropped onto a hex - tier-based insertion
                HexDropTarget hexTarget = (HexDropTarget) dropTarget;
                HexNode targetNode = hexTarget.targetNode;

                if (targetNode != null && draggingGlyph != null) {
                    // Insert the glyph as a child of the aimed tier's node
                    success = composition.insertAtNode(draggingGlyph, targetNode);
                    if (success) {
                        actionDesc = String.format("inserted at tier %d of hex '%s'",
                                hexTarget.tierLevel, targetElement.getId());
                    } else {
                        actionDesc = "failed to insert at tier";
                    }
                } else {
                    actionDesc = "invalid hex target node";
                }
            } else if (targetElement instanceof GlyphEntity) {
                // Dropped onto single glyph - wrap it
                GlyphEntity targetGlyphEntity = (GlyphEntity) targetElement;
                GlyphInstance targetGlyph = targetGlyphEntity.getGlyph();

                if (draggingGlyph != null && canWrap(draggingGlyph.getGlyph(), targetGlyph.getGlyph())) {
                    // First, ensure the target is in the composition
                    if (composition.isEmpty()) {
                        composition.placeRoot(targetGlyph);
                    }

                    // Wrap the composition root with the dragged glyph
                    success = composition.wrapNode(draggingGlyph, composition.getHex().getRoot());
                    if (success) {
                        actionDesc = "wrapped '" + targetGlyph.getGlyph().getDisplayName() + "'";
                    }
                } else if (draggingGlyph != null) {
                    LOGGER.atInfo().log("Cannot wrap: %s cannot wrap %s (role incompatibility)",
                            draggingGlyph.getGlyph().getDisplayName(),
                            targetGlyph.getGlyph().getDisplayName());
                    actionDesc = "incompatible wrap";
                }
            } else if (targetElement instanceof HexEntity) {
                // Dropped onto hex without tier info (shouldn't happen, but handle gracefully)
                HexEntity hexEntity = (HexEntity) targetElement;
                HexNode rootNode = hexEntity.getNodeAtTier(0);
                if (rootNode != null && draggingGlyph != null) {
                    success = composition.insertAtNode(draggingGlyph, rootNode);
                    if (success) {
                        actionDesc = "inserted at root of hex '" + hexEntity.getId() + "'";
                    }
                }
            }
        } else if (draggingGlyph != null) {
            // Dropped in empty space
            if (composition.isEmpty()) {
                // Empty composition - place as root
                success = composition.placeRoot(draggingGlyph);
                actionDesc = "placed as root";
            } else {
                // Existing composition - wrap the current root
                HexNode currentRoot = composition.getHex().getRoot();
                if (currentRoot != null && canWrap(draggingGlyph.getGlyph(), currentRoot.getValue().getGlyph())) {
                    success = composition.wrapNode(draggingGlyph, currentRoot);
                    actionDesc = "wrapped composition root";
                } else {
                    LOGGER.atInfo().log("Cannot wrap existing composition with %s",
                            draggingGlyph.getGlyph().getDisplayName());
                    actionDesc = "cannot wrap existing composition";
                }
            }
        }

        if (success && draggingGlyph != null) {
            LOGGER.atInfo().log("Glyph '%s' %s in composition",
                    draggingGlyph.getGlyph().getDisplayName(), actionDesc);
        } else if (!actionDesc.isEmpty() && draggingGlyph != null) {
            LOGGER.atInfo().log("Failed to add glyph '%s' - %s",
                    draggingGlyph.getGlyph().getDisplayName(), actionDesc);
        }

        // End dragging on the dragged element and return to orbit
        OrbitalElement draggedElement = mode.getDraggingElement();
        if (draggedElement != null) {
            draggedElement.setDragging(false);
            if (style instanceof RingGlyphStyle) {
                ((RingGlyphStyle) style).endDrag(draggedElement, store, success);
            } else {
                style.includeInOrbit(draggedElement);
            }
            // Sync ECS component drag state
            syncDragStateToComponent(store, draggedElement, false);
        }

        // End dragging in GlyphMode
        mode.endDrag();

        // Stop trail effect
        cleanupTrailEffect(playerId);
        glyphRenderer.stopDragTrail(store, playerRef);
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

        GlyphRole wrapperRole = wrapper.getRole();

        // EFFECTs cannot wrap anything - they are always leaves
        if (wrapperRole == GlyphRole.EFFECT) {
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
