package com.riprod.hexcode.core.common.imbuement.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class ImbuedHotbarMarker implements Component<EntityStore> {

    private static ComponentType<EntityStore, ImbuedHotbarMarker> componentType;

    public ImbuedHotbarMarker() {
    }

    public static void setComponentType(ComponentType<EntityStore, ImbuedHotbarMarker> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, ImbuedHotbarMarker> getComponentType() {
        return componentType;
    }

    @Override
    public ImbuedHotbarMarker clone() {
        return new ImbuedHotbarMarker();
    }
}
