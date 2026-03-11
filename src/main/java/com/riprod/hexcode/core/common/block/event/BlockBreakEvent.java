package com.riprod.hexcode.core.common.block.event;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.block.component.UnbreakableBlockComponent;

public class BlockBreakEvent extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    public BlockBreakEvent() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer,
            @Nonnull BreakBlockEvent event) {
        Vector3i pos = event.getTargetBlock();
        World world = buffer.getExternalData().getWorld();

        UnbreakableBlockComponent comp = BlockModule.getComponent(
                UnbreakableBlockComponent.getComponentType(), world,
                pos.x, pos.y, pos.z);
        if (comp != null) {
            event.setCancelled(true);
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
