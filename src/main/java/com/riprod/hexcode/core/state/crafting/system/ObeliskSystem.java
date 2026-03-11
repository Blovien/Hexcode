package com.riprod.hexcode.core.state.crafting.system;

import java.util.List;
import java.util.Set;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.block.component.UnbreakableBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalDataComponent;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;
import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.state.crafting.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.state.crafting.utils.PedestalDataUtil;

public class ObeliskSystem {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static void enterCrafting(CommandBuffer<EntityStore> buffer,
            PedestalBlockComponent pedestalComponent, Ref<EntityStore> selectedHexRef) {

        if (pedestalComponent.isPerPlayer()) {
            if (anyPlayerInState(buffer, pedestalComponent, PedestalState.CRAFTING)) {
                return;
            }
        }

        updateState(buffer, pedestalComponent, buffer.getExternalData().getWorld(), PedestalState.CRAFTING);
    }

    public static void enterSelecting(PedestalBlockComponent pedestalComponent,
            World world, CommandBuffer<EntityStore> buffer) {

        if (pedestalComponent.isPerPlayer()) {
            Set<Ref<EntityStore>> players = pedestalComponent.getActivePlayerRefs();
            if (players != null && !players.isEmpty()) {
                if (anyPlayerInState(buffer, pedestalComponent, PedestalState.SELECTING)
                        || anyPlayerInState(buffer, pedestalComponent, PedestalState.CRAFTING)) {
                    LOGGER.atInfo().log("obelisk: enterSelecting skipped — player already in SELECTING/CRAFTING");
                    return;
                }
            }
        }

        List<Vector3i> obeliskPositions = pedestalComponent.getActiveObelisks();
        LOGGER.atInfo().log("obelisk: enterSelecting — protecting %d obelisks", obeliskPositions.size());
        UnbreakableBlockComponent.protectBlocks(world, obeliskPositions);

        updateState(buffer, pedestalComponent, world, PedestalState.SELECTING);
    }

    public static void enterIdle(CommandBuffer<EntityStore> buffer,
            PedestalBlockComponent pedestalComponent,
            World world) {

        if (pedestalComponent.isPerPlayer()) {
            Set<Ref<EntityStore>> players = pedestalComponent.getActivePlayerRefs();
            if (players != null && !players.isEmpty()) {
                if (anyPlayerInState(buffer, pedestalComponent, PedestalState.CRAFTING)
                        || anyPlayerInState(buffer, pedestalComponent, PedestalState.SELECTING)
                        || anyPlayerInState(buffer, pedestalComponent, PedestalState.READY)) {
                    return;
                }
            }
        }

        List<Vector3i> obeliskPositions = pedestalComponent.getActiveObelisks();
        UnbreakableBlockComponent.unprotectBlocks(world, obeliskPositions);

        pedestalComponent.getActiveObelisks().clear();

        updateState(buffer, pedestalComponent, world, PedestalState.IDLE);
    }

    public static void handleReady(CommandBuffer<EntityStore> accessor, PedestalBlockComponent pedestal, World world) {
        if (pedestal.isPerPlayer())
            return;

        updateState(accessor, pedestal, world, PedestalState.READY);
    }

    public static void updateState(CommandBuffer<EntityStore> accessor, PedestalBlockComponent pedestal, World world,
            PedestalState state) {

        String blockState = "Idle";
        switch (state) {
            case IDLE:
                blockState = "Idle";
                break;
            case READY:
                blockState = "Ready";
                break;
            case SELECTING:
                blockState = "Selecting";
                break;
            case CRAFTING:
                blockState = "Crafting";
                break;
        }

        List<Vector3i> obeliskPositions = pedestal.getActiveObelisks();
        LOGGER.atInfo().log("obelisk: updateState to %s — %d obelisks in list", blockState, obeliskPositions.size());

        for (Vector3i obeliskPos : obeliskPositions) {
            if (obeliskPos == null)
                continue;
            LOGGER.atInfo().log("obelisk: changing block state at %s to %s", obeliskPos, blockState);
            PedestalBlockUtil.changeBlockState(world, obeliskPos, blockState);
        }
    }

    public static void CleanupObelisks(CommandBuffer<EntityStore> accessor, World world, List<Vector3i> obelisks) {
        if (obelisks.isEmpty())
            return;
        UnbreakableBlockComponent.unprotectBlocks(world, obelisks);

        for (Vector3i obeliskPos : obelisks) {
            if (obeliskPos == null)
                continue;
            PedestalBlockUtil.changeBlockState(world, obeliskPos, "Idle");
        }
    }

    private static boolean anyPlayerInState(CommandBuffer<EntityStore> buffer,
            PedestalBlockComponent pedestal, PedestalState targetState) {
        Set<Ref<EntityStore>> players = pedestal.getActivePlayerRefs();
        if (players == null) return false;
        for (Ref<EntityStore> playerRef : players) {
            if (playerRef == null || !playerRef.isValid()) continue;
            PedestalDataComponent data = PedestalDataUtil.getPedestalData(buffer, playerRef);
            if (data != null && data.getState() == targetState) return true;
        }
        return false;
    }
}
