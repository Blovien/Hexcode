package com.riprod.hexcode.core.state.crafting.component;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.CleanupUtils;

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

    private Ref<EntityStore> pedestalEntityRef;
    private Ref<EntityStore> headAnchorRef;

    // dragging and grabbing
    private Ref<EntityStore> draggingRef;
    private Ref<EntityStore> hoveredRef;

    private int dragTickCount;

    public Ref<EntityStore> getPedestalEntityRef() {
        return pedestalEntityRef;
    }

    public void setPedestalEntityRef(CommandBuffer<EntityStore> accessor, @Nullable Ref<EntityStore> pedestalRef) {
        CleanupUtils.safeRemoveEntity(accessor, this.pedestalEntityRef);
        this.pedestalEntityRef = pedestalRef;
    }

    public void setPedestalEntityRef(@Nullable Ref<EntityStore> pedestalRef) {
        this.pedestalEntityRef = pedestalRef;
    }

    @Nullable
    public Ref<EntityStore> getHeadAnchorRef() {
        return headAnchorRef;
    }

    public void setHeadAnchorRef(CommandBuffer<EntityStore> accessor, @Nullable Ref<EntityStore> headAnchorRef) {
        CleanupUtils.safeRemoveEntity(accessor, this.headAnchorRef);
        this.headAnchorRef = headAnchorRef;
    }

    public Ref<EntityStore> setHeadAnchorRef(@Nullable Ref<EntityStore> headAnchorRef) {
        Ref<EntityStore> oldRef = this.headAnchorRef;
        this.headAnchorRef = headAnchorRef;
        return oldRef;
    }

    @Nullable
    public Ref<EntityStore> getDraggingRef() {
        return draggingRef;
    }

    public void setDraggingRef(@Nullable Ref<EntityStore> draggingNodeRef) {
        this.draggingRef = draggingNodeRef;
    }

    @Nullable
    public Ref<EntityStore> getHoveredRef() {
        return hoveredRef;
    }

    public void setHoveredRef(@Nullable Ref<EntityStore> hoveredGlyphRef) {
        this.hoveredRef = hoveredGlyphRef;
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
        this.dragTickCount = 0;
    }

    @Nonnull
    @Override
    public HexcasterCraftingComponent clone() {
        HexcasterCraftingComponent copy = new HexcasterCraftingComponent();
        copy.pedestalEntityRef = this.pedestalEntityRef;
        copy.headAnchorRef = this.headAnchorRef;
        copy.draggingRef = this.draggingRef;
        copy.hoveredRef = this.hoveredRef;
        copy.dragTickCount = this.dragTickCount;
        return copy;
    }
}
