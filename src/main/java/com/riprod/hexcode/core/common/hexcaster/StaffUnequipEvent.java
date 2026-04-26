package com.riprod.hexcode.core.common.hexcaster;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.SwitchActiveSlotEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.state.HexState;

public class StaffUnequipEvent extends EntityEventSystem<EntityStore, SwitchActiveSlotEvent> {

    public StaffUnequipEvent() {
        super(SwitchActiveSlotEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer,
            @Nonnull SwitchActiveSlotEvent event) {
        if (event.isCancelled()) return;

        HexcasterComponent hexcaster = chunk.getComponent(index, HexcasterComponent.getComponentType());
        if (hexcaster == null) return;

        HexState state = hexcaster.getState();
        if (state != HexState.CASTING) return;

        hexcaster.requestStateChange(HexState.IDLE);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return HexcasterComponent.getComponentType();
    }
}
