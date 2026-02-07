package com.riprod.hexcode.interaction;

import javax.annotation.Nonnull;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.Collector;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.casting.GlyphSelector;
import com.riprod.hexcode.core.casting.SpawnGlyphs;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.player.component.HexcasterComponent;
import com.riprod.hexcode.utils.GlyphMath;

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

        if (!firstRun) {
            ctx.getState().state = InteractionState.Finished;
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

        GlyphComponent draggedGlyph = hexcaster.getDraggingGlyph();
        if (draggedGlyph == null) {
            ctx.getState().state = InteractionState.Finished;
            return;
        }

        HeadRotation headRotation = commandBuffer.getComponent(playerRef, HeadRotation.getComponentType());
        if (headRotation == null) {
            ctx.getState().state = InteractionState.Failed;
            return;
        }

        GlyphComponent hoveredGlyph = GlyphSelector.GetHoveredGlyph(commandBuffer, headRotation,
                hexcaster.getActiveGlyphs(), true);

        // dropped on another glyph
        if (hoveredGlyph != null) {
            try {

                // get the eye height of the player
                float eyeHeight = 0f;
                ModelComponent modelComp = commandBuffer.getComponent(playerRef, ModelComponent.getComponentType());
                eyeHeight = modelComp.getModel().getEyeHeight(playerRef, commandBuffer);
                ///////// combine glyphs
                SpawnGlyphs.MergeGlyphs(commandBuffer, draggedGlyph, hoveredGlyph, eyeHeight);
                hexcaster.setDraggingGlyph(null);
                hexcaster.removeActiveGlyph(draggedGlyph.getId());
                ctx.getState().state = InteractionState.Finished;
                return;
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Error merging glyphs, dropping on ground instead");
            }
        }
        // dropped on empty space, just drop it back to original position (exit dragging
        // state)
        hexcaster.setDraggingGlyph(null);

        // get position from pitch/yaw of the glyph
        float pitch = draggedGlyph.getPitch();
        float yaw = draggedGlyph.getYaw();
        double distance = draggedGlyph.getDistance();
        // convert to cartesian coordinates
        Vector3d pos = GlyphMath.sphericalToCartesian(new Vector3d(0, 0, 0), yaw, pitch, distance);

        commandBuffer.putComponent(draggedGlyph.getSelfRef(), MountedComponent.getComponentType(),
                new MountedComponent(draggedGlyph.getRootRef(),
                        new Vector3f((float) pos.x, (float) pos.y, (float) pos.z),
                        MountController.Minecart));

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
