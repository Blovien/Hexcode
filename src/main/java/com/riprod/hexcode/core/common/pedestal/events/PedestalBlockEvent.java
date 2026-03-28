package com.riprod.hexcode.core.common.pedestal.events;

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
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.riprod.hexcode.core.state.crafting.component.CraftingData;
import com.riprod.hexcode.core.state.crafting.utils.CraftingDataUtil;
import com.riprod.hexcode.core.state.crafting.utils.PedestalItemUtil;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.obelisk.system.ObeliskSystem;
import com.riprod.hexcode.core.common.pedestal.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;
import com.riprod.hexcode.state.HexState;

import java.util.Set;

public class PedestalBlockEvent extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public PedestalBlockEvent() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer,
            @Nonnull BreakBlockEvent event) {

        try {
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

            Set<Ref<EntityStore>> activePlayers = pedestal.getActivePlayerRefs();
            for (Ref<EntityStore> playerRef : activePlayers) {
                if (playerRef == null || !playerRef.isValid())
                    continue;
                HexcasterComponent hexcaster = buffer.getComponent(playerRef, HexcasterComponent.getComponentType());
                if (hexcaster != null) {
                    hexcaster.requestStateChange(HexState.IDLE);
                }
            }
            activePlayers.clear();

            CraftingData pedestalData = pedestal.getCraftingDataComponent();
            if (pedestalData != null) {

                Ref<EntityStore> ownerRef = pedestalData.getOwnerRef();
                boolean ownerValid = ownerRef != null && ownerRef.isValid();

                ItemStack bookStack = pedestalData.getStoredBook();
                if (bookStack != null && !bookStack.isEmpty()) {
                    if (ownerValid) {
                        PedestalItemUtil.returnBookToPlayer(buffer, ownerRef, bookStack,
                                pedestalData.getBookSourceSlot());
                    } else {
                        PedestalItemUtil.dropBookAtPosition(buffer, bookStack, pos);
                    }
                }

                String essenceId = pedestalData.getEssence();
                if (!pedestal.isConsumeEssence() && essenceId != null) {
                    boolean returned = ownerValid
                            && PedestalItemUtil.returnEssenceToPlayer(buffer, ownerRef, essenceId);
                    if (!returned) {
                        PedestalItemUtil.dropEssenceAtPosition(buffer, essenceId, pos);
                    }
                }

                Ref<EntityStore> bookRef = pedestalData.getBookDisplayRef();
                if (bookRef != null && bookRef.isValid()) {
                    buffer.removeEntity(bookRef, RemoveReason.REMOVE);
                }
                Ref<EntityStore> essenceRef = pedestalData.getEssenceDisplayRef();
                if (essenceRef != null && essenceRef.isValid()) {
                    buffer.removeEntity(essenceRef, RemoveReason.REMOVE);
                }

                Ref<EntityStore> anchorRef = pedestalData.getAnchorRef();
                if (anchorRef != null && anchorRef.isValid()) {
                    buffer.removeEntity(anchorRef, RemoveReason.REMOVE);
                }
            }

            ObeliskSystem.updateState(buffer, pedestal, world, PedestalState.SELECTING, PedestalState.IDLE);

            LOGGER.atInfo().log("pedestal cleanup complete at %s", pos);
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] PedestalBlockEvent failed: %s", e.getMessage());
        }
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
