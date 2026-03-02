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
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.crafting.entity.AnchorEntity;
import com.riprod.hexcode.core.crafting.registry.PedestalBlockComponent;
import com.riprod.hexcode.core.crafting.utils.PedestalItemUtil;
import com.riprod.hexcode.core.crafting.utils.PedestalState;

public class PedestalBlockEvent extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

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

        PedestalBlockComponent pedestal = BlockModule.getComponent(
                PedestalBlockComponent.getComponentType(), world,
                pos.x, pos.y, pos.z);
        if (pedestal == null) {
            return;
        }

        LOGGER.atInfo().log("pedestal break detected at %s, state=%s", pos, pedestal.getState());

        if (pedestal.getState() == PedestalState.CRAFTING) {
            LOGGER.atInfo().log("pedestal at %s is crafting, cancelling break", pos);
            event.setCancelled(true);
            return;
        }

        AnchorEntity.DespawnHexPreviews(buffer, pedestal);
        ObeliskBlockEvent.unprotect(pos);

        ItemStack bookStack = pedestal.getStoredBook();
        if (bookStack != null && !bookStack.isEmpty()) {
            PedestalItemUtil.dropBookAtPosition(buffer, bookStack, pos);
            pedestal.setStoredBook(ItemStack.EMPTY);
            LOGGER.atInfo().log("dropped stored book at %s", pos);
        }

        Ref<EntityStore> bookRef = pedestal.getBookDisplayRef();
        if (bookRef != null && bookRef.isValid()) {
            buffer.removeEntity(bookRef, RemoveReason.REMOVE);
            pedestal.setBookDisplayRef(null);
        }

        Ref<EntityStore> essenceRef = pedestal.getEssenceDisplayRef();
        if (essenceRef != null && essenceRef.isValid()) {
            buffer.removeEntity(essenceRef, RemoveReason.REMOVE);
            pedestal.setEssenceDisplayRef(null);
        }

        Ref<EntityStore> anchorRef = pedestal.getAnchorRef();
        if (anchorRef != null && anchorRef.isValid()) {
            buffer.removeEntity(anchorRef, RemoveReason.REMOVE);
            pedestal.setAnchorEntityRef(null);
        }

        pedestal.setEssenceItemId(null);
        pedestal.setState(PedestalState.IDLE);

        LOGGER.atInfo().log("pedestal cleanup complete at %s", pos);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
