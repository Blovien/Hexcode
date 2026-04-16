package com.riprod.hexcode.core.state.crafting.session;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.pedestal.events.PedestalSystem;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;

public class SessionTickSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public Query<EntityStore> getQuery() {
        return HexcodeSessionComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

        Ref<EntityStore> sessionRef = chunk.getReferenceTo(index);
        try {
            HexcodeSessionComponent session = chunk.getComponent(index,
                    HexcodeSessionComponent.getComponentType());

            Ref<EntityStore> ownerRef = session.getOwnerRef();
            boolean ownerDead = ownerRef == null || !ownerRef.isValid();

            if (ownerDead) {
                boolean anyValid = false;
                for (Ref<EntityStore> pRef : session.getParticipantRefs()) {
                    if (pRef != null && pRef.isValid()) {
                        anyValid = true;
                        break;
                    }
                }
                if (!anyValid) {
                    World world = buffer.getExternalData().getWorld();
                    SessionUtils.endSession(buffer, sessionRef, world);
                    return;
                }
            }

            if (session.getActiveSlotIndex() >= 0) {
                session.setAutosaveTickCounter(session.getAutosaveTickCounter() + 1);
                if (session.getAutosaveTickCounter() >= HexcodeSessionComponent.AUTOSAVE_INTERVAL) {
                    PedestalSystem.saveHexToBook(store, ownerRef, session);
                    writeBookSnapshot(store, session);
                    session.setAutosaveTickCounter(0);
                }
            }
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] SessionTickSystem failed: %s", e.getMessage());
            try {
                Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
                buffer.removeEntity(sessionRef, holder, RemoveReason.REMOVE);
            } catch (Exception cleanup) {
                LOGGER.atSevere().log("[hexcode] SessionTickSystem cleanup failed: %s", cleanup.getMessage());
            }
        }
    }

    private void writeBookSnapshot(Store<EntityStore> store, HexcodeSessionComponent session) {
        Ref<EntityStore> ownerRef = session.getOwnerRef();
        if (ownerRef == null || !ownerRef.isValid()) return;

        ItemStack book = session.getStoredBook();
        if (book == null || book.isEmpty()) return;

        HexcasterCraftingComponent craftingComp = store.getComponent(ownerRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null) return;

        craftingComp.setPersistedBookSnapshot(book);
        craftingComp.setPersistedBookSourceSlot(session.getBookSourceSlot());
    }
}
