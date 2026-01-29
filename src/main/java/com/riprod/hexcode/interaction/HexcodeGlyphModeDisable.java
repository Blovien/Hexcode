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
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.Collector;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.mode.GlyphMode;
import com.riprod.hexcode.mode.GlyphModeManager;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Exits Glyph Mode when chained from HexcodeGlyphModeToggle.
 * This interaction runs when the player releases Secondary (right-click).
 *
 * Handles cleanup: saves composition, despawns orbital glyphs, etc.
 */
public class HexcodeGlyphModeDisable extends Interaction {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<HexcodeGlyphModeDisable> CODEC = BuilderCodec.builder(
            HexcodeGlyphModeDisable.class,
            HexcodeGlyphModeDisable::new,
            Interaction.ABSTRACT_CODEC)
        .build();

    public HexcodeGlyphModeDisable() {
    }

    public HexcodeGlyphModeDisable(String id) {
        super(id);
    }

    @Nonnull
    @Override
    public WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.None;  // No client data needed - just cleanup
    }

    @Override
    protected void tick0(
            boolean firstRun,
            float time,
            @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler) {

        // Only run on first tick
        if (!firstRun) {
            context.getState().state = InteractionState.Finished;
            return;
        }
        LOGGER.atInfo().log("HexcodeGlyphModeDisable: Handling glyph mode disable interaction");

        Ref<EntityStore> ref = context.getEntity();
        if (!ref.isValid()) {
            context.getState().state = InteractionState.Finished;
            return;
        }

        Player player = context.getCommandBuffer().getComponent(ref, Player.getComponentType());
        if (player == null) {
            context.getState().state = InteractionState.Finished;
            return;
        }

        UUIDComponent uuidComponent = context.getCommandBuffer().getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent == null) {
            context.getState().state = InteractionState.Finished;
            return;
        }
        UUID playerId = uuidComponent.getUuid();

        World world = context.getCommandBuffer().getExternalData().getWorld();
        if (world == null) {
            LOGGER.atWarning().log("HexcodeGlyphModeDisable: Could not get world context");
            context.getState().state = InteractionState.Finished;
            return;
        }

        // Exit glyph mode
        GlyphModeManager modeManager = GlyphModeManager.getInstance();
        GlyphMode mode = modeManager.getSession(playerId);

        if (mode != null && mode.isActive()) {
            modeManager.exitGlyphMode(playerId, context.getCommandBuffer());
            LOGGER.atInfo().log("Player %s exited glyph mode via Disable interaction", playerId);
        }

        context.getState().state = InteractionState.Finished;
    }

    @Override
    protected void simulateTick0(
            boolean firstRun,
            float time,
            @Nonnull InteractionType type,
            @Nonnull InteractionContext context,
            @Nonnull CooldownHandler cooldownHandler) {

        // Client-side: just finish immediately
        // Server handles the actual cleanup
        context.getState().state = InteractionState.Finished;
    }

    @Override
    public boolean walk(@Nonnull Collector collector, @Nonnull InteractionContext context) {
        return false;
    }

    @Nonnull
    @Override
    protected com.hypixel.hytale.protocol.Interaction generatePacket() {
        return new com.hypixel.hytale.protocol.SimpleInteraction();
    }

    @Override
    public boolean needsRemoteSync() {
        return false;  // No sync needed - server-side cleanup only
    }
}
