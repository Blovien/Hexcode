package com.riprod.hexcode.core.hexcaster.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.drawing.component.DrawnShapeComponent;
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

    // Drawing Mode
    private FloatArrayList drawnStrokes = new FloatArrayList();
    private List<DrawnShapeComponent> drawnGlyphs = new ArrayList<>();
    private Ref<EntityStore> trailRef = null;
    private long lastParticleSpawnMillis = 0;
    private long drawStartTimeMillis = 0;

    // Crafting Mode
    private Ref<EntityStore> pendingPedestalRef = null;

    public HexcasterComponent() {
    }

    public static void setComponentType(ComponentType<EntityStore, HexcasterComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, HexcasterComponent> getComponentType() {
        return componentType;
    }

    public void clearDrawingState() {
        this.drawnStrokes.clear();
        this.drawnGlyphs.clear();
        this.trailRef = null;
        this.lastParticleSpawnMillis = 0;
        this.drawStartTimeMillis = 0;
    }

    public Ref<EntityStore> getPendingPedestalRef() {
        return pendingPedestalRef;
    }

    public void setPendingPedestalRef(@Nullable Ref<EntityStore> ref) {
        this.pendingPedestalRef = ref;
    }

    public Ref<EntityStore> consumePendingPedestalRef() {
        Ref<EntityStore> ref = this.pendingPedestalRef;
        this.pendingPedestalRef = null;
        return ref;
    }

    public void clearCraftingState() {
        this.pendingPedestalRef = null;
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
        copy.drawnStrokes = new FloatArrayList(this.drawnStrokes);
        copy.drawnGlyphs = new ArrayList<>(this.drawnGlyphs);
        copy.trailRef = this.trailRef;
        copy.lastParticleSpawnMillis = this.lastParticleSpawnMillis;
        copy.drawStartTimeMillis = this.drawStartTimeMillis;
        copy.pendingPedestalRef = this.pendingPedestalRef;
        return copy;
    }
}
