package com.riprod.hexcode.core.common.block.event;

import javax.annotation.Nonnull;

import java.util.List;
import java.util.Set;

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
import com.riprod.hexcode.core.common.block.component.UnbreakableBlockComponent;
import com.riprod.hexcode.core.common.pedestal.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.CraftingDataComponent;
import com.riprod.hexcode.core.state.crafting.entity.AnchorEntity;
import com.riprod.hexcode.core.state.crafting.utils.CraftingDataUtil;

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
            return;
        }

        PedestalBlockComponent pedestal = BlockModule.getComponent(
                PedestalBlockComponent.getComponentType(), world,
                pos.x, pos.y, pos.z);
        if (pedestal != null) {
            cleanupPedestal(buffer, pedestal);
        }
    }

    private static void cleanupPedestal(CommandBuffer<EntityStore> buffer, PedestalBlockComponent pedestal) {
        Set<Ref<EntityStore>> activePlayers = pedestal.getActivePlayerRefs();
        for (Ref<EntityStore> playerRef : activePlayers) {
            if (playerRef == null || !playerRef.isValid())
                continue;
            CraftingDataComponent playerData = buffer.getComponent(playerRef,
                    CraftingDataComponent.getComponentType());
            if (playerData == null)
                continue;

            AnchorEntity.DespawnHexPreviews(buffer, pedestal, playerData);

            List<Ref<EntityStore>> allRefs = playerData.getAllRefs();
            for (Ref<EntityStore> ref : allRefs) {
                if (ref != null && ref.isValid()) {
                    buffer.tryRemoveEntity(ref, RemoveReason.REMOVE);
                }
            }
        }

        Ref<EntityStore> pedestalEntityRef = pedestal.getPedestalEntityRef();
        if (pedestalEntityRef != null && pedestalEntityRef.isValid()) {
            CraftingDataComponent sharedData = buffer.getComponent(pedestalEntityRef,
                    CraftingDataComponent.getComponentType());
            if (sharedData != null) {
                AnchorEntity.DespawnHexPreviews(buffer, pedestal, sharedData);
                List<Ref<EntityStore>> allRefs = sharedData.getAllRefs();
                for (Ref<EntityStore> ref : allRefs) {
                    if (ref != null && ref.isValid()) {
                        buffer.tryRemoveEntity(ref, RemoveReason.REMOVE);
                    }
                }
            }
            buffer.tryRemoveEntity(pedestalEntityRef, RemoveReason.REMOVE);
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
