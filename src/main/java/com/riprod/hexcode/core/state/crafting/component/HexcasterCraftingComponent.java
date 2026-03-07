package com.riprod.hexcode.core.state.crafting.component;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hover.component.HoverableType;

public class HexcasterCraftingComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, HexcasterCraftingComponent> componentType;

    public HexcasterCraftingComponent() {
    }

    public static void setComponentType(ComponentType<EntityStore, HexcasterCraftingComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, HexcasterCraftingComponent> getComponentType() {
        return componentType;
    }

    private Ref<EntityStore> pedestalRef;
    private Ref<EntityStore> headAnchorRef;

    // dragging and grabbing
    private Ref<EntityStore> draggingRef;
    private HoverableType draggedType;
    private HoverableType hoveredType;
    private Ref<EntityStore> hoveredRef;

    private Ref<EntityStore> detailsGlyphRef;
    private List<Ref<EntityStore>> detailSlotRefs = new ArrayList<>();
    private Ref<EntityStore> hexRootRef;
    private boolean removeWarning;
    private Ref<EntityStore> removeWarningRef;
    private int dragTickCount;

    public Ref<EntityStore> getPedestalRef() {
        return pedestalRef;
    }

    public void setPedestalRef(@Nullable Ref<EntityStore> pedestalRef) {
        this.pedestalRef = pedestalRef;
    }

    @Nullable
    public Ref<EntityStore> getHeadAnchorRef() {
        return headAnchorRef;
    }

    public void setHeadAnchorRef(@Nullable Ref<EntityStore> headAnchorRef) {
        this.headAnchorRef = headAnchorRef;
    }

    @Nullable
    public Ref<EntityStore> getDraggingRef() {
        return draggingRef;
    }

    public void setDraggingRef(@Nullable Ref<EntityStore> draggingNodeRef, HoverableType type) {
        this.draggingRef = draggingNodeRef;
        this.draggedType = type;
    }

    public HoverableType getDraggingType() {
        return draggedType;
    }

    @Nullable
    public Ref<EntityStore> getHoveredRef() {
        return hoveredRef;
    }

    public void setHoveredRef(@Nullable Ref<EntityStore> hoveredGlyphRef, HoverableType type) {
        this.hoveredRef = hoveredGlyphRef;
        this.hoveredType = type;
    }

    public HoverableType getHoveredType() {
        return this.hoveredType;
    }

    @Nullable
    public Ref<EntityStore> getDetailsGlyphRef() {
        return detailsGlyphRef;
    }

    public void setDetailsGlyphRef(@Nullable Ref<EntityStore> detailsGlyphRef) {
        this.detailsGlyphRef = detailsGlyphRef;
    }

    public List<Ref<EntityStore>> getDetailSlotRefs() {
        return detailSlotRefs;
    }

    public void setDetailSlotRefs(List<Ref<EntityStore>> detailSlotRefs) {
        this.detailSlotRefs = detailSlotRefs;
    }

    @Nullable
    public Ref<EntityStore> getHexRootRef() {
        return hexRootRef;
    }

    public void setHexRootRef(@Nullable Ref<EntityStore> hexRootRef) {
        this.hexRootRef = hexRootRef;
    }

    public boolean isRemoveWarning() {
        return removeWarning;
    }

    public void setRemoveWarning(boolean removeWarning) {
        this.removeWarning = removeWarning;
    }

    @Nullable
    public Ref<EntityStore> getRemoveWarningRef() {
        return removeWarningRef;
    }

    public void setRemoveWarningRef(@Nullable Ref<EntityStore> removeWarningRef) {
        this.removeWarningRef = removeWarningRef;
    }

    public int getDragTickCount() {
        return dragTickCount;
    }

    public void setDragTickCount(int dragTickCount) {
        this.dragTickCount = dragTickCount;
    }

    public void clearCraftingState() {
        this.headAnchorRef = null;
        this.draggingRef = null;
        this.hoveredRef = null;
        this.detailsGlyphRef = null;
        this.hoveredType = null;
        this.draggedType = null;
        this.detailSlotRefs = new ArrayList<>();
        this.removeWarning = false;
        this.removeWarningRef = null;
        this.dragTickCount = 0;
    }

    @Nonnull
    @Override
    public HexcasterCraftingComponent clone() {
        HexcasterCraftingComponent copy = new HexcasterCraftingComponent();
        copy.pedestalRef = this.pedestalRef;
        copy.headAnchorRef = this.headAnchorRef;
        copy.hoveredType = this.hoveredType;
        copy.draggedType = this.draggedType;
        copy.draggingRef = this.draggingRef;
        copy.hoveredRef = this.hoveredRef;
        copy.detailsGlyphRef = this.detailsGlyphRef;
        copy.detailSlotRefs = new ArrayList<>(this.detailSlotRefs);
        copy.hexRootRef = this.hexRootRef;
        copy.removeWarning = this.removeWarning;
        copy.removeWarningRef = this.removeWarningRef;
        copy.dragTickCount = this.dragTickCount;
        return copy;
    }
}
