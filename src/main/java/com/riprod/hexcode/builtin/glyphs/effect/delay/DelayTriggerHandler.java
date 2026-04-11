package com.riprod.hexcode.builtin.glyphs.effect.delay;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.trigger.TriggerHandler;
import com.riprod.hexcode.core.common.trigger.component.TriggerComponent;
import com.riprod.hexcode.core.state.execution.component.HexSignal;

public class DelayTriggerHandler implements TriggerHandler {

    @Override
    public boolean onTick(float dt, Ref<EntityStore> entityRef,
            ArchetypeChunk<EntityStore> chunk, int index,
            TriggerComponent trigger, HexSignal signal,
            CommandBuffer<EntityStore> buffer) {
        return false;
    }

    @Override
    public void onCleanup(Ref<EntityStore> entityRef, TriggerComponent trigger,
            HexSignal signal, CommandBuffer<EntityStore> buffer) {
        if (signal == null) return;
        signal.fireAllEntries(buffer);
    }
}
