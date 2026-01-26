package com.riprod.hexcode.interaction;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.meta.MetaKey;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.ChargingInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.Collector;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.mode.GlyphMode;
import com.riprod.hexcode.mode.GlyphModeManager;
import com.riprod.hexcode.util.HexStaffUtil;
import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Handles Secondary (right-click) hold interaction for Hex Staff.
 *
 * Hold to enter Glyph Mode - glyphs orbit while held.
 * On release, chains to HexcodeGlyphModeDisable via Next: {"0.0": "..."} in
 * JSON.
 *
 * Uses client-side prediction for smooth glyph updates during lag.
 */
public class HexcodeGlyphModeToggle extends ChargingInteraction {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // MetaKeys to track previous time for delta calculation
    private static final MetaKey<Float> PREVIOUS_TIME =
        Interaction.META_REGISTRY.registerMetaObject(i -> 0.0f);
    private static final MetaKey<Float> PREVIOUS_SIMULATION_TIME =
        Interaction.META_REGISTRY.registerMetaObject(i -> 0.0f);

    public static final BuilderCodec<HexcodeGlyphModeToggle> CODEC = BuilderCodec.builder(
            HexcodeGlyphModeToggle.class,
            HexcodeGlyphModeToggle::new,
            ChargingInteraction.ABSTRACT_CODEC)
        // Add fields that ABSTRACT_CODEC doesn't include but we need:
        .appendInherited(
            new KeyedCodec<>("AllowIndefiniteHold", Codec.BOOLEAN),
            (i, s) -> i.allowIndefiniteHold = s,
            i -> i.allowIndefiniteHold,
            (i, p) -> i.allowIndefiniteHold = p.allowIndefiniteHold
        )
        .add()
        .appendInherited(
            new KeyedCodec<>("DisplayProgress", Codec.BOOLEAN),
            (i, s) -> i.displayProgress = s,
            i -> i.displayProgress,
            (i, p) -> i.displayProgress = p.displayProgress
        )
        .add()
        // Next field - simplified like WieldingInteraction (single interaction at 0.0)
        .<String>appendInherited(
            new KeyedCodec<>("Next", Interaction.CHILD_ASSET_CODEC),
            (i, s) -> {
                i.next = new Float2ObjectOpenHashMap<>();
                i.next.put(0.0F, s);
                i.sortedKeys = new float[]{0.0F};
                i.highestChargeValue = 0.0F;
            },
            i -> i.next != null ? i.next.get(0.0F) : null,
            (i, p) -> {
                i.next = p.next;
                i.sortedKeys = p.sortedKeys;
                i.highestChargeValue = p.highestChargeValue;
            }
        )
        .add()
        .build();

    public HexcodeGlyphModeToggle() {
        this.allowIndefiniteHold = true;
        this.displayProgress = false;
    }

    @Override
    protected void tick0(
            boolean firstRun,
            float time,
            @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler) {

        // Calculate delta time from previous time
        float previousTime = context.getInstanceStore().getMetaObject(PREVIOUS_TIME);
        float deltaTime = firstRun ? 0.0f : (time - previousTime);
        context.getInstanceStore().putMetaObject(PREVIOUS_TIME, time);

        Ref<EntityStore> ref = context.getEntity();
        if (!ref.isValid()) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Player player = context.getCommandBuffer().getComponent(ref, Player.getComponentType());
        if (player == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        UUIDComponent uuidComponent = context.getCommandBuffer().getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        UUID playerId = uuidComponent.getUuid();

        Inventory inventory = player.getInventory();
        boolean hasStaff = HexStaffUtil.hasHexStaffInMainHand(inventory);
        ItemStack bookStack = HexStaffUtil.getHexBookFromOffhand(inventory);

        if (!hasStaff || bookStack == null) {
            context.getState().state = InteractionState.Finished;
            return;
        }

        World world = context.getCommandBuffer().getExternalData().getWorld();
        if (world == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        GlyphModeManager modeManager = GlyphModeManager.getInstance();

        // Enter glyph mode on first tick (press)
        if (firstRun) {
            GlyphMode mode = modeManager.getSession(playerId);
            if (mode == null || !mode.isActive()) {
                modeManager.enterGlyphMode(playerId, ref, context.getCommandBuffer(), world, bookStack, inventory);
                LOGGER.atInfo().log("Player %s entered glyph mode", playerId);
            }
        }

        // SERVER: Tick glyph positions with delta time (authoritative)
        if (deltaTime > 0.0f) {
            modeManager.tickAll(deltaTime, context.getCommandBuffer());
        }

        // Let parent handle charging state machine EVERY TICK
        // This reads chargeValue from client and sets Finished when released
        super.tick0(firstRun, time, type, context, cooldownHandler);

        // NOTE: Exit logic is handled by HexcodeGlyphModeDisable via Next chain
        // When super.tick0() sets Finished, the framework chains to Next["0.0"]
    }

    @Override
    protected void simulateTick0(
            boolean firstRun,
            float time,
            @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler) {

        // Calculate delta time from previous simulation time
        float previousTime = context.getInstanceStore().getMetaObject(PREVIOUS_SIMULATION_TIME);
        float deltaTime = firstRun ? 0.0f : (time - previousTime);
        context.getInstanceStore().putMetaObject(PREVIOUS_SIMULATION_TIME, time);

        // CLIENT: Tick glyph positions with delta time for smooth visuals
        if (deltaTime > 0.0f) {
            GlyphModeManager.getInstance().tickAll(deltaTime, context.getCommandBuffer());
        }

        // Let parent handle isCharging() detection and state sync
        // This is CRITICAL - without this, client/server desync
        super.simulateTick0(firstRun, time, type, context, cooldownHandler);
    }

    @Override
    public boolean walk(@Nonnull Collector collector, @Nonnull InteractionContext context) {
        return super.walk(collector, context);
    }
}
