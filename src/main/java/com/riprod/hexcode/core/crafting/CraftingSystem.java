package com.riprod.hexcode.core.crafting;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.state.HexcodeManager;

public class CraftingSystem extends HexcodeManager {

    @Override
    public void firstTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
    }

    @Override
    public void lastTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        comp.clearCraftingState();
    }

    @Override
    public void tick0(Ref<EntityStore> ref, HexcasterComponent comp, float dt,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
    }

    @Override
    public void onPlayerJoin(Holder<EntityStore> holder, HexcasterComponent comp) {
    }

    @Override
    public void onPlayerLeave(PlayerRef playerRef) {
    }

    @Override
    public InteractionState onPrimaryEnter(Ref<EntityStore> ref, HexcasterComponent comp,
            ComponentAccessor<EntityStore> accessor) {
        return InteractionState.Finished;
    }

    @Override
    public InteractionState onPrimaryTick(Ref<EntityStore> ref, HexcasterComponent comp,
            ComponentAccessor<EntityStore> accessor) {
        return InteractionState.Finished;
    }

    @Override
    public InteractionState onPrimaryExit(Ref<EntityStore> ref, HexcasterComponent comp,
            ComponentAccessor<EntityStore> accessor) {
        return InteractionState.Finished;
    }
}
