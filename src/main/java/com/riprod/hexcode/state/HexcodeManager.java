package com.riprod.hexcode.state;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.hexcaster.component.HexcasterComponent;

public abstract class HexcodeManager {

    public abstract void firstTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer);

    public abstract void lastTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer);

    public abstract void tick0(Ref<EntityStore> ref, HexcasterComponent comp, float dt,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer);

    public abstract void onPlayerJoin(Holder<EntityStore> holder, HexcasterComponent comp);

    public abstract void onPlayerLeave(PlayerRef playerRef);

    public abstract InteractionState onPrimaryEnter(Ref<EntityStore> ref, HexcasterComponent comp,
            ComponentAccessor<EntityStore> accessor);

    public abstract InteractionState onPrimaryTick(Ref<EntityStore> ref, HexcasterComponent comp,
            ComponentAccessor<EntityStore> accessor);

    public abstract InteractionState onPrimaryExit(Ref<EntityStore> ref, HexcasterComponent comp,
            ComponentAccessor<EntityStore> accessor);

    public final void tick(Ref<EntityStore> ref, HexcasterComponent comp, float dt,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        tick0(ref, comp, dt, store, buffer);
    }
}
