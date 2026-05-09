package com.riprod.hexcode.core.common.triggers.registry;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.WorldEventSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.triggers.component.FireTriggerEvent;

// world-event system that drains FireTriggerEvent into the per-store
// TriggerListenerRegistry on the tick thread, with a CommandBuffer in scope.
public class FireTriggerSystem extends WorldEventSystem<EntityStore, FireTriggerEvent> {

    public FireTriggerSystem() {
        super(FireTriggerEvent.class);
    }

    @Override
    public void handle(@Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> buffer,
                       @Nonnull FireTriggerEvent event) {
        TriggerListenerRegistry registry = buffer.getResource(TriggerListenerRegistry.getResourceType());
        if (registry == null) return;
        registry.fire(buffer, event.getTriggerEvent());
    }
}
