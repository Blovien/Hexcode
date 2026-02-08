package com.riprod.hexcode.interaction;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.ChargingInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.Collector;
import com.hypixel.hytale.server.core.modules.interaction.interaction.operation.Label;
import com.hypixel.hytale.server.core.modules.interaction.interaction.operation.OperationsBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.casting.GlyphPositioner;
import com.riprod.hexcode.core.casting.GlyphSelector;
import com.riprod.hexcode.core.casting.GlyphStyler;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.player.component.HexcasterComponent;
import com.riprod.hexcode.player.state.CastingManager;
import com.riprod.hexcode.utils.GlyphMath;
import com.riprod.hexcode.utils.SphericalPosition;

import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CastingModeEnterInteraction extends ChargingInteraction {
    private final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    public static final BuilderCodec<CastingModeEnterInteraction> CODEC = BuilderCodec
            .builder(CastingModeEnterInteraction.class, CastingModeEnterInteraction::new,
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

    public CastingModeEnterInteraction() {
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

        if (firstRun) {
            Boolean result = CastingManager.EnterCastingMode(commandBuffer, playerRef);
            if (!result) {

                ctx.getState().state = InteractionState.Failed;
            }

            super.tick0(firstRun, time, type, ctx, cooldown);
            return;
        }

        // after first run
        HexcasterComponent hexcaster = commandBuffer.getComponent(playerRef, HexcasterComponent.getComponentType());

        if (hexcaster == null || !hexcaster.isInCastingMode()) {
            ctx.getState().state = InteractionState.Finished;
            return;
        }

        Ref<EntityStore> castingRootRef = hexcaster.getCastingRootRef();

        TransformComponent transform = commandBuffer.getComponent(playerRef, TransformComponent.getComponentType());
        HeadRotation headRotation = commandBuffer.getComponent(playerRef, HeadRotation.getComponentType());
        if (transform == null || headRotation == null) {
            ctx.getState().state = InteractionState.Failed;
            return;
        }
        Vector3d ownerPos = transform.getPosition();

        List<GlyphComponent> activeGlyphs = hexcaster.getActiveGlyphs();

        if (activeGlyphs == null || castingRootRef == null || !castingRootRef.isValid()) {
            // This should not happen, but just in case
            LOGGER.atWarning().log("Player is in casting mode but has no active glyphs");
            ctx.getState().state = InteractionState.Failed;
            return;
        }

        GlyphPositioner.PositionGlyphs(commandBuffer, playerRef, ownerPos, castingRootRef);

        // Glyph Hovering
        GlyphComponent hoveredGlyph = GlyphSelector.GetHoveredGlyph(commandBuffer, headRotation, activeGlyphs, hexcaster.getDraggingGlyph() != null);
        
        // Update hovered glyph state
        GlyphStyler.HoverGlyph(commandBuffer, hoveredGlyph, hexcaster);

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
