package com.riprod.hexcode.interaction;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.EnumMapCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.state.HexState;

public class HexStateBranch extends SimpleInteraction {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    public static final BuilderCodec<HexStateBranch> CODEC = BuilderCodec
            .builder(HexStateBranch.class, HexStateBranch::new, SimpleInteraction.CODEC)
            .<Map<HexState, String>>appendInherited(
                    new KeyedCodec<>("Branches", new EnumMapCodec<>(HexState.class, RootInteraction.CHILD_ASSET_CODEC)),
                    (i, m) -> i.branches = m,
                    i -> i.branches,
                    (i, p) -> i.branches = p.branches)
            .add()
            .build();

    @Nullable
    private Map<HexState, String> branches;

    public HexStateBranch() {
    }

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type,
            @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldown) {
        try {
            LOGGER.atInfo().log("[HSB] tick0 enter firstRun=%s time=%s type=%s", firstRun, time, type);
            CommandBuffer<EntityStore> commandBuffer = ctx.getCommandBuffer();
            if (commandBuffer == null) {
                LOGGER.atInfo().log("[HSB] tick0 FAIL: commandBuffer null");
                ctx.getState().state = InteractionState.Failed;
                return;
            }

            Ref<EntityStore> entityRef = ctx.getEntity();
            Ref<EntityStore> owningRef = ctx.getOwningEntity();
            LOGGER.atInfo().log("[HSB] tick0 refs entity=%s(valid=%s) owning=%s(valid=%s) same=%s",
                    entityRef, entityRef != null && entityRef.isValid(),
                    owningRef, owningRef != null && owningRef.isValid(),
                    entityRef == owningRef);

            Ref<EntityStore> playerRef = entityRef;
            if (playerRef == null || !playerRef.isValid()) {
                LOGGER.atInfo().log("[HSB] tick0 FAIL: entity ref null/invalid");
                ctx.getState().state = InteractionState.Failed;
                return;
            }

            HexcasterComponent hexcasterOnEntity = commandBuffer.getComponent(entityRef, HexcasterComponent.getComponentType());
            HexcasterComponent hexcasterOnOwning = owningRef != null && owningRef.isValid()
                    ? commandBuffer.getComponent(owningRef, HexcasterComponent.getComponentType())
                    : null;
            LOGGER.atInfo().log("[HSB] tick0 hexcaster on entity=%s on owning=%s",
                    hexcasterOnEntity != null ? hexcasterOnEntity.getState() : "null",
                    hexcasterOnOwning != null ? hexcasterOnOwning.getState() : "null");

            HexcasterComponent hexcaster = hexcasterOnEntity;
            if (hexcaster == null) {
                LOGGER.atInfo().log("[HSB] tick0 FAIL: HexcasterComponent missing on entity ref");
                ctx.getState().state = InteractionState.Failed;
                return;
            }

            HexState state = hexcaster.getState();
            LOGGER.atInfo().log("[HSB] tick0 state=%s branchKeys=%s containsKey=%s",
                    state,
                    branches != null ? branches.keySet() : "null-map",
                    branches != null && branches.containsKey(state));

            if (branches != null && branches.containsKey(state)) {
                String branchId = branches.get(state);
                RootInteraction branch = RootInteraction.getAssetMap().getAsset(branchId);
                if (branch == null) {
                    LOGGER.atInfo().log("[HSB] tick0 FAIL: branch asset missing id=%s", branchId);
                    ctx.getState().state = InteractionState.Failed;
                    return;
                }
                LOGGER.atInfo().log("[HSB] tick0 EXEC branchId=%s", branchId);
                ctx.getState().state = InteractionState.Finished;
                ctx.execute(branch);
            } else {
                LOGGER.atInfo().log("[HSB] tick0 FAIL: no branch for state=%s", state);
                ctx.getState().state = InteractionState.Failed;
            }
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] HexStateBranch failed: %s", e.getMessage());
            ctx.getState().state = InteractionState.Failed;
        }
    }

    @Nonnull
    @Override
    public WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.None;
    }

    @Override
    protected void simulateTick0(boolean firstRun, float time, InteractionType type,
            InteractionContext ctx, CooldownHandler cooldown) {
        LOGGER.atInfo().log("[HSB] simulateTick0 enter firstRun=%s", firstRun);
        if (!firstRun)
            return;
        HexcasterComponent hc = ctx.getCommandBuffer().getComponent(ctx.getEntity(),
                HexcasterComponent.getComponentType());
        LOGGER.atInfo().log("[HSB] simulateTick0 hexcaster=%s branches=%s",
                hc != null ? hc.getState() : "null",
                branches != null ? branches.keySet() : "null-map");
        if (hc == null || branches == null)
            return;
        String branchId = branches.get(hc.getState());
        LOGGER.atInfo().log("[HSB] simulateTick0 branchId=%s", branchId);
        if (branchId == null)
            return;
        RootInteraction branch = RootInteraction.getAssetMap().getAsset(branchId);
        if (branch != null) {
            LOGGER.atInfo().log("[HSB] simulateTick0 EXEC branchId=%s", branchId);
            ctx.getState().state = InteractionState.Finished;
            ctx.execute(branch);
        }
    }
}
