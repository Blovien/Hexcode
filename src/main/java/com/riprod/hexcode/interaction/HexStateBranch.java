package com.riprod.hexcode.interaction;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.EnumMapCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.ChargingInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.Collector;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.StringTag;
import com.hypixel.hytale.server.core.modules.interaction.interaction.operation.Label;
import com.hypixel.hytale.server.core.modules.interaction.interaction.operation.OperationsBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.state.HexState;

public class HexStateBranch extends Interaction {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final StringTag TAG_FAILED = StringTag.of("Failed");
    private static final int FAILED_LABEL_INDEX = HexState.values().length;

    @Nonnull
    public static final BuilderCodec<HexStateBranch> CODEC = BuilderCodec
            .builder(HexStateBranch.class, HexStateBranch::new, Interaction.ABSTRACT_CODEC)
            .<Map<HexState, String>>appendInherited(
                    new KeyedCodec<>("Branches", new EnumMapCodec<>(HexState.class, Interaction.CHILD_ASSET_CODEC)),
                    (i, m) -> i.branches = m,
                    i -> i.branches,
                    (i, p) -> i.branches = p.branches)
            .add()
            .<String>appendInherited(
                    new KeyedCodec<>("Failed", Interaction.CHILD_ASSET_CODEC),
                    (i, m) -> i.failed = m,
                    i -> i.failed,
                    (i, p) -> i.failed = p.failed)
            .add()
            .build();

    @Nullable
    private Map<HexState, String> branches;

    @Nullable
    private String failed;

    public HexStateBranch() {
    }

    @Override
    protected void tick0(boolean firstRun, float time, @Nullable InteractionType type,
            @Nonnull InteractionContext ctx, @Nullable CooldownHandler cooldown) {

        CommandBuffer<EntityStore> commandBuffer = ctx.getCommandBuffer();
        if (commandBuffer == null) {
            LOGGER.atWarning().log("HexStateBranch: no command buffer in context");
            if (ctx.hasLabels())
                ctx.jump(ctx.getLabel(FAILED_LABEL_INDEX));
            return;
        }

        Ref<EntityStore> playerRef = ctx.getEntity();
        if (playerRef == null || !playerRef.isValid()) {
            ctx.getState().state = InteractionState.Failed;
            if (ctx.hasLabels())
                ctx.jump(ctx.getLabel(FAILED_LABEL_INDEX));
            return;
        }

        HexcasterComponent hexcaster = commandBuffer.getComponent(playerRef, HexcasterComponent.getComponentType());
        if (hexcaster == null) {
            ctx.getState().state = InteractionState.Failed;
            if (ctx.hasLabels())
                ctx.jump(ctx.getLabel(FAILED_LABEL_INDEX));

            return;
        }

        HexState state = hexcaster.getState();

        if (branches != null && branches.containsKey(state)) {
            String branchAssetId = branches.get(state);
            ctx.getState().state = InteractionState.Finished;
            ctx.jump(ctx.getLabel(state.ordinal()));
        } else {
            ctx.getState().state = InteractionState.Failed;
            if (ctx.hasLabels())
                ctx.jump(ctx.getLabel(FAILED_LABEL_INDEX));
        }
    }

    @Override
    protected void simulateTick0(boolean firstRun, float time, @Nonnull InteractionType type,
            @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldown) {
    }

    @Override
    public void compile(@Nonnull OperationsBuilder builder) {
        Label endLabel = builder.createUnresolvedLabel();
        HexState[] states = HexState.values();

        Label[] labels = new Label[states.length + 1];

        for (int i = 0; i < labels.length; i++) {
            labels[i] = builder.createUnresolvedLabel();
        }

        builder.addOperation(this, labels);

        for (HexState state : HexState.values()) {
            builder.resolveLabel(labels[state.ordinal()]);
            String branchId = branches != null ? branches.get(state) : null;
            if (branchId != null) {
                Interaction branch = Interaction.getInteractionOrUnknown(branchId);
                branch.compile(builder);
            }
            builder.jump(endLabel);
        }

        builder.resolveLabel(labels[FAILED_LABEL_INDEX]);
        if (failed != null) {
            Interaction failedInteraction = Interaction.getInteractionOrUnknown(failed);
            failedInteraction.compile(builder);
        }

        builder.resolveLabel(endLabel);
    }

    @Nonnull
    @Override
    public WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.None;
    }

    @Override
    protected com.hypixel.hytale.protocol.Interaction generatePacket() {
        return new com.hypixel.hytale.protocol.ConditionInteraction();
    }

    @Override
    protected void configurePacket(com.hypixel.hytale.protocol.Interaction packet) {
        super.configurePacket(packet);
    }

    @Override
    public boolean needsRemoteSync() {
        if (branches != null) {
            for (String branchId : branches.values()) {
                if (Interaction.needsRemoteSync(branchId))
                    return true;
            }
        }
        return Interaction.needsRemoteSync(failed);
    }

    @Override
    public boolean walk(@Nonnull Collector collector, @Nonnull InteractionContext ctx) {
        if (branches != null) {
            for (Map.Entry<HexState, String> entry : branches.entrySet()) {
                if (InteractionManager.walkInteraction(collector, ctx,
                        StringTag.of(entry.getKey().name()), entry.getValue())) {
                    return true;
                }
            }
        }
        return failed != null && InteractionManager.walkInteraction(collector, ctx, TAG_FAILED, failed);
    }
}
