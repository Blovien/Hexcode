package com.riprod.hexcode.core.state.crafting.component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;


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

    private Ref<EntityStore> pedestalRef = null;
    private Ref<EntityStore> hoveredHexRef = null;

    public Ref<EntityStore> getPedestalRef() {
        return pedestalRef;
    }

    public void setPedestalRef(@Nullable Ref<EntityStore> pedestalRef) {
        this.pedestalRef = pedestalRef;
    }

    @Nullable
    public Ref<EntityStore> getHoveredHexRef() {
        return hoveredHexRef;
    }

    public void setHoveredHexRef(@Nullable Ref<EntityStore> hoveredHexRef) {
        this.hoveredHexRef = hoveredHexRef;
    }

    public void clearCraftingState() {
        this.pedestalRef = null;
        this.hoveredHexRef = null;
    }

    @Nonnull
    @Override
    public HexcasterCraftingComponent clone() {
        HexcasterCraftingComponent copy = new HexcasterCraftingComponent();
        copy.pedestalRef = this.pedestalRef;
        copy.hoveredHexRef = this.hoveredHexRef;
        return copy;
    }
}

