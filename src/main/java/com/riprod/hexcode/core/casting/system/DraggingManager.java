package com.riprod.hexcode.core.casting.system;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.casting.GlyphSelector;
import com.riprod.hexcode.core.casting.GlyphSpawner;
import com.riprod.hexcode.core.casting.GlyphStyler;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.player.component.HexcasterComponent;
import com.riprod.hexcode.utils.GlyphMath;

public class DraggingManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static InteractionState EnterDraggingMode(ComponentAccessor<EntityStore> accessor,
            HexcasterComponent hexcaster,
            Ref<EntityStore> playerRef) {

        GlyphComponent hoveredGlyph = hexcaster.getHoveredGlyph();
        if (hoveredGlyph == null) {
            return InteractionState.Failed;
        }

        // remove from hover state so it doesn't interfere with dragging state
        GlyphStyler.ExitHover(accessor, hoveredGlyph);

        Ref<EntityStore> glyphRef = hoveredGlyph.getSelfRef();
        if (glyphRef != null && glyphRef.isValid()) {
            accessor.removeComponent(glyphRef, MountedComponent.getComponentType());
        }

        // set it as dragging glyph
        hexcaster.setDraggingGlyph(hoveredGlyph);

        return InteractionState.NotFinished;
    }

    public static InteractionState ExitDraggingMode(ComponentAccessor<EntityStore> accessor,
            HexcasterComponent hexcaster, Ref<EntityStore> playerRef) {

        GlyphComponent draggedGlyph = hexcaster.getDraggingGlyph();
        if (draggedGlyph == null) {
            return InteractionState.Finished;
        }

        HeadRotation headRotation = accessor.getComponent(playerRef, HeadRotation.getComponentType());
        if (headRotation == null) {
            return InteractionState.Failed;
        }

        GlyphComponent hoveredGlyph = GlyphSelector.GetHoveredGlyph(accessor, headRotation,
                hexcaster.getActiveGlyphs(), true);

        // dropped on another glyph
        if (hoveredGlyph != null) {
            try {

                // get the eye height of the player
                float eyeHeight = 0f;
                ModelComponent modelComp = accessor.getComponent(playerRef,
                        ModelComponent.getComponentType());
                eyeHeight = modelComp.getModel().getEyeHeight(playerRef, accessor);
                ///////// combine glyphs
                GlyphSpawner.MergeGlyphs(accessor, draggedGlyph, hoveredGlyph, eyeHeight);
                hexcaster.setDraggingGlyph(null);
                hexcaster.removeActiveGlyph(draggedGlyph.getId());
                return InteractionState.Finished;
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

        accessor.putComponent(draggedGlyph.getSelfRef(), MountedComponent.getComponentType(),
                new MountedComponent(draggedGlyph.getRootRef(),
                        new Vector3f((float) pos.x, (float) pos.y, (float) pos.z),
                        MountController.Minecart));

        return InteractionState.Finished;
    }

    public static InteractionState DraggingModeTick(ComponentAccessor<EntityStore> accessor,
            HexcasterComponent hexcaster, Ref<EntityStore> playerRef) {
        if (hexcaster.getDraggingGlyph() == null) {
            // nothing to do
            return InteractionState.Finished;
        }

        if (!hexcaster.isInCastingMode()) {
            // shouldn't happen, but just in case
            hexcaster.setDraggingGlyph(null);
            return InteractionState.Finished;
        }

        // update dragging glyph position based on look direction
        GlyphSelector.DragGlyph(accessor, playerRef, hexcaster.getDraggingGlyph());

        return InteractionState.NotFinished;
    }
}
