package com.riprod.hexcode.core.casting.utils;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.utils.GlyphMath;
import com.riprod.hexcode.utils.SphericalPosition;

public class GlyphSelector {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static GlyphComponent GetHoveredGlyph(ComponentAccessor<EntityStore> accessor,
            HeadRotation headRotation, List<GlyphComponent> activeGlyphs) {
        return GetHoveredGlyph(accessor, headRotation, activeGlyphs, false);
    }

    public static GlyphComponent GetHoveredGlyph(ComponentAccessor<EntityStore> accessor,
            HeadRotation headRotation, List<GlyphComponent> activeGlyphs, Boolean recursive) {

        for (GlyphComponent glyphComp : activeGlyphs) {
            if (glyphComp.isBeingDragged()) {
                continue; // don't hover dragged glyphs
            }

            try {
                float playerYaw = headRotation.getRotation().getYaw();
                float playerPitch = headRotation.getRotation().getPitch();

                return findHoveredGlyph(playerYaw, playerPitch, activeGlyphs, 0f, 0f, recursive, true);
            } catch (Exception e) {
                // Just in case, don't want to crash the server if something goes wrong with one
                // glyph
                LOGGER.atWarning().withCause(e).log("Error checking hovered glyph");
            }
        }
        return null;
    }

    public static GlyphComponent findHoveredGlyph(float playerYaw, float playerPitch,
            List<GlyphComponent> glyphs, float parentYaw, float parentPitch, Boolean recursive, Boolean first) {

        for (int i = 0; i < glyphs.size(); i++) {
            GlyphComponent glyph = glyphs.get(i);
            if (glyph.isBeingDragged()) {
                continue;
            }

            float absoluteYaw = parentYaw + glyph.getYaw();
            float absolutePitch = parentPitch + glyph.getPitch();

            float angularDist = GlyphMath.calculateAngularDistance(
                    new SphericalPosition(playerYaw, playerPitch, 0),
                    new SphericalPosition(absoluteYaw, absolutePitch, 0));

            float selectionRadius = GlyphMath.getSelectionRadius(glyph.getScale());

            if (first) {
                // increase selection radius for the first level of glyphs to make it easier to
                // select a glyph when there are many
                selectionRadius *= 3f;
            }

            if (angularDist <= selectionRadius) {
                if (!glyph.getChildren().isEmpty() && recursive) {
                    GlyphComponent child = findHoveredGlyph(playerYaw, playerPitch,
                            glyph.getChildren(), absoluteYaw, absolutePitch, recursive, false);
                    if (child != null) {
                        return child;
                    }
                }
                return glyph;
            }
        }
        return null;
    }

    public static void DragGlyph(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> playerRef,
            GlyphComponent glyph) {
        TransformComponent playerTransform = accessor.getComponent(playerRef,
                TransformComponent.getComponentType());
        HeadRotation headRotation = accessor.getComponent(playerRef, HeadRotation.getComponentType());

        PositionDraggedGlyph(accessor, playerRef, headRotation, playerTransform, glyph);
    }

    public static void PositionDraggedGlyph(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> playerRef,
            HeadRotation headRotation, TransformComponent playerTransform, GlyphComponent glyph) {
        TransformComponent glyphPos = accessor.getComponent(glyph.getSelfRef(), TransformComponent.getComponentType());

        Vector3f playerRotation = headRotation.getRotation();
        Vector3d playerPos = playerTransform.getPosition();

        float eyeHeight = 0f;
        ModelComponent modelComp = accessor.getComponent(playerRef, ModelComponent.getComponentType());
        if (modelComp != null && modelComp.getModel() != null) {
            eyeHeight = modelComp.getModel().getEyeHeight(playerRef, accessor);
        }

        Vector3d eyePos = new Vector3d(playerPos.x, playerPos.y + eyeHeight, playerPos.z);
        Vector3d updatedPos = GlyphMath.sphericalToCartesian(eyePos, playerRotation.getYaw(),
                playerRotation.getPitch(), glyph.getDistance());

        glyphPos.setPosition(updatedPos);
        glyph.setYaw(playerRotation.getYaw());
        glyph.setPitch(playerRotation.getPitch());
        glyphPos.setRotation(new Vector3f(glyph.getPitch(), glyph.getYaw(), 0));

        List<GlyphComponent> children = new ArrayList<>();
        children.addAll(glyph.getChildren());
        Boolean hasChildren = !children.isEmpty();
        while (hasChildren) {
            for (int i = 0; i < children.size(); i++) {
                GlyphComponent child = children.get(i);

                TransformComponent childTransform = accessor.getComponent(child.getSelfRef(),
                        TransformComponent.getComponentType());
                childTransform.setRotation(new Vector3f(glyph.getPitch(), glyph.getYaw(), 0));
                children.remove(i);

                // Update the internal rotation of the child glyph based on the new position of the parent glyph
                child.setPitch(glyph.getPitch());
                child.setYaw(glyph.getYaw());

                children.addAll(child.getChildren());
            }

            if (children.isEmpty()) {
                hasChildren = false;
            }
        }
    }

}
