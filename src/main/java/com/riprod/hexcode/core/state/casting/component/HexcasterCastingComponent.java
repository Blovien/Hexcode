package com.riprod.hexcode.core.state.casting.component;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.EffectComponent;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;


public class HexcasterCastingComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, HexcasterCastingComponent> componentType;


    // Casting Mode
    private Ref<EntityStore> castingRootRef = null;
    private Ref<EntityStore> headAnchorRef = null;
    @Nonnull
    private List<Ref<EntityStore>> activeHexes = new ArrayList<>();
    private Ref<EntityStore> hoveredChain = null;
    private HexComponent draggingHex = null;
    private HexComponent hoveredHex = null;
    private EffectComponent hoveredGlyph = null;
    private HexComponent lastSelectedHex = null;

    public HexcasterCastingComponent() {
    }

    public static void setComponentType(ComponentType<EntityStore, HexcasterCastingComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, HexcasterCastingComponent> getComponentType() {
        return componentType;
    }

    public HexComponent getHoveredHex() {
        return hoveredHex;
    }

    public void setHoveredHex(@Nullable HexComponent hoveredHex) {
        this.hoveredHex = hoveredHex;
    }

    public EffectComponent getHoveredGlyph() {
        return hoveredGlyph;
    }

    public void setHoveredGlyph(@Nullable EffectComponent hoveredGlyph) {
        this.hoveredGlyph = hoveredGlyph;
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

    public Ref<EntityStore> getHeadAnchorRef() {
        return headAnchorRef;
    }

    public void setHeadAnchorRef(Ref<EntityStore> headAnchorRef) {
        this.headAnchorRef = headAnchorRef;
    }

    public Ref<EntityStore> getCastingRootRef() {
        return castingRootRef;
    }

    public void setCastingRootRef(Ref<EntityStore> castingRootRef) {
        this.castingRootRef = castingRootRef;
    }

    public List<Ref<EntityStore>> getActiveHexes() {
        return activeHexes;
    }

    public void setActiveHexes(@Nonnull List<Ref<EntityStore>> activeHexes) {
        this.activeHexes = activeHexes;
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
        this.hoveredChain = null;
        this.draggingHex = null;
        this.hoveredHex = null;
        this.hoveredGlyph = null;
        this.lastSelectedHex = null;
    }

    @Nonnull
    @Override
    public HexcasterCastingComponent clone() {
        HexcasterCastingComponent copy = new HexcasterCastingComponent();
        copy.castingRootRef = this.castingRootRef;
        copy.headAnchorRef = this.headAnchorRef;
        copy.activeHexes = new ArrayList<>(this.activeHexes);
        copy.hoveredChain = this.hoveredChain;
        copy.draggingHex = this.draggingHex;
        copy.hoveredHex = this.hoveredHex;
        copy.hoveredGlyph = this.hoveredGlyph;
        copy.lastSelectedHex = this.lastSelectedHex;
        return copy;
    }
}

