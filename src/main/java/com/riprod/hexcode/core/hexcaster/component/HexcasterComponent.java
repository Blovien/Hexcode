package com.riprod.hexcode.core.hexcaster.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.drawing.component.DrawnShapeComponent;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.state.HexState;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HexcasterComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, HexcasterComponent> componentType;

    private HexState currentState = HexState.IDLE;
    private HexState pendingState = null;

    public HexState getState() {
        return currentState;
    }

    public void requestStateChange(HexState newMode) {
        if (this.currentState == newMode || this.pendingState == newMode) {
            return;
        }
        this.pendingState = newMode;
    }

    public HexState consumePendingState() {
        HexState previousState = this.pendingState;
        pendingState = null;
        return previousState;
    }

    public void applyState(HexState newState) {
        if (this.currentState == newState) {
            return;
        }
        this.currentState = newState;
    }

    // Casting Mode
    private Ref<EntityStore> castingRootRef = null;
    private Ref<EntityStore> headAnchorRef = null;
    @Nonnull
    private List<GlyphComponent> activeGlyphs = new ArrayList<>();
    private List<GlyphComponent> hoveredChain = new ArrayList<>();
    private GlyphComponent draggingGlyph = null;
    private GlyphComponent hoveredGlyph = null;
    private GlyphComponent lastSelectedGlyph = null;

    // Drawing Mode
    private FloatArrayList drawnStrokes = new FloatArrayList();
    private List<DrawnShapeComponent> drawnGlyphs = new ArrayList<>();
    private Ref<EntityStore> trailRef = null;
    private long lastParticleSpawnMillis = 0;
    private long drawStartTimeMillis = 0;

    // Crafting Mode
    private Ref<EntityStore> pedestalRef = null;

    public HexcasterComponent() {
    }

    public static void setComponentType(ComponentType<EntityStore, HexcasterComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, HexcasterComponent> getComponentType() {
        return componentType;
    }

    public GlyphComponent getHoveredGlyph() {
        return hoveredGlyph;
    }

    public void setHoveredGlyph(@Nullable GlyphComponent hoveredGlyph) {
        this.hoveredGlyph = hoveredGlyph;
    }

    public GlyphComponent getDraggingGlyph() {
        return draggingGlyph;
    }

    public void setDraggingGlyph(@Nullable GlyphComponent draggingGlyph) {
        if (draggingGlyph != null) {
            setLastSelectedGlyph(draggingGlyph);
        }

        if (this.draggingGlyph != null) {
            this.draggingGlyph.setDragState(false);
        }

        if (draggingGlyph == null) {
            this.draggingGlyph = null;
            return;
        }

        this.draggingGlyph = draggingGlyph;
        this.draggingGlyph.setDragState(true);
    }

    public Ref<EntityStore> getCastingRootRef() {
        return castingRootRef;
    }

    public void setCastingRootRef(Ref<EntityStore> castingRootRef) {
        this.castingRootRef = castingRootRef;
    }

    public Ref<EntityStore> getHeadAnchorRef() {
        return headAnchorRef;
    }

    public void setHeadAnchorRef(Ref<EntityStore> headAnchorRef) {
        this.headAnchorRef = headAnchorRef;
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

    public void setLastSelectedGlyph(GlyphComponent glyph) {
        this.lastSelectedGlyph = glyph;
    }

    public GlyphComponent getLastSelectedGlyph() {
        return this.lastSelectedGlyph;
    }

    public void clearCastingState() {
        this.castingRootRef = null;
        this.headAnchorRef = null;
        this.activeGlyphs.clear();
        this.hoveredChain.clear();
        this.draggingGlyph = null;
        this.hoveredGlyph = null;
        this.lastSelectedGlyph = null;
    }

    public void clearDrawingState() {
        this.hoveredChain.clear();
        this.drawnStrokes.clear();
        this.drawnGlyphs.clear();
        this.trailRef = null;
        this.lastParticleSpawnMillis = 0;
        this.drawStartTimeMillis = 0;
    }

    public Ref<EntityStore> getPedestalRef() {
        return pedestalRef;
    }

    public void setPedestalRef(@Nullable Ref<EntityStore> pedestalRef) {
        this.pedestalRef = pedestalRef;
    }

    public void clearCraftingState() {
        this.pedestalRef = null;
        this.hoveredChain.clear();
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

    public Long getLastParticleSpawnMillis() {
        return lastParticleSpawnMillis;
    }

    public void setLastParticleSpawnMillis(Long millis) {
        this.lastParticleSpawnMillis = millis;
    }

    public long getDrawStartTimeMillis() {
        return drawStartTimeMillis;
    }

    public void setDrawStartTimeMillis(long drawStartTimeMillis) {
        this.drawStartTimeMillis = drawStartTimeMillis;
    }

    @Nonnull
    @Override
    public HexcasterComponent clone() {
        HexcasterComponent copy = new HexcasterComponent();
        copy.currentState = this.currentState;
        copy.castingRootRef = this.castingRootRef;
        copy.headAnchorRef = this.headAnchorRef;
        copy.activeGlyphs = new ArrayList<>(this.activeGlyphs);
        copy.hoveredChain = new ArrayList<>(this.hoveredChain);
        copy.draggingGlyph = this.draggingGlyph;
        copy.hoveredGlyph = this.hoveredGlyph;
        copy.lastSelectedGlyph = this.lastSelectedGlyph;
        copy.drawnStrokes = new FloatArrayList(this.drawnStrokes);
        copy.drawnGlyphs = new ArrayList<>(this.drawnGlyphs);
        copy.trailRef = this.trailRef;
        copy.lastParticleSpawnMillis = this.lastParticleSpawnMillis;
        copy.drawStartTimeMillis = this.drawStartTimeMillis;
        copy.pedestalRef = this.pedestalRef;
        return copy;
    }
}
