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
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.block.component.UnbreakableBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalDataComponent;
import com.riprod.hexcode.core.state.crafting.system.ObeliskSystem;
import com.riprod.hexcode.core.state.crafting.utils.PedestalDataUtil;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
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

        if (!pedestal.isPerPlayer()) { // if is not per player, drop the shared contents
            Ref<EntityStore> pedestalRef = pedestal.getPedestalEntityRef();
            if (pedestalRef != null && pedestalRef.isValid()) {
                PedestalDataComponent playerData = buffer.getComponent(pedestal.getPedestalEntityRef(),
                        PedestalDataComponent.getComponentType());

                if (playerData != null) {
                    PedestalDataUtil.dropContents(buffer, pedestal, playerData, pos);
                }

                buffer.removeEntity(pedestalRef, RemoveReason.REMOVE);
            }
        }

        Set<Ref<EntityStore>> playerRefs = pedestal.getActivePlayerRefs();

        if (playerRefs != null && !playerRefs.isEmpty()) {
            playerRefs.forEach(playerRef -> {

                if (pedestal.isPerPlayer()) {
                    HexcasterComponent hexcaster = buffer.getComponent(playerRef,
                            HexcasterComponent.getComponentType());
                    if (hexcaster == null)
                        return;

                    // Transition to idle for per-player cleaup logic
                    hexcaster.requestStateChange(HexState.IDLE);
                }
            });
            playerRefs.clear();
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
