package com.riprod.hexcode.builtin.glyphs.effect.domain.component;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class DomainAuraComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, DomainAuraComponent> componentType;

    public static ComponentType<EntityStore, DomainAuraComponent> getComponentType() {
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, DomainAuraComponent> type) {
        componentType = type;
    }

    private Ref<EntityStore> zoneRef;
    private float volatilityBoost;

    public DomainAuraComponent() {
    }

    public DomainAuraComponent(Ref<EntityStore> zoneRef, float volatilityBoost) {
        this.zoneRef = zoneRef;
        this.volatilityBoost = volatilityBoost;
    }

    public Ref<EntityStore> getZoneRef() {
        return zoneRef;
    }

    public float getVolatilityBoost() {
        return volatilityBoost;
    }

    @Nonnull
    @Override
    public DomainAuraComponent clone() {
        return new DomainAuraComponent(this.zoneRef, this.volatilityBoost);
    }
}
