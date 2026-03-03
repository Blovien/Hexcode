package com.riprod.hexcode.core.state.crafting.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class PedestalAnchorComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, PedestalAnchorComponent> componentType;

    public PedestalAnchorComponent() {
    }

    public static void setComponentType(ComponentType<EntityStore, PedestalAnchorComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, PedestalAnchorComponent> getComponentType() {
        return componentType;
    }

    private Vector3i pedestalLoc;

    public Vector3i getPedestalLoc() {
        return pedestalLoc;
    }

    public void setPedestalLoc(Vector3i pedestalLoc) {
        this.pedestalLoc = pedestalLoc;
    }

    @Nonnull
    @Override
    public PedestalAnchorComponent clone() {
        PedestalAnchorComponent copy = new PedestalAnchorComponent();
        copy.pedestalLoc = this.pedestalLoc;
        return copy;
    }
}
