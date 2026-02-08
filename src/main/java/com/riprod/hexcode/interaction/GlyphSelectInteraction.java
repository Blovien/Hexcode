package com.riprod.hexcode.interaction;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
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
import com.riprod.hexcode.core.casting.GlyphSelector;
import com.riprod.hexcode.core.casting.GlyphStyler;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.player.component.HexcasterComponent;
import com.riprod.hexcode.utils.GlyphMath;
import com.riprod.hexcode.utils.SphericalPosition;

import it.unimi.dsi.fastutil.floats.Float2ObjectOpenHashMap;

import javax.annotation.Nonnull;

public class GlyphSelectInteraction extends ChargingInteraction {
    private final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    public static final BuilderCodec<GlyphSelectInteraction> CODEC = BuilderCodec
            .builder(GlyphSelectInteraction.class, GlyphSelectInteraction::new, ChargingInteraction.ABSTRACT_CODEC)
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

    public GlyphSelectInteraction() {
    }

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type,
            @Nonnull InteractionContext accessor, @Nonnull CooldownHandler cooldown) {

        accessor.getState().state = InteractionState.NotFinished;

        CommandBuffer<EntityStore> commandBuffer = accessor.getCommandBuffer();
        if (commandBuffer == null) {
            accessor.getState().state = InteractionState.Failed;
            return;
        }

        Ref<EntityStore> playerRef = accessor.getEntity();
        if (playerRef == null || !playerRef.isValid()) {
            accessor.getState().state = InteractionState.Failed;
            return;
        }

        HexcasterComponent hexcaster = commandBuffer.getComponent(playerRef, HexcasterComponent.getComponentType());

        if (hexcaster == null) {
            accessor.getState().state = InteractionState.Failed;
            return;
        }

        if (firstRun) {
            GlyphComponent hoveredGlyph = hexcaster.getHoveredGlyph();

            if (hoveredGlyph == null) {
                accessor.getState().state = InteractionState.Failed;
                return;
            }

            // remove from hover state so it doesn't interfere with dragging state
            GlyphStyler.ExitHover(commandBuffer, hoveredGlyph);

            Ref<EntityStore> glyphRef = hoveredGlyph.getSelfRef();
            if (glyphRef != null && glyphRef.isValid()) {
                commandBuffer.removeComponent(glyphRef, MountedComponent.getComponentType());
            }

            // set it as dragging glyph
            hexcaster.setDraggingGlyph(hoveredGlyph);

            // defer to next tick
            accessor.getState().state = InteractionState.NotFinished;
            return;
        }

        if (hexcaster.getDraggingGlyph() == null) {
            // nothing to do
            accessor.getState().state = InteractionState.Finished;
            return;
        }

        if (!hexcaster.isInCastingMode()) {
            // shouldn't happen, but just in case
            hexcaster.setDraggingGlyph(null);
            accessor.getState().state = InteractionState.Finished;
            return;
        }

        // update dragging glyph position based on look direction
        GlyphSelector.DragGlyph(commandBuffer, playerRef, hexcaster.getDraggingGlyph());

        super.tick0(firstRun, time, type, accessor, cooldown);
    }

    @Override
    protected void simulateTick0(boolean firstRun, float time, @Nonnull InteractionType type,
            @Nonnull InteractionContext ctx, @Nonnull CooldownHandler cooldown) {
        // todo: update dragged glyph position to follow look direction (client
        // prediction)
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
