package com.riprod.hexcode.api.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.state.HexState;

public class HexStateChangeEvent implements IEvent<Void> {

    private final Ref<EntityStore> playerRef;
    private final HexState previousState;
    private final HexState newState;

    public HexStateChangeEvent(Ref<EntityStore> playerRef, HexState previousState, HexState newState) {
        this.playerRef = playerRef;
        this.previousState = previousState;
        this.newState = newState;
    }

    public Ref<EntityStore> getPlayerRef() {
        return playerRef;
    }

    public HexState getPreviousState() {
        return previousState;
    }

    public HexState getNewState() {
        return newState;
    }
}
