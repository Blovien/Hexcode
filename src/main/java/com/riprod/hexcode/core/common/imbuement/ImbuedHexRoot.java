package com.riprod.hexcode.core.common.imbuement;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexRoot;

public class ImbuedHexRoot implements HexRoot {

    private final Ref<EntityStore> wielderRef;
    private final Ref<EntityStore> hexEntityRef;

    public ImbuedHexRoot(Ref<EntityStore> wielderRef, Ref<EntityStore> hexEntityRef) {
        this.wielderRef = wielderRef;
        this.hexEntityRef = hexEntityRef;
    }

    @Override
    public boolean isAlive() {
        return hexEntityRef.isValid();
    }

    @Override
    public Ref<EntityStore> getSourceRef() {
        return wielderRef;
    }

    @Override
    public Ref<EntityStore> getRootEntityRef() {
        return hexEntityRef;
    }

    @Override
    public boolean tryConsumeMana(float cost, ComponentAccessor<EntityStore> accessor) {
        if (cost <= 0) return true;
        EntityStatMap statMap = accessor.getComponent(wielderRef, EntityStatMap.getComponentType());
        if (statMap == null) return false;
        int manaIndex = DefaultEntityStatTypes.getMana();
        if (statMap.get(manaIndex).get() < cost) return false;
        statMap.subtractStatValue(manaIndex, cost);
        return true;
    }

    @Override
    public float getCurrentMana(ComponentAccessor<EntityStore> accessor) {
        EntityStatMap statMap = accessor.getComponent(wielderRef, EntityStatMap.getComponentType());
        if (statMap == null) return 0f;
        return statMap.get(DefaultEntityStatTypes.getMana()).get();
    }

    @Override
    public boolean addMana(float amount, ComponentAccessor<EntityStore> accessor) {
        if (amount <= 0) return false;
        EntityStatMap statMap = accessor.getComponent(wielderRef, EntityStatMap.getComponentType());
        if (statMap == null) return false;
        int manaIndex = DefaultEntityStatTypes.getMana();
        if (statMap.get(manaIndex).get() >= statMap.get(manaIndex).getMax()) return false;
        statMap.addStatValue(manaIndex, amount);
        return true;
    }
}
