package com.riprod.hexcode.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.Collector;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.player.component.HexcasterComponent;

import javax.annotation.Nonnull;

public class GlyphDropInteraction extends SimpleInteraction {
    private final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    public static final BuilderCodec<GlyphDropInteraction> CODEC = BuilderCodec
            .builder(GlyphDropInteraction.class, GlyphDropInteraction::new, SimpleInteraction.CODEC)
            .build();

    public GlyphDropInteraction() {
    }

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type,
            @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldown) {

        LOGGER.atInfo().log("glyph drop processed");
        // CommandBuffer<EntityStore> commandBuffer = ctx.getCommandBuffer();
        // if (commandBuffer == null) {
        //     ctx.getState().state = InteractionState.Failed;
        //     return;
        // }

        // Ref<EntityStore> playerRef = ctx.getEntity();
        // if (playerRef == null || !playerRef.isValid()) {
        //     ctx.getState().state = InteractionState.Failed;
        //     return;
        // }

        // todo: check if dropped on another glyph
        // todo: if yes: server confirms merge (create HexNode, delete dragged entity)
        // todo: if no: lock glyph at current position
        // todo: clear HexcasterComponent.draggingGlyphRef

        // HexcasterComponent hexcaster = commandBuffer.getComponent(playerRef, HexcasterComponent.getComponentType());

        ctx.getState().state = InteractionState.Finished;
    }

    @Override
    protected void simulateTick0(boolean firstRun, float time, @Nonnull InteractionType type,
            @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldown) {
        // client waits for server confirmation on merges
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
