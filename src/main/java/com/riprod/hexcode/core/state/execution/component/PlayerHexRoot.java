package com.riprod.hexcode.core.state.execution.component;

import com.hypixel.hytale.component.Ref;
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
}
