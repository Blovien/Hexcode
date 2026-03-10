package com.riprod.hexcode.core.common.hidden.component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class HiddenComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, HiddenComponent> componentType;

    private Ref<EntityStore> ownerRef;

    public HiddenComponent() {
    }

    public HiddenComponent(Ref<EntityStore> ownerRef) {
        this.ownerRef = ownerRef;
    }

    public static void setComponentType(ComponentType<EntityStore, HiddenComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, HiddenComponent> getComponentType() {
        return componentType;
    }

    @Nullable
    public Ref<EntityStore> getOwnerRef() {
        return ownerRef;
    }

    public void setOwnerRef(@Nullable Ref<EntityStore> ownerRef) {
        this.ownerRef = ownerRef;
    }

    @Nonnull
    @Override
    public HiddenComponent clone() {
        HiddenComponent copy = new HiddenComponent();
        copy.ownerRef = this.ownerRef;
        return copy;
    }
}
