package com.riprod.hexcode.core.hexcaster.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.drawing.component.DrawnShapeComponent;
import com.riprod.hexcode.core.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.hexes.component.HexComponent;
import com.riprod.hexcode.state.HexState;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

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
    private List<HexComponent> activeHexes = new ArrayList<>();
    private List<HexComponent> hoveredChain = new ArrayList<>();
    private HexComponent draggingHex = null;
    private HexComponent hoveredHex = null;
    private HexComponent lastSelectedHex = null;

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

    public HexComponent getHoveredHex() {
        return hoveredHex;
    }

    public void setHoveredHex(@Nullable HexComponent hoveredHex) {
        this.hoveredHex = hoveredHex;
    }

    public HexComponent getDraggingHex() {
        return draggingHex;
    }

    public void setDraggingHex(@Nullable HexComponent draggingHex) {
        if (draggingHex != null) {
            setLastSelectedHex(draggingHex);
        }

        if (this.draggingHex != null) {
            this.draggingHex.setDragState(false);
        }

        if (draggingHex == null) {
            this.draggingHex = null;
            return;
        }

        this.draggingHex = draggingHex;
        this.draggingHex.setDragState(true);
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

    public List<HexComponent> getActiveHexes() {
        return activeHexes;
    }

    public void setActiveHexes(@Nonnull List<HexComponent> activeHexes) {
        this.activeHexes = activeHexes;
    }

    public void removeActiveHex(String hexId) {
        this.activeHexes.removeIf(hex -> hex.getId().equals(hexId));
    }

    public void setLastSelectedHex(HexComponent hex) {
        this.lastSelectedHex = hex;
    }

    public HexComponent getLastSelectedHex() {
        return this.lastSelectedHex;
    }

    public void clearCastingState() {
        this.castingRootRef = null;
        this.headAnchorRef = null;
        this.activeHexes.clear();
        this.hoveredChain.clear();
        this.draggingHex = null;
        this.hoveredHex = null;
        this.lastSelectedHex = null;
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
        copy.activeHexes = new ArrayList<>(this.activeHexes);
        copy.hoveredChain = new ArrayList<>(this.hoveredChain);
        copy.draggingHex = this.draggingHex;
        copy.hoveredHex = this.hoveredHex;
        copy.lastSelectedHex = this.lastSelectedHex;
        copy.drawnStrokes = new FloatArrayList(this.drawnStrokes);
        copy.drawnGlyphs = new ArrayList<>(this.drawnGlyphs);
        copy.trailRef = this.trailRef;
        copy.lastParticleSpawnMillis = this.lastParticleSpawnMillis;
        copy.drawStartTimeMillis = this.drawStartTimeMillis;
        copy.pedestalRef = this.pedestalRef;
        return copy;
    }
}
