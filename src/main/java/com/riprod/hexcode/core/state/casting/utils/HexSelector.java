package com.riprod.hexcode.core.state.casting.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.utils.GlyphMath;

public class HexSelector {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static GlyphComponent findHoveredGlyph(CommandBuffer<EntityStore> accessor, Vector3f playerRotation,
            HexComponent hex) {

        return findHoveredGlyph(accessor, playerRotation, hex, List.of(hex.getHex().getFirstGlyphId()),
                new Vector3f(0, 0, 0), new ArrayList<>());
    }

    public static GlyphComponent findOutputChild(CommandBuffer<EntityStore> accessor, HexComponent hex,
            GlyphComponent targetGlyph) {
        if (hex == null || targetGlyph == null) return null;
        Hex hexData = hex.getHex();
        Glyph glyph = hexData.get(targetGlyph.getId());
        if (glyph == null) return null;
        List<String> nextLinks = glyph.getNextLinks();
        if (nextLinks.isEmpty()) return null;
        return findOutputDepthFirst(accessor, hex, hexData, nextLinks, new HashSet<>());
    }

    private static GlyphComponent findOutputDepthFirst(CommandBuffer<EntityStore> accessor, HexComponent hex,
            Hex hexData, List<String> glyphIds, Set<String> visited) {
        for (String id : glyphIds) {
            if (id == null || !visited.add(id)) continue;
            Ref<EntityStore> ref = hex.getChildGlyphRef(id);
            if (ref == null || !ref.isValid()) continue;
            GlyphComponent comp = accessor.getComponent(ref, GlyphComponent.getComponentType());
            if (comp == null) continue;
            if ("Glyph_Output".equals(comp.getGlyphId())) return comp;
            Glyph g = hexData.get(id);
            if (g == null) continue;
            List<String> next = g.getNextLinks();
            if (!next.isEmpty()) {
                GlyphComponent found = findOutputDepthFirst(accessor, hex, hexData, next, visited);
                if (found != null) return found;
            }
        }
        return null;
    }

    public static GlyphComponent findHoveredGlyph(CommandBuffer<EntityStore> accessor, Vector3f playerRotation,
            HexComponent hex, List<String> nextGlyphs, Vector3f parentRotation, List<String> visitedGlyphs) {

        List<Ref<EntityStore>> childGlyphRefs = hex.getChildGlyphRefs(nextGlyphs);

        for (int i = 0; i < childGlyphRefs.size(); i++) {
            Ref<EntityStore> childRef = childGlyphRefs.get(i);
            if (childRef == null || !childRef.isValid()) {
                continue;
            }
            GlyphComponent childGlyph = accessor.getComponent(childRef, GlyphComponent.getComponentType());
            if (childGlyph == null) {
                continue;
            }
            if (visitedGlyphs.contains(childGlyph.getId())) {
                continue;
            }
            visitedGlyphs.add(childGlyph.getId());

            float absolutePitch = parentRotation.getPitch() + childGlyph.getPitch();
            float absoluteYaw = parentRotation.getYaw() + childGlyph.getYaw();
            Vector3f glyphRotation = new Vector3f(absolutePitch, absoluteYaw, 0);

            float angularDist = GlyphMath.calculateAngularDistance(playerRotation, glyphRotation);
            float selectionRadius = GlyphMath.getSelectionRadius(childGlyph.getScale());

            if (angularDist <= selectionRadius) {
                if (childGlyph.getNext() != null && !childGlyph.getNext().isEmpty()) {
                    GlyphComponent hoveredChild = findHoveredGlyph(accessor, playerRotation, hex,
                            childGlyph.getNext(), glyphRotation, visitedGlyphs);
                    if (hoveredChild != null) {
                        return hoveredChild;
                    }
                }

                return childGlyph;
            }
        }
        return null;
    }

    public static HexComponent findHoveredHex(CommandBuffer<EntityStore> accessor, Vector3f headRotation,
            List<Ref<EntityStore>> hexes) {

        Vector3f playerRotation = new Vector3f(headRotation.getPitch(), headRotation.getYaw(), 0);

        for (int i = 0; i < hexes.size(); i++) {
            Ref<EntityStore> hexRef = hexes.get(i);
            if (!hexRef.isValid()) {
                continue;
            }
            HexComponent hex = accessor.getComponent(hexRef, HexComponent.getComponentType());
            if (hex == null || hex.isBeingDragged()) {
                continue;
            }

            Vector3f hexRotation = new Vector3f(hex.getPitch(), hex.getYaw(), 0);
            float angularDist = GlyphMath.calculateAngularDistance(playerRotation, hexRotation);
            float selectionRadius = GlyphMath.getSelectionRadius(hex.getScale());

            if (angularDist <= selectionRadius) {
                return hex;
            }
        }
        return null;
    }

    public static void DragGlyph(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> playerRef,
            HexComponent glyph) {

        TransformComponent playerTransform = accessor.getComponent(playerRef,
                TransformComponent.getComponentType());

        HeadRotation headRotation = accessor.getComponent(playerRef, HeadRotation.getComponentType());

        PositionDraggedGlyph(accessor, playerRef, headRotation, playerTransform, glyph);
    }

    public static void PositionDraggedGlyph(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> playerRef,
            HeadRotation headRotation, TransformComponent playerTransform, HexComponent hexComponent) {
        TransformComponent glyphPos = accessor.getComponent(hexComponent.getSelfRef(),
                TransformComponent.getComponentType());

        Vector3f playerRotation = headRotation.getRotation();

        hexComponent.setYaw(playerRotation.getYaw());
        hexComponent.setPitch(playerRotation.getPitch());
        glyphPos.setRotation(new Vector3f(hexComponent.getPitch(), hexComponent.getYaw(), 0));

        // Update the rotation of the children to match the new rotation of the parent
        // glyph
        List<Ref<EntityStore>> children = hexComponent.getChildGlyphRefsList();
        for (int i = 0; i < children.size(); i++) {
            Ref<EntityStore> childRef = children.get(i);
            if (childRef == null || !childRef.isValid()) {
                continue;
            }

            TransformComponent childTransform = accessor.getComponent(childRef,
                    TransformComponent.getComponentType());
            if (childTransform == null) {
                continue;
            }
            childTransform.setRotation(new Vector3f(hexComponent.getPitch(), hexComponent.getYaw(), 0));
        }
    }

}
