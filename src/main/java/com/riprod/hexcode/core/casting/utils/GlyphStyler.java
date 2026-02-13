package com.riprod.hexcode.core.casting.utils;

import java.util.List;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.hexcaster.component.HexcasterComponent;

public class GlyphStyler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static void HoverGlyph(ComponentAccessor<EntityStore> accessor, GlyphComponent hoveredGlyph,
            HexcasterComponent hexcaster) {
        GlyphComponent currentlyHovered = hexcaster.getHoveredGlyph();

        // state 1 - doing nothing (return)
        if (hoveredGlyph == null && currentlyHovered == null) {
            return;
        }

        // state 2 - stop hovering
        if (hoveredGlyph == null && currentlyHovered != null) {
            ExitHover(accessor, currentlyHovered);
            hexcaster.setHoveredGlyph(null);
            return;
        }

        // state 3 - start hovering
        if (hoveredGlyph != null && currentlyHovered == null) {
            hexcaster.setHoveredGlyph(hoveredGlyph);
            EnterHover(accessor, hoveredGlyph);
            return;
        }

        // state 4 - switch hovered glyph
        if (hoveredGlyph != null && currentlyHovered != null && !hoveredGlyph.equals(currentlyHovered)) {
            ExitHover(accessor, currentlyHovered);
            hexcaster.setHoveredGlyph(hoveredGlyph);
            EnterHover(accessor, hoveredGlyph);
            return;
        }
    }

    public static void EnterHover(ComponentAccessor<EntityStore> accessor, GlyphComponent hoveredGlyph) {
        try {

            UpdateScale(accessor, hoveredGlyph, hoveredGlyph.getScale() * 1.2f); // reset to original scale when not hovering

        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error entering hover state for glyph");
        }

    }

    public static void ExitHover(ComponentAccessor<EntityStore> accessor, GlyphComponent hoveredGlyph) {
        try {

            UpdateScale(accessor, hoveredGlyph, hoveredGlyph.getScale()); // reset to original scale when not

        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error exiting hover state for glyph");
        }
    }

    public static void UpdateScale(ComponentAccessor<EntityStore> accessor, GlyphComponent glyph, float newScale) {
        try {

            Ref<EntityStore> selfRef = glyph.getSelfRef();

            EntityScaleComponent scaleComponent = accessor.ensureAndGetComponent(selfRef, EntityScaleComponent.getComponentType());

            scaleComponent.setScale(newScale);
            
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error updating scale for glyph");
        }
    }

    public static void UpdateRecursiveScale(ComponentAccessor<EntityStore> accessor, GlyphComponent glyph, float newScale) {
        UpdateScale(accessor, glyph, newScale);

        List<GlyphComponent> children = glyph.getChildren();
        if (children != null) {
            for (GlyphComponent child : children) {
                UpdateRecursiveScale(accessor, child, newScale);
            }
        }
    }

    public static void EnterIdleAnim(ComponentAccessor<EntityStore> accessor, GlyphComponent glyph) {
        try {

            // add Idle animation component

        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Error entering Idle animation state for glyph");
        }
    }
}
