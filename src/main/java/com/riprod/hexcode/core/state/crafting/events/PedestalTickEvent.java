package com.riprod.hexcode.core.state.crafting.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalAnchorComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.entity.PedestalEntity;
import com.riprod.hexcode.core.state.crafting.utils.PlayerLocationUtil;
import com.riprod.hexcode.state.HexState;
import io.sentry.util.Pair;

public class PedestalTickEvent extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final float PLAYER_DETECTION_INTERVAL_DT = 2.0f;
    public static final String PLAYER_DETECTION_DT_KEY = "CRAFTING";

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

        if (!pedestalBlock.isPerPlayer()) { // tick if not per player and not idle
            Ref<EntityStore> anchorRef = accessor.getReferenceTo(index);
            tickPlayerDetection(buffer, store, dt, pedestalBlock, anchorRef);
        }
    }

    private void tickPlayerDetection(CommandBuffer<EntityStore> accessor, Store<EntityStore> store, float dt,
            PedestalBlockComponent pedestal, Ref<EntityStore> anchorRef) {

        if (pedestal.getTickLength(PLAYER_DETECTION_DT_KEY) < PLAYER_DETECTION_INTERVAL_DT) {
            pedestal.incrementTickLength(PLAYER_DETECTION_DT_KEY, dt);
            return;
        }
        pedestal.setTickLength(PLAYER_DETECTION_DT_KEY, 0f);

        Vector3i blockPos = pedestal.getLocation();
        if (blockPos == null) {
            return;
        }

        Vector3d anchorPos = PedestalEntity.getAnchorPosition(blockPos);
        double maxRadius = pedestal.getMaxRadius();
        double maxRadiusSq = maxRadius * maxRadius;

        List<Pair<Ref<EntityStore>, HexcasterComponent>> nearbyEntities = PlayerLocationUtil.findNearbyPlayers(accessor,
                anchorPos, maxRadius);

        // find nearby players and set them to CRAFTING if they are idle
        for (Pair<Ref<EntityStore>, HexcasterComponent> hexPair : nearbyEntities) {

            HexcasterComponent hexcaster = hexPair.getSecond();

            if (!pedestal.getActivePlayerRefs().contains(hexPair.getFirst())) {
                hexcaster.setPendingPedestalRef(anchorRef);
                hexcaster.requestStateChange(HexState.CRAFTING);
                pedestal.addDetectedPlayer(accessor, hexPair.getFirst());
                LOGGER.atInfo().log("pedestal: auto-entering player %s into CRAFTING, anchor=%s", hexPair.getFirst(),
                        anchorRef);
            }
        }

        Set<Ref<EntityStore>> activePlayers = pedestal.getActivePlayerRefs();
        List<Ref<EntityStore>> toRemove = new ArrayList<>();
        for (Ref<EntityStore> playerRef : activePlayers) {
            if (playerRef == null || !playerRef.isValid()) {
                toRemove.add(playerRef);
                continue;
            }

            HexcasterComponent hexcaster = accessor.getComponent(playerRef, HexcasterComponent.getComponentType());
            if (hexcaster == null
                    || (hexcaster.getState() != HexState.CRAFTING && hexcaster.getState() != HexState.DRAWING)) {
                toRemove.add(playerRef);
                continue;
            }

            TransformComponent transform = accessor.getComponent(playerRef, TransformComponent.getComponentType());
            if (transform == null) {
                continue;
            }

            double distSq = transform.getPosition().distanceSquaredTo(anchorPos);
            if (distSq > maxRadiusSq) {
                hexcaster.requestStateChange(HexState.IDLE);
                toRemove.add(playerRef);
                LOGGER.atInfo().log("pedestal: player %s out of range, exiting CRAFTING", playerRef);
            }
        }
        activePlayers.removeAll(toRemove);
    }
}
