package com.riprod.hexcode.api.event;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class HoverChangeEvent implements IEvent<Void> {

    private final Ref<EntityStore> playerRef;
    @Nullable
    private final Ref<EntityStore> hoveredRef;
    @Nullable
    private final Ref<EntityStore> previousHoveredRef;

    public HoverChangeEvent(Ref<EntityStore> playerRef, @Nullable Ref<EntityStore> hoveredRef,
            @Nullable Ref<EntityStore> previousHoveredRef) {
        this.playerRef = playerRef;
        this.hoveredRef = hoveredRef;
        this.previousHoveredRef = previousHoveredRef;
    }

    public Ref<EntityStore> getPlayerRef() {
        return playerRef;
    }

    @Nullable
    public Ref<EntityStore> getHoveredRef() {
        return hoveredRef;
    }

    @Nullable
    public Ref<EntityStore> getPreviousHoveredRef() {
        return previousHoveredRef;
    }
}
