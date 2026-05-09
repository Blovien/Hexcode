package com.riprod.hexcode.core.common.imbuement.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// archetype-filter marker; present iff any equipped armor slot carries
// imbuement metadata. zero fields, transient, updated reactively by
// ImbuementMarkerSystem. used as the OnDamageReceivedSystem query so the
// ECS framework skips unimbued targets.
public final class ImbuedArmorMarker implements Component<EntityStore> {

    private static ComponentType<EntityStore, ImbuedArmorMarker> componentType;

    public ImbuedArmorMarker() {
    }

    public static void setComponentType(ComponentType<EntityStore, ImbuedArmorMarker> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, ImbuedArmorMarker> getComponentType() {
        return componentType;
    }

    @Override
    public ImbuedArmorMarker clone() {
        return new ImbuedArmorMarker();
    }
}
