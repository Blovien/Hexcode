package com.riprod.hexcode.player.state;

import java.util.List;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.casting.SpawnGlyphs;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.glyphs.utils.CreateGlyph;
import com.riprod.hexcode.core.hexbook.HexBookComponent;
import com.riprod.hexcode.core.hexstaff.HexStaffComponent;
import com.riprod.hexcode.player.component.HexcasterComponent;
import com.riprod.hexcode.player.system.CasterInventory;

public class CastingManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static Boolean EnterCastingMode(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> playerRef) {

        // Ensure player is not already in casting mode
        HexcasterComponent hexcaster = accessor.ensureAndGetComponent(playerRef, HexcasterComponent.getComponentType());

        if (hexcaster != null && hexcaster.isInCastingMode()) {
            return false;
        }

        HexStaffComponent staff = CasterInventory.getHexStaffComponent(accessor, playerRef);
        HexBookComponent book = CasterInventory.getHexBookComponent(accessor, playerRef);

        if (staff == null || book == null) {
            LOGGER.atSevere()
                    .log("Player is missing required HexStaffComponent or HexBookComponent to enter casting mode");
            return false;
        }

        List<GlyphComponent> glyphs = book.getGlyphs();
        String style = staff.getStyleId();

        TransformComponent transform = accessor.getComponent(playerRef, TransformComponent.getComponentType());
        Vector3d ownerPos = transform.getPosition();
        Ref<EntityStore> castingRootRef = CreateGlyph.createCastingRoot(accessor, ownerPos);

        hexcaster.setCastingRootRef(castingRootRef);

        LOGGER.atInfo().log("Spawning %d glyphs for player in casting mode", glyphs.size());

        List<GlyphComponent> spawnedGlyphs = SpawnGlyphs.spawnGlyphs(accessor, playerRef, castingRootRef, glyphs,
                style);

        LOGGER.atInfo().log("Spawned %d glyphs for player in casting mode", spawnedGlyphs.size());
        hexcaster.setActiveGlyphs(spawnedGlyphs);
        hexcaster.setInCastingMode(true);
        return true;
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

    public static void ExitCastingMode(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> playerRef) {
        HexcasterComponent hexcaster = accessor.getComponent(playerRef, HexcasterComponent.getComponentType());
        if (hexcaster == null || !hexcaster.isInCastingMode()) {
            return;
        }

        // Despawn all active glyphs
        List<GlyphComponent> activeGlyphs = hexcaster.getActiveGlyphs();
        for (GlyphComponent glyph : activeGlyphs) {
            try {
                // recursively cleanup child glyphs first just in case, then remove the glyph
                // entity itself
                CleanupGlyphChildren(accessor, glyph);
                Holder<EntityStore> glyphHolder = EntityStore.REGISTRY.newHolder();
                accessor.removeEntity(glyph.getSelfRef(), glyphHolder, RemoveReason.REMOVE);

                GlyphComponent glyphComp = glyphHolder.getComponent(GlyphComponent.getComponentType());
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e)
                        .log("Failed to despawn glyph entity with ref: " + glyph.getId());
            }
        }
        activeGlyphs.clear();

        // remove casting root entity
        Ref<EntityStore> castingRootRef = hexcaster.getCastingRootRef();
        if (castingRootRef != null) {
            try {
                Holder<EntityStore> rootHolder = EntityStore.REGISTRY.newHolder();
                accessor.removeEntity(castingRootRef, rootHolder, RemoveReason.REMOVE);
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to despawn casting root entity");
            }
            hexcaster.setCastingRootRef(null);
        }

        // Set casting mode to false
        hexcaster.setInCastingMode(false);
    }
}
