package com.riprod.hexcode.interaction;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.Collector;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.state.HexState;
import com.riprod.hexcode.state.HexcodeManager;
import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.state.StateRouter;

public class HexModeExit extends SimpleInteraction {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    public static final BuilderCodec<HexModeExit> CODEC = BuilderCodec
            .builder(HexModeExit.class, HexModeExit::new, SimpleInteraction.CODEC)
            .<String>appendInherited(
                    new KeyedCodec<>("TargetState", Codec.STRING),
                    (i, s) -> i.targetState = HexState.valueOf(s),
                    i -> i.targetState != null ? i.targetState.name() : null,
                    (i, p) -> i.targetState = p.targetState)
            .add()
            .build();

    private HexState targetState;

    public HexModeExit() {
    }

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type,
            @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldown) {
        try {
            if (targetState == null) {
                ctx.getState().state = InteractionState.Failed;
                return;
            }

            CommandBuffer<EntityStore> commandBuffer = ctx.getCommandBuffer();
            if (commandBuffer == null) {
                ctx.getState().state = InteractionState.Failed;
                return;
            }

            Ref<EntityStore> playerRef = ctx.getEntity();
            if (playerRef == null || !playerRef.isValid()) {
                ctx.getState().state = InteractionState.Failed;
                return;
            }

            HexcasterComponent hexcaster = commandBuffer.getComponent(playerRef, HexcasterComponent.getComponentType());
            if (hexcaster == null) {
                ctx.getState().state = InteractionState.Failed;
                return;
            }

            if (hexcaster.getState() != targetState) {
                ctx.getState().state = InteractionState.Finished;
                return;
            }

            HexcodeManager manager = StateRouter.route(targetState);
            if (manager == null) {
                ctx.getState().state = InteractionState.Finished;
                return;
            }

            if (firstRun) {
                ctx.getState().state = manager.exitInteraction(commandBuffer, playerRef, hexcaster);
            }

            super.tick0(firstRun, time, type, ctx, cooldown);
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] HexModeExit failed: %s", e.getMessage());
            ctx.getState().state = InteractionState.Failed;
        }
    }

    @Override
    protected void simulateTick0(boolean firstRun, float time, @Nonnull InteractionType type,
            @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldown) {
    }

    @Nonnull
    @Override
    public WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.Server;
    }

    @Override
    public boolean walk(@Nonnull Collector collector, @Nonnull InteractionContext ctx) {
        return false;
    }
}
