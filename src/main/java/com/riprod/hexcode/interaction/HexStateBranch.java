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
import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
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

            HexState state = hexcaster.getState();

            if (branches != null && branches.containsKey(state)) {
                RootInteraction branch = RootInteraction.getAssetMap().getAsset(branches.get(state));
                if (branch == null) {
                    ctx.getState().state = InteractionState.Failed;
                    return;
                }
                ctx.getState().state = InteractionState.Finished;
                ctx.execute(branch);
            } else {
                ctx.getState().state = InteractionState.Failed;
            }
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] HexStateBranch failed: %s", e.getMessage());
            ctx.getState().state = InteractionState.Failed;
        }
    }
}