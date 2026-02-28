package com.riprod.hexcode.core.casting.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.casting.component.HexcasterCastingComponent;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.hexes.component.HexComponent;
import com.riprod.hexcode.utils.GlyphMath;

public class GlyphStyler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final float SCALE_PER_GLYPH = 0.20f; // increase scale by 5% per glyph
    public static final float SCALE_SINGLE_GLYPH = 0.45f; // if only 1 glyph, make it slightly smaller to avoid clipping
    public static final float SCALE_MULTIPLIER = 0.5f;

    private static final float HOVER_SCALE = 1.2f;

    public static void hoverHex(ComponentAccessor<EntityStore> accessor, HexComponent hoveredHex,
            HexcasterCastingComponent castingComp) {

        HexComponent previous = castingComp.getHoveredHex();

        GlyphComponent hoveredGlyph = castingComp.getHoveredGlyph();

        if (hoveredGlyph != null) {
            // do nothing while hovering a specific glyph
            return;
        }

        if (previous == hoveredHex) return;

        if (previous != null) {
            exitHexHover(accessor, previous);
        }

        castingComp.setHoveredHex(hoveredHex);

        if (hoveredHex != null) {
            enterHexHover(accessor, hoveredHex);
        }
    }

    public static void hoverGlyph(ComponentAccessor<EntityStore> accessor, GlyphComponent hoveredGlyph,
            HexcasterCastingComponent castingComp) {

        GlyphComponent previous = castingComp.getHoveredGlyph();

        if (previous == hoveredGlyph) return;

        if (previous != null) {
            exitGlyphHover(accessor, previous);
        }

        castingComp.setHoveredGlyph(hoveredGlyph);

        if (hoveredGlyph != null) {
            enterGlyphHover(accessor, hoveredGlyph);
        }
    }

    private static void enterHexHover(ComponentAccessor<EntityStore> accessor, HexComponent hex) {
        try {
            hex.setHoverState(true);
            String firstGlyphId = hex.getHex().getFirstGlyphId();
            Ref<EntityStore> firstGlyphRef = hex.getChildGlyphRef(firstGlyphId);
            updateScale(accessor, firstGlyphRef, hex.getScale() * HOVER_SCALE);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error entering hex hover state");
        }
    }

    private static void exitHexHover(ComponentAccessor<EntityStore> accessor, HexComponent hex) {
        try {
            hex.setHoverState(false);
            String firstGlyphId = hex.getHex().getFirstGlyphId();
            Ref<EntityStore> firstGlyphRef = hex.getChildGlyphRef(firstGlyphId);
            updateScale(accessor, firstGlyphRef, hex.getScale());
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error exiting hex hover state");
        }
    }

    private static void enterGlyphHover(ComponentAccessor<EntityStore> accessor, GlyphComponent glyph) {
        try {
            glyph.setHoverState(true);
            updateScale(accessor, glyph.getSelfRef(), glyph.getScale() * HOVER_SCALE);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error entering glyph hover state");
        }
    }

    private static void exitGlyphHover(ComponentAccessor<EntityStore> accessor, GlyphComponent glyph) {
        try {
            glyph.setHoverState(false);
            updateScale(accessor, glyph.getSelfRef(), glyph.getScale());
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error exiting glyph hover state");
        }
    }

    public static void updateScale(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> selfRef, float newScale) {
        try {

            EntityScaleComponent scaleComponent = accessor.ensureAndGetComponent(selfRef,
                    EntityScaleComponent.getComponentType());

            scaleComponent.setScale(newScale);

        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error updating scale for glyph");
        }
    }

    public static void updateTransformPosition(ComponentAccessor<EntityStore> accessor, GlyphComponent glyph,
            Vector3d newPosition) {
        try {

            Ref<EntityStore> selfRef = glyph.getSelfRef();

            TransformComponent transform = accessor.getComponent(selfRef, TransformComponent.getComponentType());
            transform.setPosition(newPosition);

        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error updating position for glyph");
        }
    }

    public static void updateMountPosition(CommandBuffer<EntityStore> accessor, GlyphComponent glyph,
            Vector3f newPosition) {
        try {

            MountedComponent newMount = new MountedComponent(glyph.getParentRef(), newPosition,
                    MountController.Minecart);
            accessor.putComponent(glyph.getSelfRef(), MountedComponent.getComponentType(), newMount);

        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error updating mount position for glyph");
        }
    }

    public static void enterIdleAnim(ComponentAccessor<EntityStore> accessor, HexComponent glyph) {
        try {

            // add Idle animation component

        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error entering Idle animation state for glyph");
        }
    }

    /**
     * Updates the rendered positions of the child based on the new scale
     * 
     * @param accessor
     * @param hexComponent
     */
    public static void UpdateHexTree(CommandBuffer<EntityStore> accessor, HexComponent hexComponent,
            GlyphComponent parentGlyph) {

        int numGlyphs = hexComponent.getHex().getGlyphs().size();

        float scaleMultiplier = 1 + (numGlyphs * SCALE_PER_GLYPH); // increase scale by 5% per glyph

        // set the initial glyph's scale based on the number of children it has and the
        // depth there is
        parentGlyph.setScale(scaleMultiplier);
        hexComponent.setScale(scaleMultiplier);
        UpdateGlyphTree(accessor, hexComponent, parentGlyph, new HashSet<>());
    }

    /**
     * Updates the rendered positions of the child based on the new scale
     * 
     * @param accessor
     * @param hexComponent
     */
    private static void UpdateGlyphTree(CommandBuffer<EntityStore> accessor, HexComponent hexComponent,
            GlyphComponent parentGlyph, Set<String> styledGlyphs) {

        List<String> nextGlyphIds = parentGlyph.getNext();

        List<Ref<EntityStore>> children = hexComponent.getChildGlyphRefs(nextGlyphIds);

        if (children != null && !children.isEmpty()) {

            List<Vector3f> childRotations = GlyphMath.getChildRotations(children.size(), parentGlyph.getScale(), parentGlyph.getRotation().getZ());

            float scaleAmount = parentGlyph.getScale() * SCALE_MULTIPLIER;
            if (children.size() == 1) {
                scaleAmount = parentGlyph.getScale() * SCALE_SINGLE_GLYPH; // if only 1 child, make it slightly smaller to avoid
                // clipping
            }

            for (int i = 0; i < children.size(); i++) {
                Ref<EntityStore> childRef = children.get(i);
                GlyphComponent child = accessor.getComponent(childRef, GlyphComponent.getComponentType());
                Vector3f childRotation = childRotations.get(i);

                if (styledGlyphs.contains(child.getId())) {
                    continue; // if we've already styled this glyph, skip it to avoid infinite loops
                }
                styledGlyphs.add(child.getId());

                child.setScale(scaleAmount);
                child.setRotation(childRotation); // match rotation of parent
                child.setOffset(GlyphMath.toMountOffset(childRotation, parentGlyph.getRotation()));

                GlyphStyler.updateScale(accessor, childRef, child.getScale());
                GlyphStyler.updateMountPosition(accessor, child, child.getOffset());

                // Recursively update all children inside of the glyph
                UpdateGlyphTree(accessor, hexComponent, child, styledGlyphs);
            }
        }
    }
}
