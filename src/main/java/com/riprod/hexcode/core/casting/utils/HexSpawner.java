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
import com.riprod.hexcode.core.glyphs.utils.CreateHex;
import com.riprod.hexcode.core.hexes.component.Hex;
import com.riprod.hexcode.core.hexes.component.HexComponent;
import com.riprod.hexcode.utils.GlyphMath;

public class HexSpawner {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static List<HexComponent> spawnHexes(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ownerRef,
            Ref<EntityStore> castingRootRef,
            List<Hex> hexes,
            String styleId) {

        CastingStyle style = CastingStyleRegistry.getOrDefault(styleId);
        TransformComponent ownerTransform = accessor.getComponent(ownerRef, TransformComponent.getComponentType());
        Vector3d ownerPos = ownerTransform.getPosition();
        HeadRotation headRotation = accessor.getComponent(ownerRef, HeadRotation.getComponentType());
        Vector3f ownerRotation = headRotation.getRotation();

        List<Vector3f> positions = style.getInitialPositions(hexes.size(), ownerRotation.getYaw(),
                ownerRotation.getPitch());

        List<HexComponent> spawnedHexes = new ArrayList<>();

        for (int i = 0; i < hexes.size(); i++) {
            Hex hex = hexes.get(i);
            HexComponent hexComponent = new HexComponent(hex);
            Vector3f pos = positions.get(i);

            Vector3d cartesian = GlyphMath.sphericalToCartesian(pos);
            hexComponent.setOffset(new Vector3f((float) cartesian.x, (float) cartesian.y, (float) cartesian.z));
            hexComponent.setRotation(pos);
            hexComponent.setRootRef(castingRootRef);
            hexComponent.setParentRef(ownerRef);

            try {
                spawnHexes(accessor, ownerRef, hexComponent, ownerPos);
                spawnedHexes.add(hexComponent);
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e)
                        .log("[spawnHexes] failed to spawn hex[%d] '%s'", i, hexComponent.getHex());
            }
        }
        return spawnedHexes;

    }

    private static void spawnHexes(ComponentAccessor<EntityStore> buffer,
            Ref<EntityStore> ownerRef, HexComponent hex, Vector3d parentPos) {

        List<Ref<EntityStore>> hexRef = CreateHex.createHexEntity(buffer, hex, parentPos);
        hex.setChildHexRefs(hexRef);
    }

    public static void MergeGlyphs(ComponentAccessor<EntityStore> accessor, GlyphComponent selectedGlyph,
            GlyphComponent droppedOnGlyph, float eyeHeight) {

        selectedGlyph.setParentRef(droppedOnGlyph.getSelfRef());
        selectedGlyph.setScale(droppedOnGlyph.getScale() * 0.5f); // TODO: finalize child glyph scale
        selectedGlyph.setRootRef(droppedOnGlyph.getSelfRef());

        GlyphStyler.updateScale(accessor, selectedGlyph, selectedGlyph.getScale());

        List<GlyphComponent> children = droppedOnGlyph.getChildren();

        children.add(selectedGlyph);

        if (children != null) {
            // update existing children positions (location correct)
            GlyphMath.distributeChildAngles(children, droppedOnGlyph.getScale());

            for (int i = 0; i < children.size(); i++) {
                GlyphComponent child = children.get(i);

                float d = droppedOnGlyph.getDistance();
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

                GlyphStyler.updateScale(accessor, child, child.getScale());

                // replace mounted component to new position
                // Recursively update all children inside of the glyph
                UpdateGlyphRender(accessor, child);
            }
        }
    }
}
