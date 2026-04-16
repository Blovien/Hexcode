package com.riprod.hexcode.core.common.block.event;

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
import com.riprod.hexcode.core.common.block.component.UnbreakableBlockComponent;
import com.riprod.hexcode.core.common.obelisk.component.ObeliskBlockComponent;
import com.riprod.hexcode.core.common.pedestal.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.session.HexcodeSessionComponent;
import com.riprod.hexcode.core.state.crafting.session.SessionUtils;
import com.riprod.hexcode.core.state.crafting.entity.AnchorEntity;
import com.hypixel.hytale.logger.HytaleLogger;

public class BlockBreakEvent extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public BlockBreakEvent() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer,
            @Nonnull BreakBlockEvent event) {
        try {
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
                cleanupPedestal(buffer, pedestal, world);
            }

            ObeliskBlockComponent obelisk = BlockModule.getComponent(
                    ObeliskBlockComponent.getComponentType(), world,
                    pos.x, pos.y, pos.z);
            if (obelisk != null) {
                handleObeliskBreak(world, obelisk, pos);
            }
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] BlockBreakEvent failed: %s", e.getMessage());
        }
    }

    private static void cleanupPedestal(CommandBuffer<EntityStore> buffer, PedestalBlockComponent pedestal, World world) {
        Ref<EntityStore> sessionRef = SessionUtils.getSessionRef(pedestal);
        if (sessionRef != null) {
            SessionUtils.endSession(buffer, sessionRef, world);
        }

        Ref<EntityStore> anchorRef = pedestal.getAnchorRef();
        if (anchorRef != null && anchorRef.isValid()) {
            buffer.tryRemoveEntity(anchorRef, RemoveReason.REMOVE);
            pedestal.setAnchorRef(null);
        }
    }

    private static void handleObeliskBreak(World world, ObeliskBlockComponent obelisk, Vector3i pos) {
        Vector3i pedestalLoc = obelisk.getRegisteredPedestalLoc();
        if (pedestalLoc == null) {
            return;
        }

        PedestalBlockComponent pedestal = BlockModule.getComponent(
                PedestalBlockComponent.getComponentType(), world,
                pedestalLoc.x, pedestalLoc.y, pedestalLoc.z);
        if (pedestal != null) {
            pedestal.removeObelisk(pos);
        }
        obelisk.clearRegistration();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
