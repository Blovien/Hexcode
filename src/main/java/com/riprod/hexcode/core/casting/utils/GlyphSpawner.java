package com.riprod.hexcode.core.casting.utils;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.casting.component.CastingStyle;
import com.riprod.hexcode.core.casting.registery.CastingStyleRegistry;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.glyphs.utils.CreateGlyph;
import com.riprod.hexcode.utils.GlyphMath;
import com.riprod.hexcode.utils.SphericalPosition;

public class GlyphSpawner {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static List<GlyphComponent> spawnGlyphs(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ownerRef,
            Ref<EntityStore> castingRootRef,
            List<GlyphComponent> glyphs,
            String styleId) {

        CastingStyle style = CastingStyleRegistry.getOrDefault(styleId);
        TransformComponent ownerTransform = accessor.getComponent(ownerRef, TransformComponent.getComponentType());
        Vector3d ownerPos = ownerTransform.getPosition();
        HeadRotation headRotation = accessor.getComponent(ownerRef, HeadRotation.getComponentType());
        Vector3f ownerRotation = headRotation.getRotation();

        List<SphericalPosition> positions = style.getInitialPositions(glyphs.size(), ownerRotation.getYaw(),
                ownerRotation.getPitch());

        List<GlyphComponent> spawnedGlyphs = new ArrayList<>();

        for (int i = 0; i < glyphs.size(); i++) {
            GlyphComponent glyph = glyphs.get(i);
            SphericalPosition pos = positions.get(i);

            Vector3d cartesian = GlyphMath.sphericalToCartesian(pos);
            glyph.setOffset(new Vector3f((float) cartesian.x, (float) cartesian.y, (float) cartesian.z));
            glyph.setYaw(pos.getYaw());
            glyph.setPitch(pos.getPitch());
            glyph.setDistance(pos.getDistance());
            glyph.setRootRef(castingRootRef);
            glyph.setParentRef(ownerRef);

            try {
                GlyphComponent spawnedGlyph = spawnGlyph(accessor, ownerRef, pos, glyph, ownerPos);
                spawnedGlyphs.add(spawnedGlyph);
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e)
                        .log("[spawnGlyphs] failed to spawn glyph[%d] '%s'", i, glyph.getGlyphId());
            }
        }
        return spawnedGlyphs;

    }

    private static GlyphComponent spawnGlyph(ComponentAccessor<EntityStore> buffer,
            Ref<EntityStore> ownerRef, SphericalPosition pos, GlyphComponent glyph, Vector3d parentPos) {

        List<GlyphComponent> glyphRef = CreateGlyph.createGlyphEntity(buffer, glyph, parentPos);
        return glyphRef.get(0);
    }

    public static void MergeGlyphs(ComponentAccessor<EntityStore> accessor, GlyphComponent selectedGlyph,
            GlyphComponent droppedOnGlyph, float eyeHeight) {

        selectedGlyph.setParentRef(droppedOnGlyph.getSelfRef());
        selectedGlyph.setScale(droppedOnGlyph.getScale() * 0.5f); // TODO: finalize child glyph scale
        selectedGlyph.setRootRef(droppedOnGlyph.getSelfRef());

        GlyphStyler.UpdateScale(accessor, selectedGlyph, selectedGlyph.getScale());

        List<GlyphComponent> children = droppedOnGlyph.getChildren();

        children.add(selectedGlyph);

        if (children != null) {
            // update existing children positions (location correct)
            GlyphMath.distributeChildAngles(children, droppedOnGlyph.getScale());

            for (int i = 0; i < children.size(); i++) {
                GlyphComponent child = children.get(i);

                float d = (float) droppedOnGlyph.getDistance();
                child.setOffset(
                        -d * (float) Math.sin(child.getYaw()),
                        d * (float) Math.sin(child.getPitch()),
                        0f);

                // replace mounted component to new position
                accessor.putComponent(child.getSelfRef(), MountedComponent.getComponentType(),
                        new MountedComponent(child.getRootRef(),
                                child.getOffset(),
                                MountController.Minecart));

            }
        }
        // update all children inside of the glyph recursively to new position / scale
        UpdateGlyphRender(accessor, selectedGlyph);
    }

    /**
     * Updates the rendered positions of the child based on the new scale
     * 
     * @param accessor
     * @param parentGlyph
     */
    private static void UpdateGlyphRender(ComponentAccessor<EntityStore> accessor, GlyphComponent parentGlyph) {
        List<GlyphComponent> children = parentGlyph.getChildren();
        if (children != null) {
            GlyphMath.distributeChildAngles(children, parentGlyph.getScale());

            float scaleAmount = parentGlyph.getScale() * 0.5f;
            if (children.size() == 1) {
                scaleAmount = parentGlyph.getScale() * 0.45f;
            }

            for (int i = 0; i < children.size(); i++) {
                GlyphComponent child = children.get(i);

                child.setScale(scaleAmount);

                child.setOffset(child.getPitch(), child.getYaw(), 0);

                GlyphStyler.UpdateScale(accessor, child, child.getScale());

                // replace mounted component to new position
                // Recursively update all children inside of the glyph
                UpdateGlyphRender(accessor, child);
            }
        }
    }
}
