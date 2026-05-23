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
import org.joml.Vector3f;
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

        return findHoveredGlyph(accessor, playerRotation.x, playerRotation.y, hex,
            List.of(hex.getHex().getFirstGlyphId()), 0f, 0f, new ArrayList<>());
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

    private static GlyphComponent findHoveredGlyph(CommandBuffer<EntityStore> accessor,
            float playerPitch, float playerYaw, HexComponent hex, List<String> nextGlyphs,
            float parentPitch, float parentYaw, List<String> visitedGlyphs) {

        for (int i = 0; i < nextGlyphs.size(); i++) {
            Ref<EntityStore> childRef = hex.getChildGlyphRef(nextGlyphs.get(i));
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

            float absolutePitch = parentPitch + childGlyph.x;
            float absoluteYaw = parentYaw + childGlyph.y;

            float angularDist = GlyphMath.calculateAngularDistance(playerPitch, playerYaw, absolutePitch, absoluteYaw);
            float selectionRadius = GlyphMath.getSelectionRadius(childGlyph.getScale());

            if (angularDist <= selectionRadius) {
                if (childGlyph.getNext() != null && !childGlyph.getNext().isEmpty()) {
                    GlyphComponent hoveredChild = findHoveredGlyph(accessor, playerPitch, playerYaw, hex,
                            childGlyph.getNext(), absolutePitch, absoluteYaw, visitedGlyphs);
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

        float playerPitch = headRotation.x;
        float playerYaw = headRotation.y;

        for (int i = 0; i < hexes.size(); i++) {
            Ref<EntityStore> hexRef = hexes.get(i);
            if (!hexRef.isValid()) {
                continue;
            }
            HexComponent hex = accessor.getComponent(hexRef, HexComponent.getComponentType());
            if (hex == null || hex.isBeingDragged()) {
                continue;
            }

            float angularDist = GlyphMath.calculateAngularDistance(playerPitch, playerYaw, hex.x, hex.y);
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

        hexComponent.y = playerRotation.y);
        hexComponent.setPitch(playerRotation.x);
        glyphPos.getRotation().assign(hexComponent.pitch(), hexComponent.yaw(), 0);

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
            childTransform.getRotation().assign(hexComponent.x, hexComponent.y, 0);
        }
    }

}
