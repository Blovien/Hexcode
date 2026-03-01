package com.riprod.hexcode.core.crafting.events;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.crafting.registry.PedestalBlockComponent;
import com.riprod.hexcode.core.crafting.spawners.AnchorSpawner;
import com.riprod.hexcode.core.crafting.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.crafting.utils.PedestalState;

public class PedestalBlockEvent extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    public PedestalBlockEvent() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer,
            @Nonnull BreakBlockEvent event) {

        if (event.isCancelled()) {
            return;
        }

        Vector3i pos = event.getTargetBlock();
        World world = buffer.getExternalData().getWorld();

        if (!PedestalBlockUtil.isPedestal(world.getBlockType(pos.x, pos.y, pos.z))) {
            return;
        }

        PedestalBlockComponent pedestal = BlockModule.getComponent(
                PedestalBlockComponent.getComponentType(), world,
                pos.x, pos.y, pos.z);
        if (pedestal == null) {
            return;
        }

        if (pedestal.getState() == PedestalState.CRAFTING) {
            event.setCancelled(true);
            return;
        }

        AnchorSpawner.DespawnHexPreviews(buffer, pedestal);

        ObeliskBlockEvent.unprotect(pos);

        Ref<EntityStore> bookRef = pedestal.getBookDisplayRef();
        if (bookRef != null && bookRef.isValid()) {
            buffer.removeEntity(bookRef, RemoveReason.REMOVE);
        }

        Ref<EntityStore> essenceRef = pedestal.getEssenceDisplayRef();
        if (essenceRef != null && essenceRef.isValid()) {
            buffer.removeEntity(essenceRef, RemoveReason.REMOVE);
        }

        Ref<EntityStore> anchorRef = pedestal.getAnchorRef();
        if (anchorRef != null && anchorRef.isValid()) {
            buffer.removeEntity(anchorRef, RemoveReason.REMOVE);
        }

        // todo: drop stored book as item entity
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
