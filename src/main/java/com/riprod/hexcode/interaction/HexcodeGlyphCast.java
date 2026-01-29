package com.riprod.hexcode.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
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
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.Collector;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.data.GlyphInstance;
import com.riprod.hexcode.executing.HexExecutor;
import com.riprod.hexcode.hex.HexNode;
import com.riprod.hexcode.mode.GlyphMode;
import com.riprod.hexcode.mode.GlyphModeManager;
import com.riprod.hexcode.util.HexBookMetadata;
import com.riprod.hexcode.util.HexStaffUtil;
import com.riprod.hexcode.util.RaycastUtil;
import javax.annotation.Nonnull;
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
public class HexcodeGlyphCast extends Interaction {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();


    private final HexExecutor hexExecutor;

    // Cached component types for performance (lazy initialization)
    private static volatile ComponentType<EntityStore, TransformComponent> transformComponentType;
    private static volatile ComponentType<EntityStore, HeadRotation> headRotationComponentType;

    public static final BuilderCodec<HexcodeGlyphCast> CODEC = BuilderCodec.builder(
            HexcodeGlyphCast.class,
            HexcodeGlyphCast::new,
            Interaction.ABSTRACT_CODEC).build();

    public HexcodeGlyphCast() {
        this.hexExecutor = new HexExecutor();
    }

    public HexcodeGlyphCast(String id) {
        super(id);
        this.hexExecutor = new HexExecutor();
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
            @Nonnull CooldownHandler cooldownHandler) {
        // early fail
        Ref<EntityStore> ref = context.getEntity();

        if (!ref.isValid()) {
            context.getState().state = InteractionState.Failed;
            LOGGER.atInfo().log("HexcodeGlyphCast: Invalid entity reference");
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

        // Get glyph mode state
        GlyphModeManager modeManager = GlyphModeManager.getInstance();
        GlyphMode mode = modeManager.getSession(playerId);
        boolean inGlyphMode = mode != null && mode.isActive();
        if (inGlyphMode) {
            context.getState().state = InteractionState.Finished;
            LOGGER.atInfo().log("HexcodeGlyphAction: In glyph mode - cancelling normal cast");
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

        // Check equipment requirements
        Inventory inventory = player.getInventory();

        if (!HexStaffUtil.hasHexcodeEquipment(inventory)) {
            // Missing required equipment
            LOGGER.atInfo().log("HexcodeGlyphAction: Wrong Equipment");
            context.getState().state = InteractionState.Failed;
            // exit glyph mode
            modeManager.removeSession(playerId);
            return;
        }

        // Not in glyph mode: cast hex on first tick
        if (firstRun) {
            LOGGER.atInfo().log("HexcodeGlyphAction: Casting hex on first tick");

            // Get world context for WorldHexDataStore
            World world = context.getCommandBuffer().getExternalData().getWorld();

            // First try to get hex from WorldHexDataStore (per-book queued spell)
            ItemStack bookStack = HexStaffUtil.getHexBookFromOffhand(inventory);
            if (bookStack != null && world != null) {
                // Get book UUID for logging
                UUID bookUuid = HexBookMetadata.getBookUUID(bookStack);
                if (bookUuid == null) {
                    LOGGER.atInfo().log("Book has no UUID - never used in glyph mode");
                }
            }

            // Fallback: try active hex from mode session
            if (mode != null) {
                HexNode activeHexNode = mode.getHexToCast();
                if (activeHexNode != null) {
                    castHex(playerId, ref, store, mode);
                    LOGGER.atInfo().log("Player %s cast hex via Primary", playerId);
                } else {
                    LOGGER.atInfo().log("No active hex to cast");
                }
            } else {
                LOGGER.atInfo().log("No hex queued in book and no glyph mode session");
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
        // Drag end is now handled in tick0() when client state indicates release
    }

    @Override
    protected void simulateTick0(
            boolean firstRun,
            float time,
            @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler) {

        // No simulation needed for this interaction
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
        // Enable sync so client state is sent to server
        return true;
    }

    /**
     * Cast the active hex from glyph mode.
     *
     * <p>With unified treatment, the hex to cast is a HexNode root.
     * A single glyph is a HexNode with no children.
     */
    private void castHex(UUID playerId, Ref<EntityStore> playerRef, Store<EntityStore> store, GlyphMode mode) {
        HexNode hexRoot = mode.getHexToCast();

        if (hexRoot == null) {
            LOGGER.atInfo().log("Failed to cast hex - no active hex selected");
            return;
        }

        // Calculate mana cost by traversing the hex tree
        float totalManaCost = calculateNodeManaCost(hexRoot);
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
        HexExecutor.ExecutionResult result = hexExecutor.execute(hexRoot, playerRef, store, null, direction);
        if (result.isSuccess()) {
            LOGGER.atInfo().log("Hex cast successfully");
        } else {
            LOGGER.atWarning().log("Hex execution failed: %s", result.getMessage());
        }

        // Clear active hex after casting
        mode.clearActiveHex();
    }

    /**
     * Cast a hex from WorldHexDataStore.
     *
     * <p>Used when player casts outside glyph mode - the hex is read from
     * the world storage using the book's UUID.
     *
     * <p>With unified treatment, the hex is stored as a HexNode root.
     *
     * @param playerId  The player's UUID
     * @param playerRef The player entity reference
     * @param store     The entity store
     * @param hexRoot   The HexNode root to cast (from WorldHexDataStore)
     * @param world     The world context (for clearing after cast)
     * @param bookUuid  The book's UUID (for clearing after cast)
     */
    private void castHexFromBook(UUID playerId, Ref<EntityStore> playerRef, Store<EntityStore> store,
            HexNode hexRoot, World world, UUID bookUuid) {
        if (hexRoot == null) {
            LOGGER.atInfo().log("Failed to cast hex from book - empty or invalid hex");
            return;
        }

        // Calculate mana cost by traversing the hex tree
        float totalManaCost = calculateNodeManaCost(hexRoot);
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
        HexExecutor.ExecutionResult result = hexExecutor.execute(hexRoot, playerRef, store, null, direction);
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
     * Recursively calculate mana cost for a node and its children.
     *
     * @param node The HexNode to calculate cost for
     * @return Total mana cost (sum of all glyph costs in tree)
     */
    private float calculateNodeManaCost(HexNode node) {
        if (node == null) {
            return 0f;
        }
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
}
