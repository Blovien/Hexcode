package com.riprod.hexcode.core.crafting.system;

import java.util.List;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.core.crafting.component.PedestalAnchorComponent;
import com.riprod.hexcode.core.crafting.component.PedestalState;
import com.riprod.hexcode.core.crafting.registry.PedestalBlockComponent;
import com.riprod.hexcode.core.crafting.spawners.PedestalSpawner;
import com.riprod.hexcode.core.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.state.HexState;

public class PedestalTickSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public Query<EntityStore> getQuery() {
        return PedestalAnchorComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> accessor,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {

        PedestalAnchorComponent anchor = accessor.getComponent(index, PedestalAnchorComponent.getComponentType());
        if (anchor == null || anchor.getPedestalLoc() == null) {
            return;
        }

        Vector3i pedestalPos = anchor.getPedestalLoc();

        PedestalBlockComponent pedestalBlock = BlockModule.getComponent(PedestalBlockComponent.getComponentType(),
                buffer.getExternalData().getWorld(), pedestalPos.getX(), pedestalPos.getY(), pedestalPos.getZ());

        if (pedestalBlock == null) {
            return;
        }

        switch (pedestalBlock.getState()) {
            case READY: // has all required components but not active yet
                tickReady(dt, pedestalBlock, buffer.getExternalData().getWorld(), buffer);
                break;
            case CRAFTING: // crafting
                tickCrafting(pedestalBlock, store, buffer);
            case ACTIVE: // has hexes placed but not crafting yet
                Ref<EntityStore> anchorRef = accessor.getReferenceTo(index);
                tickPlayerDetection(pedestalBlock, anchorRef, store, buffer);
                break;
            case OFF: // error state
                break;
        }
    }

    private void tickReady(float dt, PedestalBlockComponent pedestal, World world,
            CommandBuffer<EntityStore> buffer) {
    }

    private void tickPlayerDetection(PedestalBlockComponent pedestal, Ref<EntityStore> anchorRef,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

        Vector3i blockPos = pedestal.getLocation();
        if (blockPos == null) {
            return;
        }

        Vector3d anchorPos = PedestalSpawner.getAnchorPosition(blockPos);
        double maxRadius = pedestal.getMaxRadius();
        double maxRadiusSq = maxRadius * maxRadius;

        List<Ref<EntityStore>> nearbyEntities = TargetUtil.getAllEntitiesInSphere(anchorPos, maxRadius, buffer);

        // find nearby players and set them to CRAFTING if they are idle
        for (int i = 0; i < nearbyEntities.size(); i++) {
            Ref<EntityStore> entityRef = nearbyEntities.get(i);
            if (entityRef == null || !entityRef.isValid()) {
                continue;
            }

            HexcasterComponent hexcaster = buffer.getComponent(entityRef, HexcasterComponent.getComponentType());
            if (hexcaster == null) {
                continue;
            }

            if (hexcaster.getState() == HexState.IDLE || hexcaster.getState() == HexState.EXECUTION) {
                hexcaster.setPendingPedestalRef(anchorRef);
                hexcaster.requestStateChange(HexState.CRAFTING);
                pedestal.addDetectedPlayer(entityRef);
                LOGGER.atInfo().log("pedestal: auto-entering player %s into CRAFTING, anchor=%s", entityRef, anchorRef);
            }
        }

        // remove players that are out of range or no longer idle/crafting
        List<Ref<EntityStore>> activePlayers = pedestal.getActivePlayerRefs();
        for (int i = activePlayers.size() - 1; i >= 0; i--) {
            Ref<EntityStore> playerRef = activePlayers.get(i);
            if (playerRef == null || !playerRef.isValid()) {
                activePlayers.remove(i);
                continue;
            }

            HexcasterComponent hexcaster = buffer.getComponent(playerRef, HexcasterComponent.getComponentType());
            if (hexcaster == null || hexcaster.getState() != HexState.CRAFTING) {
                activePlayers.remove(i);
                continue;
            }

            TransformComponent transform = buffer.getComponent(playerRef, TransformComponent.getComponentType());
            if (transform == null) {
                continue;
            }

            double distSq = transform.getPosition().distanceSquaredTo(anchorPos);
            if (distSq > maxRadiusSq) {
                hexcaster.requestStateChange(HexState.IDLE);
                activePlayers.remove(i);
                LOGGER.atInfo().log("pedestal: player %s out of range, exiting CRAFTING", playerRef);
            }
        }
    }

    private void tickCrafting(PedestalBlockComponent pedestal, Store<EntityStore> store,
            CommandBuffer<EntityStore> buffer) {

        Ref<EntityStore> activeHexRef = pedestal.getActiveHexEntityRef();
        if (activeHexRef == null || !activeHexRef.isValid()) {
            return;
        }
    }
}
