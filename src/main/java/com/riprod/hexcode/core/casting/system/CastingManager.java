package com.riprod.hexcode.core.casting.system;

import java.util.List;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.casting.GlyphPositioner;
import com.riprod.hexcode.core.casting.GlyphSelector;
import com.riprod.hexcode.core.casting.GlyphSpawner;
import com.riprod.hexcode.core.casting.GlyphStyler;
import com.riprod.hexcode.core.execute.Compiler;
import com.riprod.hexcode.core.execute.component.HexGraph;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.glyphs.utils.CreateGlyph;
import com.riprod.hexcode.core.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.hexbook.component.HexBookComponent;
import com.riprod.hexcode.core.hexstaff.component.HexStaffComponent;
import com.riprod.hexcode.player.component.HexcasterComponent;
import com.riprod.hexcode.player.system.CasterInventory;

public class CastingManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static InteractionState EnterCastingMode(ComponentAccessor<EntityStore> accessor,
            HexcasterComponent hexcaster,
            Ref<EntityStore> playerRef) {

        HexStaffComponent staff = CasterInventory.getHexStaffComponent(accessor, playerRef);
        HexBookComponent book = CasterInventory.getHexBookComponent(accessor, playerRef);

        if (staff == null || book == null) {
            LOGGER.atSevere()
                    .log("Player is missing required HexStaffComponent or HexBookComponent to enter casting mode");
            return InteractionState.Failed;
        }

        List<GlyphComponent> glyphs = book.getGlyphs();
        String style = staff.getStyleId();

        TransformComponent transform = accessor.getComponent(playerRef, TransformComponent.getComponentType());
        Vector3d ownerPos = transform.getPosition();
        Ref<EntityStore> castingRootRef = CreateGlyph.createCastingRoot(accessor, ownerPos);

        hexcaster.setCastingRootRef(castingRootRef);

        LOGGER.atInfo().log("Spawning %d glyphs for player in casting mode", glyphs.size());

        List<GlyphComponent> spawnedGlyphs = GlyphSpawner.spawnGlyphs(accessor, playerRef, castingRootRef, glyphs,
                style);

        LOGGER.atInfo().log("Spawned %d glyphs for player in casting mode", spawnedGlyphs.size());
        hexcaster.setActiveGlyphs(spawnedGlyphs);
        return InteractionState.NotFinished;
    }

    private static void CleanupGlyphChildren(ComponentAccessor<EntityStore> accessor, GlyphComponent glyph) {
        // recursively remove all child glyphs of the given glyph
        try {
            List<GlyphComponent> children = glyph.getChildren();
            if (children != null) {
                for (GlyphComponent child : children) {
                    CleanupGlyphChildren(accessor, child);
                    Holder<EntityStore> childHolder = EntityStore.REGISTRY.newHolder();
                    accessor.removeEntity(child.getSelfRef(), childHolder, RemoveReason.REMOVE);
                }
            }
        } catch (Exception e) {
            LOGGER.atSevere().withCause(e).log("Failed to cleanup child glyphs for glyph with ref: " + glyph.getId());
        }
    }

    public static InteractionState ExitCastingMode(ComponentAccessor<EntityStore> accessor,
            HexcasterComponent hexcaster,
            Ref<EntityStore> playerRef) {

        // Despawn all active glyphs
        List<GlyphComponent> activeGlyphs = hexcaster.getActiveGlyphs();
        for (GlyphComponent glyph : activeGlyphs) {
            try {
                CleanupGlyphChildren(accessor, glyph);
                Holder<EntityStore> glyphHolder = EntityStore.REGISTRY.newHolder();
                accessor.removeEntity(glyph.getSelfRef(), glyphHolder, RemoveReason.REMOVE);
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e)
                        .log("Failed to despawn glyph entity with ref: " + glyph.getId());
            }
        }

        // remove casting root entity
        Ref<EntityStore> castingRootRef = hexcaster.getCastingRootRef();
        if (castingRootRef != null) {
            try {
                Holder<EntityStore> rootHolder = EntityStore.REGISTRY.newHolder();
                accessor.removeEntity(castingRootRef, rootHolder, RemoveReason.REMOVE);
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to despawn casting root entity");
            }
        }

        // Compile the last selected spell
        HexStaffComponent staff = CasterInventory.getHexStaffComponent(accessor, playerRef);
        GlyphComponent rootGlyph = hexcaster.getLastSelectedGlyph();

        if (rootGlyph == null) {
            LOGGER.atWarning().log("No root glyph found for compilation, skipping");
            return InteractionState.Finished;
        }

        if (staff == null) {
            LOGGER.atSevere().log("Player is missing HexStaffComponent, cannot compile spell!");
            return InteractionState.Failed;
        }

        HexGraph compiledGlyph = Compiler.compile(rootGlyph);

        staff.setActiveSpell(compiledGlyph);

        LOGGER.atInfo().log("Compiled spell with root glyph id: %s", rootGlyph.toString());

        // Set casting mode to false
        return InteractionState.Finished;
    }

    public static InteractionState CastingModeTick(ComponentAccessor<EntityStore> accessor,
            HexcasterComponent hexcaster, Ref<EntityStore> playerRef) {

        if (hexcaster == null || !hexcaster.isInCastingMode()) {
            return InteractionState.Finished;
        }

        Ref<EntityStore> castingRootRef = hexcaster.getCastingRootRef();

        TransformComponent transform = accessor.getComponent(playerRef,
                TransformComponent.getComponentType());
        HeadRotation headRotation = accessor.getComponent(playerRef, HeadRotation.getComponentType());
        if (transform == null || headRotation == null) {
            return InteractionState.Failed;
        }
        Vector3d ownerPos = transform.getPosition();

        List<GlyphComponent> activeGlyphs = hexcaster.getActiveGlyphs();

        if (activeGlyphs == null || castingRootRef == null || !castingRootRef.isValid()) {
            // This should not happen, but just in case
            LOGGER.atWarning().log("Player is in casting mode but has no active glyphs");
            return InteractionState.Failed;
        }

        GlyphPositioner.PositionGlyphs(accessor, playerRef, ownerPos, castingRootRef);

        // Glyph Hovering
        GlyphComponent hoveredGlyph = GlyphSelector.GetHoveredGlyph(accessor, headRotation, activeGlyphs,
                hexcaster.getDraggingGlyph() != null);

        // Update hovered glyph state
        GlyphStyler.HoverGlyph(accessor, hoveredGlyph, hexcaster);

        return InteractionState.NotFinished;
    }
}
