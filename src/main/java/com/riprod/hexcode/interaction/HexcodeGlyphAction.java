package com.riprod.hexcode.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
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
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.Collector;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.data.GlyphInstance;
import com.riprod.hexcode.execution.HexExecutor;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.mode.CompositionState;
import com.riprod.hexcode.mode.GlyphMode;
import com.riprod.hexcode.mode.GlyphModeManager;
import com.riprod.hexcode.util.HexMathUtil;
import com.riprod.hexcode.util.HexStaffUtil;
import com.riprod.hexcode.util.RaycastUtil;
import com.riprod.hexcode.visual.GlyphRenderer;
import com.riprod.hexcode.visual.TrailEffect;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    private final HexExecutor hexExecutor;
    private final GlyphRenderer glyphRenderer;
    private final Map<UUID, TrailEffect> activeTrails;

    public static final BuilderCodec<HexcodeGlyphAction> CODEC = BuilderCodec.builder(
            HexcodeGlyphAction.class,
            HexcodeGlyphAction::new,
            Interaction.ABSTRACT_CODEC
    ).build();

    public HexcodeGlyphAction() {
        this.hexExecutor = new HexExecutor();
        this.glyphRenderer = new GlyphRenderer();
        this.activeTrails = new HashMap<>();
    }

    public HexcodeGlyphAction(String id) {
        super(id);
        this.hexExecutor = new HexExecutor();
        this.glyphRenderer = new GlyphRenderer();
        this.activeTrails = new HashMap<>();
    }

    @Nonnull
    @Override
    public WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.None;
    }

    @Override
    protected void tick0(
            boolean firstRun,
            float time,
            @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler
    ) {
        Ref<EntityStore> ref = context.getEntity();
        if (!ref.isValid()) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Store<EntityStore> store = ref.getStore();

        // Get player from entity
        Player player = context.getCommandBuffer().getComponent(ref, Player.getComponentType());
        if (player == null) {
            LOGGER.atFine().log("HexcodeGlyphAction: No Player component found");
            context.getState().state = InteractionState.Failed;
            return;
        }

        // Get player UUID
        UUIDComponent uuidComponent = context.getCommandBuffer().getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            LOGGER.atFine().log("HexcodeGlyphAction: No UUID component found");
            context.getState().state = InteractionState.Failed;
            return;
        }
        UUID playerId = uuidComponent.getUuid();

        // Check equipment requirements
        Inventory inventory = player.getInventory();
        if (!HexStaffUtil.hasHexcodeEquipment(inventory)) {
            // Missing required equipment
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
                handleDragStart(playerId, ref, store, mode);
            }
            // Keep interaction running while dragging
            if (mode.isDragging()) {
                context.getState().state = InteractionState.NotFinished;
            }
        } else {
            // Not in glyph mode: cast hex on first tick
            if (firstRun) {
                if (mode != null) {
                    CompositionState composition = mode.getComposition();
                    if (composition != null && !composition.isEmpty()) {
                        castHex(playerId, ref, store, mode);
                        LOGGER.atInfo().log("Player %s cast hex via Primary", playerId);
                    } else {
                        LOGGER.atFine().log("No hex composed to cast");
                    }
                } else {
                    LOGGER.atFine().log("No glyph mode session - enter glyph mode first to compose a hex");
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
            @Nonnull InteractionContext context
    ) {
        super.handle(ref, firstRun, time, type, context);

        // Called when interaction ends (mouse released)
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
            @Nonnull CooldownHandler cooldownHandler
    ) {
        // Client-side prediction (optional)
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
        return false;
    }

    /**
     * Handle glyph drag start.
     */
    private void handleDragStart(UUID playerId, Ref<EntityStore> playerRef, Store<EntityStore> store, GlyphMode mode) {
        // Check if player is looking at a glyph in the orbital ring
        GlyphInstance hoveredGlyph = mode.getHoveredGlyph();
        if (hoveredGlyph == null) {
            LOGGER.atFine().log("No glyph hovered to drag");
            return;
        }

        // Get player position for drag start
        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }
        Vector3d startPos = new Vector3d(
                transform.getPosition().x,
                transform.getPosition().y + 1.5,
                transform.getPosition().z
        );

        // Start dragging
        mode.startDrag(hoveredGlyph, startPos);
        LOGGER.atInfo().log("Started dragging glyph '%s'", hoveredGlyph.getGlyph().getDisplayName());

        // Start trail effect
        int color = hoveredGlyph.getGlyph().getVisual().getColor();
        TrailEffect trail = new TrailEffect(color, 20, 0.5f);
        trail.start();
        activeTrails.put(playerId, trail);

        // Start drag trail visual
        glyphRenderer.startDragTrail(store, playerRef, color);
    }

    /**
     * Handle glyph drag end.
     */
    private void handleDragEnd(UUID playerId, Ref<EntityStore> playerRef, Store<EntityStore> store, GlyphMode mode) {
        if (!mode.isDragging()) {
            return;
        }

        GlyphInstance draggingGlyph = mode.getDraggingGlyph();
        Vector3d dropPosition = mode.getDragPosition();

        // Determine what to do with the dropped glyph
        CompositionState composition = mode.getComposition();

        // Check if dropping in crafting space
        if (isInCraftingSpace(mode, dropPosition, store)) {
            // Try to add the glyph to composition
            boolean added = composition.addLeaf(draggingGlyph, composition.getHex().getRoot());
            if (added) {
                LOGGER.atInfo().log("Added glyph '%s' to composition", draggingGlyph.getGlyph().getDisplayName());
            } else {
                LOGGER.atInfo().log("Failed to add glyph '%s' - invalid placement", draggingGlyph.getGlyph().getDisplayName());
            }
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

        // Calculate mana cost
        float playerMana = getPlayerMana(store, playerRef);

        // Get cast origin and direction
        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }
        Vector3d direction = RaycastUtil.getPlayerLookDirection(transform);

        consumePlayerMana(playerRef, 999);
        LOGGER.atInfo().log("Consumed %d mana (power: %.0f%%)", 999, playerMana);

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
     * Check if a position is within the crafting space.
     */
    private boolean isInCraftingSpace(GlyphMode mode, Vector3d position, Store<EntityStore> store) {
        if (position == null) {
            return false;
        }

        Ref<EntityStore> playerRef = mode.getPlayer();
        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null) {
            return true; // Fallback
        }

        Vector3d playerPos = transform.getPosition();
        float craftingSpaceDistance = mode.getCraftingSpaceDistance();

        // Calculate crafting space center (in front of player)
        Vector3d lookDir = RaycastUtil.getPlayerLookDirection(transform);
        Vector3d craftingCenter = new Vector3d(
                playerPos.x + lookDir.x * craftingSpaceDistance,
                playerPos.y + 1.5 + lookDir.y * craftingSpaceDistance,
                playerPos.z + lookDir.z * craftingSpaceDistance
        );

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
