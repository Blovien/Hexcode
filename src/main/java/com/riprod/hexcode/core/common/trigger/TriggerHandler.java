package com.riprod.hexcode.core.common.trigger;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.trigger.component.TriggerComponent;
import com.riprod.hexcode.core.state.execution.component.HexSignal;

public interface TriggerHandler {
    /**
     * called every tick. return true to request immediate kill
     */
    boolean onTick(float dt, Ref<EntityStore> entityRef,
            ArchetypeChunk<EntityStore> chunk, int index,
            TriggerComponent trigger, HexSignal signal,
            CommandBuffer<EntityStore> buffer);

    /**
     * called once on first tick, after firstBranch fires (if any)
     */
    default void onFirstTick(Ref<EntityStore> entityRef, TriggerComponent trigger,
            HexSignal signal, CommandBuffer<EntityStore> buffer) {
    }

    /**
     * called before entity removal. do side effects here (VFX, block restore, etc.)
     * system handles waiter decrement + entity removal after this returns
     */
    default void onCleanup(Ref<EntityStore> entityRef, TriggerComponent trigger,
            HexSignal signal, CommandBuffer<EntityStore> buffer) {
    }
}
