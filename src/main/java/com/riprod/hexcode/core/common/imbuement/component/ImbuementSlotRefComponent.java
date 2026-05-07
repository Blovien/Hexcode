package com.riprod.hexcode.core.common.imbuement.component;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ImbuementSlotRefComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, ImbuementSlotRefComponent> componentType;

    public static void setComponentType(ComponentType<EntityStore, ImbuementSlotRefComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, ImbuementSlotRefComponent> getComponentType() {
        return componentType;
    }

    private String slotKey;

    public ImbuementSlotRefComponent() {
    }

    public ImbuementSlotRefComponent(String slotKey) {
        this.slotKey = slotKey;
    }

    public String getSlotKey() {
        return slotKey;
    }

    public void setSlotKey(String slotKey) {
        this.slotKey = slotKey;
    }

    @Nonnull
    @Override
    public ImbuementSlotRefComponent clone() {
        return new ImbuementSlotRefComponent(this.slotKey);
    }
}
