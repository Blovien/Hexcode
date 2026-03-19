package com.riprod.hexcode.api.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class EnterSelectingEvent implements IEvent<Void> {

    private final Ref<EntityStore> playerRef;
    private final Vector3i pedestalLocation;

    public EnterSelectingEvent(Ref<EntityStore> playerRef, Vector3i pedestalLocation) {
        this.playerRef = playerRef;
        this.pedestalLocation = pedestalLocation;
    }

    public Ref<EntityStore> getPlayerRef() {
        return playerRef;
    }

    public Vector3i getPedestalLocation() {
        return pedestalLocation;
    }
}
