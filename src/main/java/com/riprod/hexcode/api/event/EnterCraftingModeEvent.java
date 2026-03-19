package com.riprod.hexcode.api.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.pedestal.component.PedestalBlockComponent;

public class EnterCraftingModeEvent implements IEvent<Void> {

    private final Ref<EntityStore> playerRef;
    private final Hex hex;
    private final PedestalBlockComponent pedestalBlockComponent;

    public EnterCraftingModeEvent(Ref<EntityStore> playerRef, Hex hex,
            PedestalBlockComponent pedestalBlockComponent) {
        this.playerRef = playerRef;
        this.hex = hex;
        this.pedestalBlockComponent = pedestalBlockComponent;
    }

    public Ref<EntityStore> getPlayerRef() {
        return playerRef;
    }

    public Hex getHex() {
        return hex;
    }

    public PedestalBlockComponent getPedestalBlockComponent() {
        return pedestalBlockComponent;
    }
}
