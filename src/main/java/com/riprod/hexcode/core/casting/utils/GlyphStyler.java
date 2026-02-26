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
import com.riprod.hexcode.core.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.hexes.component.HexComponent;
import com.riprod.hexcode.utils.GlyphMath;

public class GlyphStyler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final float SCALE_PER_GLYPH = 0.05f; // increase scale by 5% per glyph
    public static final float SCALE_SINGLE_GLYPH = 0.45f; // if only 1 glyph, make it slightly smaller to avoid clipping
    public static final float SCALE_MULTIPLIER = 0.5f;

    public static void hoverGlyph(ComponentAccessor<EntityStore> accessor, HexComponent hoveredHex,
            HexcasterCastingComponent hexcasterCasting) {

        HexComponent currentlyHovered = hexcasterCasting.getHoveredHex();

        // state 1 - doing nothing (return)
        if (hoveredHex == null && currentlyHovered == null) {
            return;
        }

        // state 2 - stop hovering
        if (hoveredHex == null && currentlyHovered != null) {
            exitHover(accessor, currentlyHovered);
            hexcasterCasting.setHoveredHex(null);
            return;
        }

        // state 3 - start hovering
        if (hoveredHex != null && currentlyHovered == null) {
            hexcasterCasting.setHoveredHex(hoveredHex);
            enterHover(accessor, hoveredHex);
            return;
        }

        // state 4 - switch hovered glyph
        if (hoveredHex != null && currentlyHovered != null && !hoveredHex.equals(currentlyHovered)) {
            exitHover(accessor, currentlyHovered);
            hexcasterCasting.setHoveredHex(hoveredHex);
            enterHover(accessor, hoveredHex);
            return;
        }
    }

    public static void enterHover(ComponentAccessor<EntityStore> accessor, HexComponent hoveredGlyph) {
        try {

            updateScale(accessor, hoveredGlyph.getSelfRef(), hoveredGlyph.getScale() * 1.2f); // reset to original scale
                                                                                              // when not
            // hovering

        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error entering hover state for glyph");
        }

    }

    public static void exitHover(ComponentAccessor<EntityStore> accessor, HexComponent hoveredGlyph) {
        try {

            updateScale(accessor, hoveredGlyph.getSelfRef(), hoveredGlyph.getScale()); // reset to original scale when
                                                                                       // not

        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error exiting hover state for glyph");
        }
    }

    public static void updateScale(ComponentAccessor<EntityStore> accessor, GlyphComponent glyph, float newScale) {
        try {

            Ref<EntityStore> selfRef = glyph.getSelfRef();

            updateScale(accessor, selfRef, newScale);

        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error updating scale for glyph");
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

        int numGlyphs = hexComponent.getChildGlyphRefs().size();

        float scaleMultiplier = 1 + (numGlyphs * SCALE_PER_GLYPH); // increase scale by 5% per glyph

        // set the initial glyph's scale based on the number of children it has and the
        // depth there is
        parentGlyph.setScale(scaleMultiplier);
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

            List<Vector3f> childRotations = GlyphMath.getChildRotations(children.size(), parentGlyph.getScale());

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
