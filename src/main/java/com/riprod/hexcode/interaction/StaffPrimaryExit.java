package com.riprod.hexcode.interaction;

import javax.annotation.Nonnull;

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
import com.riprod.hexcode.core.casting.system.DraggingManager;
import com.riprod.hexcode.core.drawing.system.DrawingManager;
import com.riprod.hexcode.player.component.HexcasterComponent;

public class StaffPrimaryExit extends SimpleInteraction {

    @Nonnull
    public static final BuilderCodec<StaffPrimaryExit> CODEC = BuilderCodec
            .builder(StaffPrimaryExit.class, StaffPrimaryExit::new, SimpleInteraction.CODEC)
            .build();

    public StaffPrimaryExit() {
    }

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type,
            @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldown) {

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

        // First run logic

        if (firstRun) {
            switch (hexcaster.getCurrentMode()) {
                case CASTING: {
                    ctx.getState().state = DraggingManager.ExitDraggingMode(commandBuffer, hexcaster, playerRef);
                    break;
                }
                case DRAWING: {
                    ctx.getState().state = DrawingManager.StopDrawing(commandBuffer, hexcaster, playerRef);
                    break;
                }
                default:
                    ctx.getState().state = InteractionState.Finished;
                    break;
            }

            super.tick0(firstRun, time, type, ctx, cooldown);
            return;
        }

        // Tick logic

        switch (hexcaster.getCurrentMode()) {
            case CASTING:
            case DRAWING:
            default:
                ctx.getState().state = InteractionState.Finished;
                break;
        }
        return;
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
