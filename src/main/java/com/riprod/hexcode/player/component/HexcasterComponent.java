package com.riprod.hexcode.player.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.drawing.component.DrawnShapeComponent;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HexcasterComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, HexcasterComponent> componentType;

    public enum HexcasterMode {
        IDLE,
        CASTING,
        DRAWING,
        CRAFTING
    }

    private HexcasterMode currentMode = HexcasterMode.IDLE;

    public HexcasterMode getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(HexcasterMode mode) {
        this.currentMode = mode;
    }

    // Casting Mode
    private Ref<EntityStore> castingRootRef = null;
    @Nonnull
    private List<GlyphComponent> activeGlyphs = new ArrayList<>();
    private List<GlyphComponent> hoveredChain = new ArrayList<>();
    private GlyphComponent draggingGlyph = null;
    private GlyphComponent hoveredGlyph = null;

    // Drawing Mode
    private FloatArrayList drawnStrokes = new FloatArrayList();
    private List<DrawnShapeComponent> drawnGlyphs = new ArrayList<>();
    private Ref<EntityStore> trailRef = null;

    // Crafting Mode

    public HexcasterComponent() {
    }

    public static void setComponentType(ComponentType<EntityStore, HexcasterComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, HexcasterComponent> getComponentType() {
        return componentType;
    }

    /**
     * -----------------------------
     * Global Methods
     * -----------------------------
     */

    public void setState(HexcasterMode newMode) {
        if (this.currentMode == newMode) {
            return;
        }
        fromState();
        this.currentMode = newMode;
        toState();
    }

    private void fromState() {
        switch (currentMode) {
            case IDLE -> fromIdle();
            case CASTING -> fromCastingMode();
            case CRAFTING -> fromCraftingMode();
            case DRAWING -> fromDrawingMode();
        }
    }

    private void toState() {
        switch (currentMode) {
            case IDLE -> toIdle();
            case CASTING -> toCastingMode();
            case CRAFTING -> toCraftingMode();
            case DRAWING -> toDrawingMode();
        }
    }

    public boolean isInIdleMode() {
        return currentMode == HexcasterMode.IDLE;
    }

    private void fromIdle() {
    }

    private void toIdle() {
        this.currentMode = HexcasterMode.IDLE;
        this.castingRootRef = null;
        this.activeGlyphs.clear();
        this.hoveredChain.clear();
        this.draggingGlyph = null;
        this.hoveredGlyph = null;

    }

    public GlyphComponent getHoveredGlyph() {
        return hoveredGlyph;
    }

    @Nullable
    public void setHoveredGlyph(GlyphComponent hoveredGlyph) {
        this.hoveredGlyph = hoveredGlyph;
    }

    public GlyphComponent getDraggingGlyph() {
        return draggingGlyph;
    }

    /**
     * -----------------------------
     * Casting Mode methods
     * -----------------------------
     */

    public boolean isInCastingMode() {
        return currentMode == HexcasterMode.CASTING;
    }

    private void toCastingMode() {
        this.currentMode = HexcasterMode.CASTING;
    }

    private void fromCastingMode() {
        this.castingRootRef = null;
        this.activeGlyphs.clear();
        this.hoveredChain.clear();
        this.draggingGlyph = null;
        this.hoveredGlyph = null;
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

    public void pushToChain(GlyphComponent glyph) {
        this.hoveredChain.add(glyph);
    }

    public void popFromChain() {
        if (!this.hoveredChain.isEmpty()) {
            this.hoveredChain.remove(this.hoveredChain.size() - 1);
        }
    }

    public void popFromChain(UUID glyphId) {
        if (!this.hoveredChain.isEmpty()) {
            this.hoveredChain.removeIf(glyph -> glyph.getId().equals(glyphId));
        }
    }

    public void isInChain(GlyphComponent glyph) {
        this.hoveredChain.contains(glyph);
    }

    /**
     * -----------------------------
     * Crafting Mode methods
     * -----------------------------
     */

    public boolean isInCraftingMode() {
        return currentMode == HexcasterMode.CRAFTING;
    }

    private void toCraftingMode() {
        this.currentMode = HexcasterMode.CRAFTING;
    }

    private void fromCraftingMode() {
        this.hoveredChain.clear();
    }

    /**
     * -----------------------------
     * Drawing Mode methods
     * -----------------------------
     */

    public boolean isInDrawingMode() {
        return currentMode == HexcasterMode.DRAWING;
    }

    private void toDrawingMode() {
        this.currentMode = HexcasterMode.DRAWING;
    }

    private void fromDrawingMode() {
        this.hoveredChain.clear();
        this.drawnStrokes.clear();
        this.drawnGlyphs.clear();
        if (this.trailRef != null) {
            // figure out how to despawn the trail entity
        }
    }

    public FloatArrayList getDrawnStrokes() {
        return drawnStrokes;
    }

    public List<DrawnShapeComponent> getDrawnGlyphs() {
        return drawnGlyphs;
    }

    public void addDrawnStroke(float[] stroke) {
        this.drawnStrokes.add(stroke[0]);
        this.drawnStrokes.add(stroke[1]);
    }

    public void addDrawnGlyph(DrawnShapeComponent glyph) {
        this.drawnGlyphs.add(glyph);
    }

    public void clearStrokes() {
        this.drawnStrokes.clear();
    }

    public void clearDrawing() {
        clearStrokes();
        this.drawnGlyphs.clear();
    }

    public void setTrailRef(Ref<EntityStore> trailRef) {
        this.trailRef = trailRef;
    }

    public void clearTrailRef() {
        this.trailRef = null;
    }

    public Ref<EntityStore> getTrailRef() {
        return trailRef;
    }

    @Nonnull
    @Override
    public HexcasterComponent clone() {
        HexcasterComponent copy = new HexcasterComponent();
        copy.currentMode = this.currentMode;
        copy.castingRootRef = this.castingRootRef;
        copy.activeGlyphs = new ArrayList<>(this.activeGlyphs);
        copy.draggingGlyph = this.draggingGlyph;
        copy.hoveredGlyph = this.hoveredGlyph;
        copy.drawnStrokes = new FloatArrayList(this.drawnStrokes);
        copy.drawnGlyphs = new ArrayList<>(this.drawnGlyphs);
        return copy;
    }
}
