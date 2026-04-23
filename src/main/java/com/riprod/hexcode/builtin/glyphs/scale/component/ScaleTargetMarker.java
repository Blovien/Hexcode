package com.riprod.hexcode.builtin.glyphs.scale.component;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ScaleTargetMarker implements Component<EntityStore> {

    private static ComponentType<EntityStore, ScaleTargetMarker> componentType;

    public static ComponentType<EntityStore, ScaleTargetMarker> getComponentType() {
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, ScaleTargetMarker> type) {
        componentType = type;
    }

    private Ref<EntityStore> triggerRef;

    public ScaleTargetMarker() {
    }

    public ScaleTargetMarker(Ref<EntityStore> triggerRef) {
        this.triggerRef = triggerRef;
    }

    public Ref<EntityStore> getTriggerRef() {
        return triggerRef;
    }

    public void setTriggerRef(Ref<EntityStore> triggerRef) {
        this.triggerRef = triggerRef;
    }

    @Nonnull
    @Override
    public ScaleTargetMarker clone() {
        return new ScaleTargetMarker(triggerRef);
    }
}
