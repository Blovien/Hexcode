package com.riprod.hexcode.core.state.crafting.entity;

import java.util.List;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.glyphs.utils.CreateGlyph;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hidden.utils.HiddenUtils;
import com.riprod.hexcode.core.state.casting.utils.GlyphStyler;
import com.riprod.hexcode.utils.GlyphMath;

public class GlyphSpawner {

    private static final float GLYPH_HEIGHT_MULTIPLIER = 0.2f;

    public static void spawnGlyphs(CommandBuffer<EntityStore> accessor, HexComponent hex, GlyphComponent glyph,
            Vector3d parentPos, Vector3f parentRot, Ref<EntityStore> playerRef) {

        // create the glyph
        Ref<EntityStore> glyphRef = CreateGlyph.createGlyph(accessor, glyph, parentPos, parentRot, playerRef);
        glyph.setSelfRef(glyphRef); // backwards reference
        hex.addChildGlyphRef(glyph.getId(), glyphRef);

        List<Glyph> children = hex.getGlyphs(glyph.getNext());

        List<Vector3f> childRotations = GlyphMath.getChildRotations(children.size(), glyph.getScale(),
                glyph.getRotation().getZ());

        // Spawn the children
        for (int i = 0; i < children.size(); i++) {

            // get the required variables
            Glyph childGlyph = children.get(i);
            Vector3f childRotation = childRotations.get(i);
            if (hex.getChildGlyphRef(childGlyph.getId()) != null) {
                continue; // if the child glyph is already spawned elsewhere, skip it
            }

            GlyphComponent childGlyphComponent = new GlyphComponent(childGlyph.clone());

            if (children.size() == 1) {
                childGlyphComponent.setScale(glyph.getScale() * GlyphStyler.SCALE_SINGLE_GLYPH); // reduce scale of the
            } else {
                childGlyphComponent.setScale(glyph.getScale() * GlyphStyler.SCALE_MULTIPLIER); // reduce scale of
            }

            // set the relative offset based on the angle and distance
            childGlyphComponent.setRotation(childRotation);
            Vector3f offset = GlyphMath.toMountOffset(childRotation, childGlyph.getRotation());
            float yOffset = childGlyphComponent.getScale() * GLYPH_HEIGHT_MULTIPLIER;
            Vector3f scaledOffset = new Vector3f(offset).add(0, 0, yOffset);
            childGlyphComponent.setOffset(scaledOffset);
            
            // ref setup
            childGlyphComponent.setParentRef(glyph.getSelfRef());
            childGlyphComponent.setHexRef(hex.getSelfRef());

            // spawn the child glyph
            spawnGlyphs(accessor, hex, childGlyphComponent, parentPos, parentRot, playerRef);
        }
    }
}
