package com.riprod.hexcode.player.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.casting.GlyphStyler;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HexcasterComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, HexcasterComponent> componentType;

    // Casting Mode
    private boolean inCastingMode = false;
    private Ref<EntityStore> castingRootRef = null;
    @Nonnull
    private List<GlyphComponent> activeGlyphs = new ArrayList<>();
    private GlyphComponent draggingGlyph = null;
    private GlyphComponent hoveredGlyph = null;

    public HexcasterComponent() {
    }

    public static void setComponentType(ComponentType<EntityStore, HexcasterComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, HexcasterComponent> getComponentType() {
        return componentType;
    }

    public boolean isInCastingMode() {
        return inCastingMode;
    }

    public void setInCastingMode(boolean inCastingMode) {
        this.inCastingMode = inCastingMode;
    }

    public GlyphComponent getDraggingGlyph() {
        return draggingGlyph;
    }

    @Nullable
    public void setDraggingGlyph(GlyphComponent draggingGlyph) {

        if (this.draggingGlyph != null) {
            this.draggingGlyph.setDragState(false);
        }

        // state 1 - we were dragging a glyph and now we're not
        if (draggingGlyph == null && this.draggingGlyph != null) {
            this.draggingGlyph.setDragState(false);
            this.draggingGlyph = null;
            return;
        }

        // state 2 - passed null but not dragging
        if (draggingGlyph == null) {
            return;
        }

        // state 3 - we start dragging a glyph we were hovering over
        if (this.hoveredGlyph.equals(draggingGlyph)) {
            // if we were already dragging a glyph, put it back to normal state
            this.draggingGlyph = draggingGlyph;
            this.draggingGlyph.setDragState(true);
            return;
        }

        // state 4 - we start dragging a glyph we were not hovering over (should not
        // happen but just in case)
        this.draggingGlyph = draggingGlyph;
        this.draggingGlyph.setDragState(true);
    }

    public Ref<EntityStore> getCastingRootRef() {
        return castingRootRef;
    }

    public void setCastingRootRef(Ref<EntityStore> castingRootRef) {
        this.castingRootRef = castingRootRef;
    }

    public GlyphComponent getHoveredGlyph() {
        return hoveredGlyph;
    }

    @Nullable
    public void setHoveredGlyph(GlyphComponent hoveredGlyph) {
        this.hoveredGlyph = hoveredGlyph;
    }

    public List<GlyphComponent> getActiveGlyphs() {
        return activeGlyphs;
    }

    public void setActiveGlyphs(@Nonnull List<GlyphComponent> activeGlyphs) {
        this.activeGlyphs = activeGlyphs;
    }

    public void addActiveGlyph(GlyphComponent glyph) {
        this.activeGlyphs.add(glyph);
    }

    public void removeActiveGlyph(GlyphComponent glyph) {
        this.activeGlyphs.remove(glyph);
    }

    public void removeActiveGlyph(UUID glyphId) {
        this.activeGlyphs.removeIf(glyph -> glyph.getId().equals(glyphId));
    }

    @Nonnull
    @Override
    public HexcasterComponent clone() {
        HexcasterComponent copy = new HexcasterComponent();
        copy.inCastingMode = this.inCastingMode;
        copy.castingRootRef = this.castingRootRef;
        copy.activeGlyphs = new ArrayList<>(this.activeGlyphs);
        copy.draggingGlyph = this.draggingGlyph;
        copy.hoveredGlyph = this.hoveredGlyph;
        return copy;
    }
}
