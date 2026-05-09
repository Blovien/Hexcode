package com.riprod.hexcode.core.common.imbuement.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// archetype-filter marker; present iff any hotbar slot carries imbuement
// metadata. zero fields, transient (no codec), updated reactively by
// ImbuementMarkerSystem on InventoryChangeEvent. consumers use this as a
// query filter so the ECS framework skips unimbued players entirely.
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
