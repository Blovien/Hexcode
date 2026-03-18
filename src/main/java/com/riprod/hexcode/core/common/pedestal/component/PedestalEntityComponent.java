package com.riprod.hexcode.core.common.pedestal.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class PedestalEntityComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, PedestalEntityComponent> componentType;

    public PedestalEntityComponent() {
    }

    public static void setComponentType(ComponentType<EntityStore, PedestalEntityComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, PedestalEntityComponent> getComponentType() {
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
    public PedestalEntityComponent clone() {
        PedestalEntityComponent copy = new PedestalEntityComponent();
        copy.pedestalLoc = this.pedestalLoc;
        return copy;
    }
}
