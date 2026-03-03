package com.riprod.hexcode.interaction;

import java.util.Arrays;

import javax.annotation.Nonnull;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.ChargingInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.state.HexState;
import com.riprod.hexcode.state.HexcodeManager;
import com.riprod.hexcode.state.StateRouter;

import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;

public class HexMode extends ChargingInteraction {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    public static final BuilderCodec<HexMode> CODEC = BuilderCodec
            .builder(HexMode.class, HexMode::new, ChargingInteraction.ABSTRACT_CODEC)
            .<String>appendInherited(
                    new KeyedCodec<>("TargetState", Codec.STRING),
                    (i, s) -> i.targetState = HexState.valueOf(s),
                    i -> i.targetState != null ? i.targetState.name() : null,
                    (i, p) -> i.targetState = p.targetState)
            .add()
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
                if (i.next != null) {
                    i.sortedKeys = i.next.keySet().toFloatArray();
                    Arrays.sort(i.sortedKeys);
                    i.highestChargeValue = i.sortedKeys[i.sortedKeys.length - 1];
                }
            })
            .build();

    private HexState targetState;

    public HexMode() {
    }

    @Override
    protected void tick0(boolean firstRun, float dt, @Nonnull InteractionType type,
            @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldown) {

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
            LOGGER.atInfo().log("hexmode: state mismatch, current=%s target=%s", hexcaster.getState(), targetState);
            ctx.getState().state = InteractionState.Finished;
            return;
        }

        HexcodeManager manager = StateRouter.route(targetState);
        if (manager == null) {
            ctx.getState().state = InteractionState.Finished;
            return;
        }

        if (firstRun) {
            ctx.getState().state = manager.enterInteraction(commandBuffer, playerRef, hexcaster);
        } else {
            ctx.getState().state = manager.tickInteraction(commandBuffer, playerRef, dt, hexcaster);
        }

        super.tick0(firstRun, dt, type, ctx, cooldown);
    }
}
