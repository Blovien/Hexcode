package com.riprod.hexcode.interaction;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.ChargingInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.Collector;
import com.hypixel.hytale.server.core.modules.interaction.interaction.operation.Label;
import com.hypixel.hytale.server.core.modules.interaction.interaction.operation.OperationsBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.state.HexState;

import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;

import javax.annotation.Nonnull;

public class StaffSecondaryEnter extends ChargingInteraction {
    @Nonnull
    public static final BuilderCodec<StaffSecondaryEnter> CODEC = BuilderCodec
            .builder(StaffSecondaryEnter.class, StaffSecondaryEnter::new,
                    ChargingInteraction.ABSTRACT_CODEC)
            .<String>appendInherited(
                    new KeyedCodec<>("Next", Interaction.CHILD_ASSET_CODEC),
                    (i, s) -> {
                        i.next = new Float2ObjectOpenHashMap<>();
                        i.next.put(0.0f, s);
                    },
                    i -> i.next != null ? i.next.get(0.0f) : null,
                    (i, p) -> i.next = p.next)
            .add()
            .afterDecode(i -> {
                i.allowIndefiniteHold = true;
            })
            .build();

    public StaffSecondaryEnter() {
    }

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type,
            @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldown) {

        ctx.getState().state = InteractionState.NotFinished;
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

        HexcasterComponent hexcaster = commandBuffer.getComponent(playerRef,
                HexcasterComponent.getComponentType());

        if (hexcaster == null) {
            ctx.getState().state = InteractionState.Failed;
            return;
        }

        if (firstRun) {
            switch (hexcaster.getState()) {
                case IDLE:
                case EXECUTION: {
                    hexcaster.requestStateChange(HexState.CASTING);
                    ctx.getState().state = InteractionState.NotFinished;
                    break;
                }
                case CRAFTING: {
                    hexcaster.requestStateChange(HexState.DRAWING);
                    ctx.getState().state = InteractionState.NotFinished;
                    break;
                }
                default:
                    ctx.getState().state = InteractionState.Finished;
                    break;
            }

            super.tick0(firstRun, time, type, ctx, cooldown);
            return;
        }

        ctx.getState().state = InteractionState.NotFinished;
        super.tick0(firstRun, time, type, ctx, cooldown);
    }

    @Override
    protected void simulateTick0(boolean firstRun, float time, @Nonnull InteractionType type,
            @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldown) {
        super.simulateTick0(firstRun, time, type, ctx, cooldown);
    }

    @Override
    public void compile(@Nonnull OperationsBuilder builder) {
        Label end = builder.createUnresolvedLabel();
        Label[] labels = new Label[(this.next != null ? this.next.size() : 0) + 1];

        for (int i = 0; i < labels.length; i++) {
            labels[i] = builder.createUnresolvedLabel();
        }

        builder.addOperation(this, labels);
        builder.jump(end);

        if (this.next != null && !this.next.isEmpty()) {
            builder.resolveLabel(labels[0]);
            Interaction interaction = Interaction.getInteractionOrUnknown(this.next.get(0.0f));
            interaction.compile(builder);
            builder.jump(end);
        }

        builder.resolveLabel(end);
    }

    @Nonnull
    @Override
    public WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.Client;
    }

    @Override
    protected com.hypixel.hytale.protocol.Interaction generatePacket() {
        return new com.hypixel.hytale.protocol.ChargingInteraction();
    }

    @Override
    public boolean needsRemoteSync() {
        return true;
    }

    @Override
    public boolean walk(@Nonnull Collector collector, @Nonnull InteractionContext ctx) {
        return false;
    }
}
