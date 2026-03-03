package com.riprod.hexcode.state;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;

public abstract class HexcodeManager {

        public abstract void firstTick(Ref<EntityStore> ref, HexcasterComponent comp,
                        Store<EntityStore> store, CommandBuffer<EntityStore> buffer);

        public abstract void lastTick(Ref<EntityStore> ref, HexcasterComponent comp,
                        Store<EntityStore> store, CommandBuffer<EntityStore> buffer);

        public abstract void tick0(Ref<EntityStore> ref, HexcasterComponent comp, float dt,
                        Store<EntityStore> store, CommandBuffer<EntityStore> buffer);

        public abstract void onPlayerJoin(Holder<EntityStore> holder, HexcasterComponent comp);

        public abstract void onPlayerLeave(PlayerRef playerRef);

        public final void tick(Ref<EntityStore> ref, HexcasterComponent comp, float dt,
                        Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
                tick0(ref, comp, dt, store, buffer);
        }

        public abstract InteractionState exitInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref, HexcasterComponent comp);

        public abstract InteractionState enterInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref, HexcasterComponent comp);

        public abstract InteractionState tickInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref, float dt, HexcasterComponent comp);
}
