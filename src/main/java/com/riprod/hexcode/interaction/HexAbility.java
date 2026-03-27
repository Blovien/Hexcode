package com.riprod.hexcode.interaction;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.state.HexcodeManager;
import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.state.StateRouter;

public class HexAbility extends SimpleInteraction {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    public static final BuilderCodec<HexAbility> CODEC = BuilderCodec
            .builder(HexAbility.class, HexAbility::new, SimpleInteraction.CODEC)
            .build();

    public HexAbility() {
    }

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type,
            @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldown) {
        try {
            if (!firstRun) {
                ctx.getState().state = InteractionState.Finished;
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

            HexcodeManager manager = StateRouter.route(hexcaster.getState());
            if (manager == null) {
                ctx.getState().state = InteractionState.Finished;
                return;
            }

            ctx.getState().state = manager.enterAbility(commandBuffer, playerRef, hexcaster, type);
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] HexAbility failed: %s", e.getMessage());
            ctx.getState().state = InteractionState.Failed;
        }
    }

    @Override
    protected void simulateTick0(boolean firstRun, float time, @Nonnull InteractionType type,
            @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldown) {
    }
}
