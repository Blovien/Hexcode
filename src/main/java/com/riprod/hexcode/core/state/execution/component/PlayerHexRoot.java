package com.riprod.hexcode.core.state.execution.component;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PlayerHexRoot implements HexRoot {
    private final Ref<EntityStore> playerRef;
    private final Ref<EntityStore> hexEntityRef;

    public PlayerHexRoot(Ref<EntityStore> playerRef, Ref<EntityStore> hexEntityRef) {
        this.playerRef = playerRef;
        this.hexEntityRef = hexEntityRef;
    }

    @Override
    public boolean isAlive() {
        return hexEntityRef.isValid();
    }

    @Override
    public Ref<EntityStore> getSourceRef() {
        return playerRef;
    }

    @Override
    public Ref<EntityStore> getRootEntityRef() {
        return hexEntityRef;
    }

    @Override
    public boolean tryConsumeMana(float cost, ComponentAccessor<EntityStore> accessor) {
        if (cost <= 0) return true;
        EntityStatMap statMap = accessor.getComponent(playerRef, EntityStatMap.getComponentType());
        if (statMap == null) return false;
        int manaIndex = DefaultEntityStatTypes.getMana();
        if (statMap.get(manaIndex).get() < cost) return false;
        statMap.subtractStatValue(manaIndex, cost);
        return true;
    }

    @Override
    public float getCurrentMana(ComponentAccessor<EntityStore> accessor) {
        EntityStatMap statMap = accessor.getComponent(playerRef, EntityStatMap.getComponentType());
        if (statMap == null) return 0f;
        return statMap.get(DefaultEntityStatTypes.getMana()).get();
    }
}
