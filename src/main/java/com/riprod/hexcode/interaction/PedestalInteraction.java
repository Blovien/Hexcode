package com.riprod.hexcode.interaction;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.crafting.system.PedestalInteractionSystem;

public class PedestalInteraction extends SimpleInteraction {

    @Nonnull
    public static final BuilderCodec<PedestalInteraction> CODEC = BuilderCodec
            .builder(PedestalInteraction.class, PedestalInteraction::new, SimpleInteraction.CODEC)
            .build();

    public PedestalInteraction() {
    }

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type,
            @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldown) {

        if (!firstRun) {
            ctx.getState().state = InteractionState.Finished;
            return;
        }

        CommandBuffer<EntityStore> buffer = ctx.getCommandBuffer();
        if (buffer == null) {
            ctx.getState().state = InteractionState.Failed;
            return;
        }

        Ref<EntityStore> playerRef = ctx.getEntity();
        if (playerRef == null || !playerRef.isValid()) {
            ctx.getState().state = InteractionState.Failed;
            return;
        }

        BlockPosition targetBlock = ctx.getTargetBlock();
        if (targetBlock == null) {
            ctx.getState().state = InteractionState.Failed;
            return;
        }

        PedestalInteractionSystem.HandleInteraction(buffer, playerRef, targetBlock);

        ctx.getState().state = InteractionState.Finished;
        super.tick0(firstRun, time, type, ctx, cooldown);
    }

    @Override
    protected void simulateTick0(boolean firstRun, float time, @Nonnull InteractionType type,
            @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldown) {
    }
}
