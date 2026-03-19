package com.riprod.hexcode.core.common.obelisk.system;

import java.util.List;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.block.component.UnbreakableBlockComponent;
import com.riprod.hexcode.core.common.pedestal.component.PedestalBlockComponent;
import com.riprod.hexcode.core.common.pedestal.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;

public class ObeliskSystem {

    public static void updateState(CommandBuffer<EntityStore> buffer, PedestalBlockComponent pedestal,
            World world, PedestalState previousState, PedestalState newState) {

        List<Vector3i> obeliskPositions = pedestal.getActiveObelisks();

        if (newState == PedestalState.SELECTING && previousState != PedestalState.SELECTING) {
            UnbreakableBlockComponent.protectBlocks(world, obeliskPositions);
        }

        if (newState == PedestalState.IDLE) {
            UnbreakableBlockComponent.unprotectBlocks(world, obeliskPositions);
        }

        String blockState = switch (newState) {
            case IDLE -> "Idle";
            case READY -> "Ready";
            case SELECTING -> "Selecting";
            case CRAFTING -> "Crafting";
        };

        for (Vector3i obeliskPos : obeliskPositions) {
            if (obeliskPos == null) continue;
            PedestalBlockUtil.changeBlockState(world, obeliskPos, blockState);
        }

        ObeliskDispatcher.dispatchStateChange(buffer, pedestal, previousState, newState);

        if (newState == PedestalState.IDLE) {
            pedestal.getActiveObelisks().clear();
        }
    }

    public static void cleanupObelisks(CommandBuffer<EntityStore> buffer, World world, List<Vector3i> obelisks) {
        if (obelisks.isEmpty()) return;
        UnbreakableBlockComponent.unprotectBlocks(world, obelisks);

        for (Vector3i obeliskPos : obelisks) {
            if (obeliskPos == null) continue;
            PedestalBlockUtil.changeBlockState(world, obeliskPos, "Idle");
        }
    }
}
