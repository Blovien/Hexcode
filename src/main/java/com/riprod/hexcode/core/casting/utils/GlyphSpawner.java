package com.riprod.hexcode.core.casting.utils;

import java.util.List;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.glyphs.component.Glyph;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.glyphs.utils.CreateGlyph;
import com.riprod.hexcode.core.hexes.component.HexComponent;
import com.riprod.hexcode.utils.GlyphMath;

public class GlyphSpawner {
    public static void spawnGlyphs(ComponentAccessor<EntityStore> accessor, HexComponent hex, GlyphComponent glyph,
            Vector3d parentPos) {

        // create the glyph
        Ref<EntityStore> glyphRef = CreateGlyph.createGlyph(accessor, glyph, parentPos);
        glyph.setSelfRef(glyphRef); // backwards reference
        hex.addChildGlyphRef(glyph.getId(), glyphRef);

        List<Glyph> children = hex.getGlyphs(glyph.getNext());

        List<Vector3f> childRotations = GlyphMath.getChildRotations(children.size(), glyph.getScale());

        // Spawn the children
        for (int i = 0; i < children.size(); i++) {

            
            // get the required variables
            Glyph childGlyph = children.get(i);
            Vector3f childRotation = childRotations.get(i);
            if (hex.getChildGlyphRef(childGlyph.getId()) != null) {
                continue; // if the child glyph is already spawned elsewhere, skip it
            }

            GlyphComponent childGlyphComponent = new GlyphComponent(childGlyph);
            
            // set the relative offset based on the angle and distance
            childGlyphComponent.setRotation(childRotation);
            childGlyphComponent.setOffset(GlyphMath.toMountOffset(childRotation, glyph.getRotation()));

            if (children.size() == 1) {
                childGlyphComponent.setScale(glyph.getScale() * GlyphStyler.SCALE_SINGLE_GLYPH); // reduce scale of the last glyph by 20%
            } else {
                childGlyphComponent.setScale(glyph.getScale() * GlyphStyler.SCALE_MULTIPLIER); // reduce scale of children by 50% to avoid clipping
            }

            // ref setup
            childGlyphComponent.setParentRef(glyph.getSelfRef());
            childGlyphComponent.setHexRef(hex.getSelfRef());

            // spawn the child glyph
            spawnGlyphs(accessor, hex, childGlyphComponent, parentPos);
        }
    }
}
