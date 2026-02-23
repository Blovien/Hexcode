package com.riprod.hexcode.core.crafting.system;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.crafting.component.ObeliskBlockState;
import com.riprod.hexcode.core.crafting.component.PedestalBlockState;
import com.riprod.hexcode.core.crafting.component.PedestalComponent;
import com.riprod.hexcode.core.crafting.component.PedestalState;
import com.riprod.hexcode.core.crafting.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.crafting.utils.PedestalSpawner;
import com.riprod.hexcode.core.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.state.HexState;

public class PedestalTickSystem extends EntityTickingSystem<EntityStore> {

    private static final Map<Vector3i, Ref<EntityStore>> activePedestals = new HashMap<>();
    private static final Set<PlayerRef> connectedPlayers = new HashSet<>();

    public static void addPlayer(PlayerRef playerRef) {
        connectedPlayers.add(playerRef);
    }

    public static void removePlayer(PlayerRef playerRef) {
        connectedPlayers.remove(playerRef);
    }

    public static void registerPedestal(Vector3i pos, Ref<EntityStore> ref) {
        activePedestals.put(pos, ref);
    }

    public static void unregisterPedestal(Vector3i pos) {
        activePedestals.remove(pos);
    }

    public static Ref<EntityStore> getAnchorAt(Vector3i pos) {
        return activePedestals.get(pos);
    }

    @Override
    public Query<EntityStore> getQuery() {
        return PedestalComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {

        PedestalComponent pedestal = chunk.getComponent(index, PedestalComponent.getComponentType());
        Ref<EntityStore> anchorRef = chunk.getReferenceTo(index);

        if (pedestal == null) {
            return;
        }

        PedestalState state = pedestal.getPedestalState();
        World world = buffer.getExternalData().getWorld();

        if (state == PedestalState.ACTIVATING) {
            tickActivating(dt, pedestal, anchorRef, world, buffer);
            return;
        }

        if (state == PedestalState.DEACTIVATING) {
            tickDeactivating(dt, pedestal, anchorRef, world, buffer);
            return;
        }

        if (state == PedestalState.ON) {
            tickPlayerDetection(pedestal, anchorRef, store, buffer);
        }
    }

    private void tickActivating(float dt, PedestalComponent pedestal,
            Ref<EntityStore> anchorRef, World world, CommandBuffer<EntityStore> buffer) {

        float timer = pedestal.getTransitionTimer() - dt;
        pedestal.setTransitionTimer(timer);

        if (timer <= 0) {
            pedestal.setPedestalState(PedestalState.ON);
            PedestalBlockUtil.changeBlockState(world, pedestal.getBlockPosition(), "On");
            registerPedestal(pedestal.getBlockPosition(), anchorRef);
            activateObelisks(pedestal, world);
        }
    }

    private void tickDeactivating(float dt, PedestalComponent pedestal,
            Ref<EntityStore> anchorRef, World world, CommandBuffer<EntityStore> buffer) {

        float timer = pedestal.getTransitionTimer() - dt;
        pedestal.setTransitionTimer(timer);

        if (timer <= 0) {
            deactivateObelisks(pedestal, world);
            ejectAllPlayers(pedestal, buffer);
            PedestalBlockUtil.changeBlockState(world, pedestal.getBlockPosition(), "default");
            unregisterPedestal(pedestal.getBlockPosition());
            PedestalSpawner.despawnAnchor(buffer, anchorRef, pedestal, buffer);
        }
    }

    private void activateObelisks(PedestalComponent pedestal, World world) {
        Vector3i center = pedestal.getBlockPosition();
        int radius = (int) pedestal.getDetectionRadius();
        List<Vector3i> obeliskPositions = pedestal.getObeliskPositions();
        obeliskPositions.clear();

        int maxObelisks = getMaxObelisks(world, center);

        for (int x = center.x - radius; x <= center.x + radius; x++) {
            for (int y = center.y - 2; y <= center.y + 4; y++) {
                for (int z = center.z - radius; z <= center.z + radius; z++) {
                    if (obeliskPositions.size() >= maxObelisks) {
                        break;
                    }
                    BlockType type = world.getBlockType(x, y, z);
                    if (PedestalBlockUtil.isObelisk(type)) {
                        obeliskPositions.add(new Vector3i(x, y, z));
                    }
                }
                if (obeliskPositions.size() >= maxObelisks) {
                    break;
                }
            }
            if (obeliskPositions.size() >= maxObelisks) {
                break;
            }
        }

        int totalPower = 0;
        for (Vector3i pos : obeliskPositions) {
            ObeliskProtectionSystem.protect(pos);
            PedestalBlockUtil.changeBlockState(world, pos, "Active");

            ObeliskBlockState obelisk = BlockModule.getComponent(
                    ObeliskBlockState.getComponentType(), world, pos.x, pos.y, pos.z);
            if (obelisk != null) {
                totalPower += obelisk.getPower();
            }
        }

        pedestal.setObeliskPowerTotal(totalPower);
    }

    private void deactivateObelisks(PedestalComponent pedestal, World world) {
        for (Vector3i pos : pedestal.getObeliskPositions()) {
            ObeliskProtectionSystem.unprotect(pos);
            PedestalBlockUtil.changeBlockState(world, pos, "default");
        }
        pedestal.getObeliskPositions().clear();
        pedestal.setObeliskPowerTotal(0);
    }

    private int getMaxObelisks(World world, Vector3i pedestalPos) {
        PedestalBlockState blockState = BlockModule.getComponent(
                PedestalBlockState.getComponentType(), world,
                pedestalPos.x, pedestalPos.y, pedestalPos.z);
        if (blockState != null) {
            return blockState.getMaxObelisks();
        }
        return 4;
    }

    private void tickPlayerDetection(PedestalComponent pedestal,
            Ref<EntityStore> anchorRef, Store<EntityStore> store,
            CommandBuffer<EntityStore> buffer) {

        TransformComponent anchorTransform = buffer.getComponent(anchorRef,
                TransformComponent.getComponentType());
        if (anchorTransform == null) {
            return;
        }

        Vector3d anchorPos = anchorTransform.getPosition();
        double radiusSq = pedestal.getDetectionRadius() * pedestal.getDetectionRadius();

        for (PlayerRef player : connectedPlayers) {
            Ref<EntityStore> playerRef = player.getReference();
            if (playerRef == null || !playerRef.isValid()) {
                continue;
            }

            if (pedestal.getPlayersInRange().contains(playerRef)) {
                continue;
            }

            HexcasterComponent hexcaster = buffer.getComponent(playerRef,
                    HexcasterComponent.getComponentType());
            if (hexcaster == null || hexcaster.getState() != HexState.IDLE) {
                continue;
            }

            TransformComponent playerTransform = buffer.getComponent(playerRef,
                    TransformComponent.getComponentType());
            if (playerTransform == null) {
                continue;
            }

            double distSq = new Vector3d(anchorPos).subtract(playerTransform.getPosition()).squaredLength();
            if (distSq <= radiusSq) {
                pedestal.getPlayersInRange().add(playerRef);
                hexcaster.setPedestalRef(anchorRef);
                hexcaster.requestStateChange(HexState.CRAFTING);
            }
        }

        Iterator<Ref<EntityStore>> it = pedestal.getPlayersInRange().iterator();
        while (it.hasNext()) {
            Ref<EntityStore> playerRef = it.next();

            if (!playerRef.isValid()) {
                it.remove();
                continue;
            }

            TransformComponent playerTransform = buffer.getComponent(playerRef,
                    TransformComponent.getComponentType());
            if (playerTransform == null) {
                it.remove();
                continue;
            }

            double distSq = new Vector3d(anchorPos).subtract(playerTransform.getPosition()).squaredLength();
            if (distSq > radiusSq) {
                it.remove();
                HexcasterComponent hexcaster = buffer.getComponent(playerRef,
                        HexcasterComponent.getComponentType());
                if (hexcaster != null && hexcaster.getState() == HexState.CRAFTING) {
                    hexcaster.requestStateChange(HexState.IDLE);
                }
            }
        }
    }

    private void ejectAllPlayers(PedestalComponent pedestal, CommandBuffer<EntityStore> buffer) {
        for (Ref<EntityStore> playerRef : pedestal.getPlayersInRange()) {
            if (!playerRef.isValid()) {
                continue;
            }
            HexcasterComponent hexcaster = buffer.getComponent(playerRef,
                    HexcasterComponent.getComponentType());
            if (hexcaster != null && hexcaster.getState() == HexState.CRAFTING) {
                hexcaster.requestStateChange(HexState.IDLE);
            }
        }
        pedestal.getPlayersInRange().clear();
    }
}
