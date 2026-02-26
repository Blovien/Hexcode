package com.riprod.hexcode.core.casting.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.casting.component.CastingStyle;
import com.riprod.hexcode.core.casting.registery.CastingStyleRegistry;
import com.riprod.hexcode.core.glyphs.component.Glyph;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.hexes.component.Hex;
import com.riprod.hexcode.core.hexes.component.HexComponent;
import com.riprod.hexcode.core.hexes.utils.CreateHex;
import com.riprod.hexcode.utils.GlyphMath;

public class HexSpawner {
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static List<Ref<EntityStore>> spawnHexes(CommandBuffer<EntityStore> accessor, Ref<EntityStore> ownerRef,
            Ref<EntityStore> castingRootRef,
            List<Hex> hexes,
            String styleId) {

        List<Ref<EntityStore>> spawnedHexes = new ArrayList<>();

        // Owner Info
        TransformComponent ownerTransform = accessor.getComponent(ownerRef, TransformComponent.getComponentType());
        Vector3d ownerPos = ownerTransform.getPosition();
        HeadRotation headRotation = accessor.getComponent(ownerRef, HeadRotation.getComponentType());
        Vector3f ownerRotation = headRotation.getRotation();

        // Hex Positions
        CastingStyle style = CastingStyleRegistry.getOrDefault(styleId);
        List<Vector3f> rotations = style.getInitialPositions(hexes.size(), ownerRotation.getYaw(),
                ownerRotation.getPitch());

        // Spawn Hexes
        for (int i = 0; i < hexes.size(); i++) {
            Hex hex = hexes.get(i);
            HexComponent hexComponent = new HexComponent(hex);
            Vector3f rot = rotations.get(i);
            Vector3d position = GlyphMath.sphericalToCartesian(rot);
            hexComponent.setRootRef(castingRootRef);
            hexComponent.setParentRef(castingRootRef);

            // spawning the hex
            hexComponent.setOffset(new Vector3f((float) position.x, (float) position.y, (float) position.z));
            hexComponent.setRotation(rot);
            Ref<EntityStore> hexRef = CreateHex.createHexEntity(accessor, hexComponent, ownerPos);
            hexComponent.setSelfRef(hexRef);
            spawnedHexes.add(hexRef);

            // getting the scale of the first glyph
            int numGlyphs = hex.getGlyphs().size();
            float scaleMultiplier = 1 + (numGlyphs * GlyphStyler.SCALE_PER_GLYPH); // increase scale by 5% per glyph

            // getting the first glyph
            String firstGlyphId = hex.getFirstGlyphId();
            Glyph firstGlyph = hex.get(firstGlyphId);
            GlyphComponent firstGlyphComponent = new GlyphComponent(firstGlyph);

            // setting up the first glyph
            firstGlyphComponent.setHexRef(hexRef);
            firstGlyphComponent.setParentRef(hexRef);
            firstGlyphComponent.setOffset(Vector3f.ZERO); // set to exactly where the hex is
            firstGlyphComponent.setRotation(rot);
            firstGlyphComponent.setScale(scaleMultiplier);
            hexComponent.setScale(scaleMultiplier);

            // spawns all of the children recursively and adds to the hexComponent reference
            GlyphSpawner.spawnGlyphs(accessor, hexComponent, firstGlyphComponent, ownerPos);
        }
        return spawnedHexes;

    }

    /** Just logically merges the hex into the hex tree */
    public static void MergeGlyphs(CommandBuffer<EntityStore> accessor, GlyphComponent droppedOnGlyph,
            HexComponent draggedHex, float eyeHeight) {

        HexComponent droppedOnHex = accessor.getComponent(droppedOnGlyph.getHexRef(), HexComponent.getComponentType());
        Hex hex1 = droppedOnHex.getHex();

        // automatically configures the references of the insertLoc -> draggedHex
        hex1.absorb(draggedHex.getHex(), droppedOnGlyph.getId());

        // add all of the children from the dragged hex to the dropped on hex
        Map<String, Ref<EntityStore>> droppedGlyphs = draggedHex.getChildGlyphRefs();
        droppedOnHex.addChildGlyphRefs(droppedGlyphs);
        String firstGlyphId = draggedHex.getHex().getFirstGlyphId();

        // get the first glyph ref
        Ref<EntityStore> firstGlyphRef = droppedGlyphs.get(firstGlyphId);
        GlyphComponent firstChildGlyph = accessor.getComponent(firstGlyphRef, GlyphComponent.getComponentType());

        firstChildGlyph.setParentRef(droppedOnGlyph.getSelfRef()); // update the parent ref of the first child glyph to the new parent glyph
        accessor.tryRemoveComponent(firstGlyphRef, MountedComponent.getComponentType()); // unmount the first glyph from the dragged hex

        for (Ref<EntityStore> childRef : droppedGlyphs.values()) {
            if (childRef == null || !childRef.isValid()) {
                continue;
            }
            GlyphComponent childGlyph = accessor.getComponent(childRef, GlyphComponent.getComponentType());
            childGlyph.setHexRef(droppedOnGlyph.getHexRef()); // update the hex ref of all of the children to the new hex
        }

        // mount the dragged glyph to the dropped on glyph
        MountedComponent mounted = new MountedComponent(droppedOnGlyph.getSelfRef(), Vector3f.ZERO,
                MountController.Minecart);
        accessor.putComponent(firstGlyphRef, MountedComponent.getComponentType(), mounted);

        // remove the old hex entity, the children will not be removed
        accessor.tryRemoveEntity(draggedHex.getSelfRef(), RemoveReason.REMOVE);

        // get first hex of the main tree for updating
        Ref<EntityStore> rootGlyph = droppedOnHex.getChildGlyphRef(hex1.getFirstGlyphId());
        GlyphComponent rootGlyphComponent = accessor.getComponent(rootGlyph, GlyphComponent.getComponentType());

        // update all children inside of the glyph recursively to new position / scale
        GlyphStyler.UpdateHexTree(accessor, droppedOnHex, rootGlyphComponent);
    }
}
