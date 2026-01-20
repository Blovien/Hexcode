package com.riprod.hexcode.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.HexExecutor;
import com.riprod.hexcode.execution.ManaCostCalculator;
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
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.MouseButtonEvent;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event handlers for the Hexcode mod.
 *
 * Handles:
 * - Glyph mode toggle on right-click with staff
 * - Glyph drag start/end events
 * - Hex casting with mana consumption
 * - Stamina drain while in mode
 * - Mode exit conditions
 * - Player disconnect cleanup
 */
public class EventHandlers {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final GlyphModeManager modeManager;
    private final HexExecutor hexExecutor;
    private final ManaCostCalculator costCalculator;
    private final GlyphRenderer glyphRenderer;
    private final Map<UUID, TrailEffect> activeTrails;

    public EventHandlers() {
        this.modeManager = GlyphModeManager.getInstance();
        this.hexExecutor = new HexExecutor();
        this.costCalculator = new ManaCostCalculator();
        this.glyphRenderer = new GlyphRenderer();
        this.activeTrails = new HashMap<>();
    }

    /**
     * Register all event handlers.
     *
     * @param eventRegistry The event registry
     */
    public void register(EventRegistry eventRegistry) {
        registerMouseButtonHandler(eventRegistry);
        registerDisconnectHandler(eventRegistry);
    }

    /**
     * Handle mouse button events for glyph mode toggle.
     */
    private void registerMouseButtonHandler(EventRegistry eventRegistry) {
        eventRegistry.register(PlayerMouseButtonEvent.class, event -> {
            try {
                LOGGER.atInfo().log("Handling mouse button event");
                handleMouseButton(event);
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Error handling mouse button event");
            }
        });
    }

    private void handleMouseButton(PlayerMouseButtonEvent event) {
        Ref<EntityStore> playerRef = event.getPlayerRef();
        Store<EntityStore> store = playerRef.getStore();

        // Get player UUID first
        UUIDComponent uuidComponent = store.getComponent(playerRef, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            return;
        }
        UUID playerId = uuidComponent.getUuid();

        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            return;
        }

        GlyphMode mode = modeManager.getSession(playerId);
        boolean inGlyphMode = mode != null && mode.isActive();

        var eventButton = event.getMouseButton();

        // Handle based on mouse button
        if (eventButton.mouseButtonType == MouseButtonType.Right) {
            handleRightClick(event, player, playerId, playerRef, store, mode, inGlyphMode);
        } else if (eventButton.mouseButtonType == MouseButtonType.Left) {
            handleLeftClick(event, player, playerId, playerRef, store, mode, inGlyphMode);
        }
    }

    /**
     * Handle right-click: toggle glyph mode or cast hex.
     */
    private void handleRightClick(PlayerMouseButtonEvent event, Player player, UUID playerId,
            Ref<EntityStore> playerRef, Store<EntityStore> store,
            GlyphMode mode, boolean inGlyphMode) {
        Inventory inventory = player.getInventory();
        if (!HexStaffUtil.hasHexStaffInOffhand(inventory)) {
            return;
        }

        if (inGlyphMode) {
            // Already in glyph mode - check if we should cast
            CompositionState composition = mode.getComposition();
            if (!composition.isEmpty()) {
                // Cast the hex
                castHex(playerId, playerRef, store, mode);
            }
            // Exit glyph mode
            modeManager.exitGlyphMode(playerId);
            LOGGER.atInfo().log("Player %s exited glyph mode", playerId);
            cleanupTrailEffect(playerId);
        } else {
            // Enter glyph mode
            modeManager.toggleGlyphMode(playerId, playerRef, null);
            LOGGER.atInfo().log("Player %s entered glyph mode", playerId);
        }

        event.setCancelled(true);
    }

    /**
     * Handle left-click: drag start/end in glyph mode.
     */
    private void handleLeftClick(PlayerMouseButtonEvent event, Player player, UUID playerId,
            Ref<EntityStore> playerRef, Store<EntityStore> store,
            GlyphMode mode, boolean inGlyphMode) {
        if (!inGlyphMode) {
            return;
        }

        if (event.getMouseButton().state == MouseButtonState.Pressed) {
            handleDragStart(playerId, playerRef, store, mode);
        } else {
            handleDragEnd(playerId, playerRef, store, mode);
        }

        event.setCancelled(true);
    }

    /**
     * Handle glyph drag start.
     */
    private void handleDragStart(UUID playerId, Ref<EntityStore> playerRef, Store<EntityStore> store, GlyphMode mode) {
        // Check if player is looking at a glyph in the orbital ring
        Glyph hoveredGlyph = mode.getHoveredGlyph();
        if (hoveredGlyph == null) {
            LOGGER.atInfo().log("No glyph hovered to drag");
            return;
        }

        // Get player position for drag start
        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }
        Vector3d startPos = new Vector3d(transform.getPosition().x, transform.getPosition().y + 1.5,
                transform.getPosition().z);

        // Start dragging
        mode.startDrag(hoveredGlyph, startPos);
        LOGGER.atInfo().log("Started dragging glyph '%s'", hoveredGlyph.getDisplayName());

        // Start trail effect
        int color = hoveredGlyph.getVisual().getColor();
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

        Glyph draggingGlyph = mode.getDraggingGlyph();
        Vector3d dropPosition = mode.getDragPosition();

        // Determine what to do with the dropped glyph
        CompositionState composition = mode.getComposition();

        // Check if dropping in crafting space
        if (isInCraftingSpace(mode, dropPosition, store)) {
            // Try to add the glyph to composition
            boolean added;
            if (composition.isEmpty()) {
                // First glyph becomes root
                added = composition.placeAsRoot(draggingGlyph);
            } else {
                // TODO: Determine if wrapping or adding sibling based on drop position
                // For now, wrap the root node
                added = composition.wrapNode(draggingGlyph, composition.getHex().getRoot());
            }
            if (added) {
                LOGGER.atInfo().log("Added glyph '%s' to composition", draggingGlyph.getDisplayName());
                // Show cost preview
                showCostPreview(mode, store, playerRef);
            } else {
                LOGGER.atInfo().log("Failed to add glyph '%s' - invalid placement", draggingGlyph.getDisplayName());
                // Visual feedback for invalid action
                showInvalidActionFeedback(store, playerRef);
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

        if (hex.isEmpty() || !hex.isValid()) {
            LOGGER.atInfo().log("Failed to cast hex - empty or invalid composition");
            return;
        }

        // Calculate mana cost
        int manaCost = costCalculator.calculateBaseCost(hex);
        float playerMana = getPlayerMana(store, playerRef);

        // Check if player can cast
        ManaCostCalculator.CastResult castResult = costCalculator.canCast(playerMana, manaCost);

        LOGGER.atInfo().log("Hex mana cost: %d, player mana: %.1f, can cast: %b", manaCost, playerMana, castResult.canCast());

        if (!castResult.canCast()) {
            LOGGER.atInfo().log("Not enough mana to cast");
            // Show insufficient mana feedback
            showInsufficientManaFeedback(store, playerRef);
            return;
        }

        // Get cast origin and direction
        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }
        Vector3d direction = RaycastUtil.getPlayerLookDirection(transform);

        // Consume mana
        int manaConsumed = castResult.getManaCost();
        consumePlayerMana(playerRef, manaConsumed);
        LOGGER.atInfo().log("Consumed %d mana (power: %.0f%%)", manaConsumed, castResult.getPowerMultiplier() * 100);

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

        // Get player position
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
     * Show mana cost preview during composition.
     */
    private void showCostPreview(GlyphMode mode, Store<EntityStore> store, Ref<EntityStore> playerRef) {
        CompositionState composition = mode.getComposition();
        Hex hex = composition.getHex();

        if (!hex.isEmpty()) {
            int cost = costCalculator.calculateBaseCost(hex);
            LOGGER.atInfo().log("Current composition cost: %d mana", cost);
            // TODO: Display cost in HUD/UI
        }
    }

    /**
     * Show feedback for invalid placement.
     */
    private void showInvalidActionFeedback(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        // TODO: Play error sound, flash red, etc.
        LOGGER.atInfo().log("Invalid glyph placement");
    }

    /**
     * Show feedback for insufficient mana.
     */
    private void showInsufficientManaFeedback(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        // TODO: Play error sound, flash mana bar, etc.
        LOGGER.atInfo().log("Insufficient mana for cast");
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
        EntityStatValue manaValue = playerStats.get(manaIndex);
        if (manaValue == null) {
            return;
        }

        // Subtract mana (negative amount to consume)
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

    /**
     * Handle player disconnect to clean up glyph mode session.
     */
    private void registerDisconnectHandler(EventRegistry eventRegistry) {
        eventRegistry.register(PlayerDisconnectEvent.class, event -> {
            try {
                handleDisconnect(event);
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Error handling disconnect event");
            }
        });
    }

    private void handleDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();

        UUID playerId = playerRef.getUuid();

        // Clean up glyph mode session
        modeManager.removeSession(playerId);
        LOGGER.atInfo().log("Cleaned up glyph mode session for disconnected player %s", playerId);
    }
}
