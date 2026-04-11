package com.riprod.hexcode.builtin.glyphs.effect.concentration.component;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ConcentrationTriggerComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, ConcentrationTriggerComponent> componentType;

    private Ref<EntityStore> casterRef;

    public ConcentrationTriggerComponent() {
    }

    public ConcentrationTriggerComponent(Ref<EntityStore> casterRef) {
        this.casterRef = casterRef;
    }

    public static void setComponentType(ComponentType<EntityStore, ConcentrationTriggerComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, ConcentrationTriggerComponent> getComponentType() {
        return componentType;
    }

    public Ref<EntityStore> getCasterRef() {
        return casterRef;
    }

    public void setCasterRef(Ref<EntityStore> casterRef) {
        this.casterRef = casterRef;
    }

    @Nonnull
    @Override
    public ConcentrationTriggerComponent clone() {
        return new ConcentrationTriggerComponent(this.casterRef);
    }
}
