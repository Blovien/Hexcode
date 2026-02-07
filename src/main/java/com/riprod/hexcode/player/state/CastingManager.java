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
import com.riprod.hexcode.player.component.HexcasterComponent;

public class CastingManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void EnterCastingMode(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> playerRef) {

        // Ensure player is not already in casting mode
        HexcasterComponent hexcaster = accessor.ensureAndGetComponent(playerRef, HexcasterComponent.getComponentType());

        if (hexcaster != null && hexcaster.isInCastingMode()) {
            return;
        }

        hexcaster.setInCastingMode(true);

        // Get the inventory of the player

        // Get the book of the player

        // Get the glyphs to spawn

        // Get the staff of the player

        // Get the casting style ID

        // Spawn the glyphs

        // Add the glyphRefs to the player's HexcasterComponent

        // mock glyphs for now
        List<GlyphComponent> mockGlyphs = List.of(
                new GlyphComponent("Blink"),
                new GlyphComponent("Cold"),
                new GlyphComponent("Death"),
                new GlyphComponent("Fire"),
                new GlyphComponent("Grow"),
                new GlyphComponent("Heal"),
                new GlyphComponent("Ice"),
                new GlyphComponent("Life"),
                new GlyphComponent("Plasma"),
                new GlyphComponent("Stamina"),
                new GlyphComponent("Velocity"));
        TransformComponent transform = accessor.getComponent(playerRef, TransformComponent.getComponentType());
        Vector3d ownerPos = transform.getPosition();
        Ref<EntityStore> castingRootRef = CreateGlyph.createCastingRoot(accessor, ownerPos);

        hexcaster.setCastingRootRef(castingRootRef);

        List<GlyphComponent> glyphs = SpawnGlyphs.spawnGlyphs(accessor, playerRef, castingRootRef, mockGlyphs, "ring");
        hexcaster.setActiveGlyphs(glyphs);
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
                // recursively cleanup child glyphs first just in case, then remove the glyph entity itself
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
