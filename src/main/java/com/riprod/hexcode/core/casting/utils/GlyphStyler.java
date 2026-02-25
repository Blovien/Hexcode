package com.riprod.hexcode.core.casting.utils;

import java.util.List;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.hexes.component.HexComponent;

public class GlyphStyler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void hoverGlyph(ComponentAccessor<EntityStore> accessor, HexComponent hoveredHex,
            HexcasterComponent hexcaster) {

        HexComponent currentlyHovered = hexcaster.getHoveredHex();

        // state 1 - doing nothing (return)
        if (hoveredHex == null && currentlyHovered == null) {
            return;
        }

        // state 2 - stop hovering
        if (hoveredHex == null && currentlyHovered != null) {
            exitHover(accessor, currentlyHovered);
            hexcaster.setHoveredHex(null);
            return;
        }

        // state 3 - start hovering
        if (hoveredHex != null && currentlyHovered == null) {
            hexcaster.setHoveredHex(hoveredHex);
            enterHover(accessor, hoveredHex);
            return;
        }

        // state 4 - switch hovered glyph
        if (hoveredHex != null && currentlyHovered != null && !hoveredHex.equals(currentlyHovered)) {
            exitHover(accessor, currentlyHovered);
            hexcaster.setHoveredHex(hoveredHex);
            enterHover(accessor, hoveredHex);
            return;
        }
    }

    public static void enterHover(ComponentAccessor<EntityStore> accessor, HexComponent hoveredGlyph) {
        try {

            updateScale(accessor, hoveredGlyph, hoveredGlyph.getScale() * 1.2f); // reset to original scale when not
                                                                                 // hovering

        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error entering hover state for glyph");
        }

    }

    public static void exitHover(ComponentAccessor<EntityStore> accessor, HexComponent hoveredGlyph) {
        try {

            updateScale(accessor, hoveredGlyph, hoveredGlyph.getScale()); // reset to original scale when not

        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error exiting hover state for glyph");
        }
    }

    public static void updateScale(ComponentAccessor<EntityStore> accessor, GlyphComponent glyph, float newScale) {
        try {

            Ref<EntityStore> selfRef = glyph.getSelfRef();

            EntityScaleComponent scaleComponent = accessor.ensureAndGetComponent(selfRef,
                    EntityScaleComponent.getComponentType());

            scaleComponent.setScale(newScale);

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

    public static void updateRecursiveScale(ComponentAccessor<EntityStore> accessor, HexComponent hex,
            float newScale) {
        updateScale(accessor, hex.getSelfRef(), newScale);
        
        List<Ref<EntityStore>> children = hex.getChildHexRefs();
        
        // Update all of the children to the new scale as well
        if (children != null) {
            for (Ref<EntityStore> childRef : children) {
                updateScale(accessor, childRef, newScale);
            }
        }
    }

    public static void enterIdleAnim(ComponentAccessor<EntityStore> accessor, HexComponent glyph) {
        try {

            // add Idle animation component

        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error entering Idle animation state for glyph");
        }
    }
}
