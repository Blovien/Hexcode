package com.riprod.hexcode.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.Collector;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.mode.GlyphMode;
import com.riprod.hexcode.mode.GlyphModeManager;
import com.riprod.hexcode.util.HexStaffUtil;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Handles Secondary (right-click) interaction for Hex Staff.
 * Toggles Glyph Mode on/off when both Hex Staff and Hex Book are equipped.
 */
public class HexcodeGlyphModeToggle extends Interaction {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<HexcodeGlyphModeToggle> CODEC = BuilderCodec.builder(
            HexcodeGlyphModeToggle.class,
            HexcodeGlyphModeToggle::new,
            Interaction.ABSTRACT_CODEC
    ).build();

    public HexcodeGlyphModeToggle() {
    }

    public HexcodeGlyphModeToggle(String id) {
        super(id);
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
        if (!firstRun) {
            // Already finished on first tick
            return;
        }

        Ref<EntityStore> ref = context.getEntity();
        if (!ref.isValid()) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        // Get player from entity
        Player player = context.getCommandBuffer().getComponent(ref, Player.getComponentType());
        if (player == null) {
            LOGGER.atFine().log("HexcodeGlyphModeToggle: No Player component found");
            context.getState().state = InteractionState.Failed;
            return;
        }

        // Get player UUID
        UUIDComponent uuidComponent = context.getCommandBuffer().getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            LOGGER.atFine().log("HexcodeGlyphModeToggle: No UUID component found");
            context.getState().state = InteractionState.Failed;
            return;
        }
        UUID playerId = uuidComponent.getUuid();

        // Check equipment requirements
        Inventory inventory = player.getInventory();
        boolean hasStaff = HexStaffUtil.hasHexStaffInMainHand(inventory);
        boolean hasBook = HexStaffUtil.hasHexBookInOffhand(inventory);

        LOGGER.atFine().log("HexcodeGlyphModeToggle: hasStaff=%b, hasBook=%b", hasStaff, hasBook);

        if (!hasStaff || !hasBook) {
            // Missing required equipment - finish without action
            LOGGER.atFine().log("HexcodeGlyphModeToggle: Missing equipment");
            return;
        }

        // Toggle glyph mode
        GlyphModeManager modeManager = GlyphModeManager.getInstance();
        GlyphMode mode = modeManager.getSession(playerId);
        boolean inGlyphMode = mode != null && mode.isActive();

        if (inGlyphMode) {
            // Exit glyph mode (pass command buffer for deferred entity despawn)
            modeManager.exitGlyphMode(playerId, context.getCommandBuffer());
            LOGGER.atInfo().log("Player %s exited glyph mode via Secondary", playerId);
        } else {
            // Enter glyph mode (pass command buffer for deferred entity spawn)
            modeManager.toggleGlyphMode(playerId, ref, null, context.getCommandBuffer());
            LOGGER.atInfo().log("Player %s entered glyph mode via Secondary", playerId);
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
}
