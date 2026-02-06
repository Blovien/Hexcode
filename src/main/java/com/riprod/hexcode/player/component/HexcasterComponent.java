package com.riprod.hexcode.player.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class HexcasterComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, HexcasterComponent> componentType;

    // Casting Mode
    private boolean inCastingMode = false;
    private Ref<EntityStore> castingRootRef = null;
    @Nonnull
    private List<Ref<EntityStore>> activeGlyphRefs = new ArrayList<>();
    private Ref<EntityStore> draggingGlyphRef = null;

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

    public Ref<EntityStore> getDraggingGlyphRef() {
        return draggingGlyphRef;
    }

    public void setDraggingGlyphRef(Ref<EntityStore> draggingGlyphRef) {
        this.draggingGlyphRef = draggingGlyphRef;
    }

    public Ref<EntityStore> getCastingRootRef() {
        return castingRootRef;
    }

    public void setCastingRootRef(Ref<EntityStore> castingRootRef) {
        this.castingRootRef = castingRootRef;
    }

    public List<Ref<EntityStore>> getActiveGlyphRefs() {
        return activeGlyphRefs;
    }

    public void setActiveGlyphRefs(@Nonnull List<Ref<EntityStore>> activeGlyphRefs) {
        this.activeGlyphRefs = activeGlyphRefs;
    }

    public void addActiveGlyphRef(Ref<EntityStore> glyphRef) {
        this.activeGlyphRefs.add(glyphRef);
    }

    public void removeActiveGlyphRef(Ref<EntityStore> glyphRef) {
        this.activeGlyphRefs.remove(glyphRef);
    }


    @Nonnull
    @Override
    public HexcasterComponent clone() {
        HexcasterComponent copy = new HexcasterComponent();
        copy.inCastingMode = this.inCastingMode;
        copy.castingRootRef = this.castingRootRef;
        copy.activeGlyphRefs = new ArrayList<>(this.activeGlyphRefs);
        return copy;
    }
}
