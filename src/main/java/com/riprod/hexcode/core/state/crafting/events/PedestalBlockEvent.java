package com.riprod.hexcode.core.state.crafting.events;

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
import com.riprod.hexcode.core.common.block.component.UnbreakableBlockComponent;
import com.riprod.hexcode.core.common.block.event.BlockBreakEvent;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalPlayerData;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;
import com.riprod.hexcode.core.state.crafting.entity.AnchorEntity;
import com.riprod.hexcode.core.state.crafting.system.ObeliskSystem;
import com.riprod.hexcode.core.state.crafting.utils.PedestalItemUtil;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.state.HexState;

import java.util.List;
import java.util.Map;
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

        if (pedestal.getStates().contains(PedestalState.CRAFTING)) {
            LOGGER.atInfo().log("pedestal at %s is crafting, cancelling break", pos);
            event.setCancelled(true);
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

        Map<String, PedestalPlayerData> playerDataList = pedestal.getPlayerData();

        if (playerDataList != null && !playerDataList.isEmpty()) {
            for (Map.Entry<String, PedestalPlayerData> entry : playerDataList.entrySet()) {
                String playerId = entry.getKey();
                PedestalPlayerData playerData = entry.getValue();
                if (playerData == null)
                    continue;

                ItemStack bookStack = playerData.getStoredBook();
                if (bookStack != null && !bookStack.isEmpty()) {
                    PedestalItemUtil.dropBookAtPosition(buffer, bookStack, pos);
                }

                Ref<EntityStore> bookRef = playerData.getBookDisplayRef();
                if (bookRef != null && bookRef.isValid()) {
                    buffer.removeEntity(bookRef, RemoveReason.REMOVE);
                    playerData.setBookDisplayRef(null);
                }

                Ref<EntityStore> essenceRef = playerData.getEssenceDisplayRef();
                if (essenceRef != null && essenceRef.isValid()) {
                    buffer.removeEntity(essenceRef, RemoveReason.REMOVE);
                    playerData.setEssenceDisplayRef(null);
                }

                if (!pedestal.isConsumeEssence() && playerData.getEssence() != null) {
                    PedestalItemUtil.dropEssenceAtPosition(buffer, playerData.getEssence(), pos);
                }

                playerData.setEssence(null);
                playerData.setState(PedestalState.IDLE);
                AnchorEntity.DespawnHexPreviews(buffer, pedestal, playerData);
                Ref<EntityStore> anchorRef = playerData.getAnchorRef();
                if (anchorRef != null && anchorRef.isValid()) {
                    buffer.removeEntity(anchorRef, RemoveReason.REMOVE);
                    playerData.setAnchorEntityRef(null);
                }
            }
            playerDataList.clear();
        }

        UnbreakableBlockComponent.unprotect(world, pos);
        ObeliskSystem.enterIdle(buffer, pedestal, world);

        LOGGER.atInfo().log("pedestal cleanup complete at %s", pos);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
