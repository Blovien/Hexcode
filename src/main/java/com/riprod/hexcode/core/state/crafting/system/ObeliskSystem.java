package com.riprod.hexcode.core.state.crafting.system;

import java.util.List;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.block.component.UnbreakableBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;
import com.riprod.hexcode.core.state.crafting.utils.PedestalBlockUtil;

/** Handles obelisk-related logic and interactions */
public class ObeliskSystem {
    public static void enterCrafting(CommandBuffer<EntityStore> buffer,
            PedestalBlockComponent pedestalComponent, Ref<EntityStore> selectedHexRef) {

        // Activates hexes
        List<PedestalState> pedestalStates = pedestalComponent.getStates();
        if (pedestalComponent.isPerPlayer()) {
            boolean anyInCrafting = pedestalStates.stream()
                    .anyMatch(state -> state == PedestalState.CRAFTING);
            if (anyInCrafting) {
                return; // return if there are any players who are in crafting on this pedestal
            }
        }

        updateState(buffer, pedestalComponent, buffer.getExternalData().getWorld(), PedestalState.CRAFTING);
    }

    public static void enterSelecting(PedestalBlockComponent pedestalComponent,
            World world, CommandBuffer<EntityStore> buffer) {

        List<PedestalState> pedestalStates = pedestalComponent.getStates();
        if (pedestalComponent.isPerPlayer() && !pedestalStates.isEmpty()) {
            boolean anyInSelectingOrCrafting = pedestalStates.stream()
                    .anyMatch(state -> state == PedestalState.SELECTING || state == PedestalState.CRAFTING);
            if (anyInSelectingOrCrafting) {
                return; // return if there are any players who are crafting / selecting on this pedestal
            }
        }

        List<Vector3i> obeliskPositions = pedestalComponent.getActiveObelisks();

        UnbreakableBlockComponent.protectBlocks(world, obeliskPositions);

        // Handle state change
        updateState(buffer, pedestalComponent, world, PedestalState.SELECTING);
    }

    public static void enterIdle(CommandBuffer<EntityStore> buffer,
            PedestalBlockComponent pedestalComponent,
            World world) {

        List<PedestalState> pedestalStates = pedestalComponent.getStates();
        if (pedestalComponent.isPerPlayer() && !pedestalStates.isEmpty()) {
            boolean anyNotIdle = pedestalStates.stream().anyMatch(state -> state != PedestalState.IDLE);
            if (anyNotIdle) {
                return; // return if there are still crafting states active for other players
            }
        }

        List<Vector3i> obeliskPositions = pedestalComponent.getActiveObelisks();

        UnbreakableBlockComponent.unprotectBlocks(world, obeliskPositions);

        pedestalComponent.addObelisk(null);

        // handle state change
        updateState(buffer, pedestalComponent, world, PedestalState.IDLE);
    }

    public static void handleReady(CommandBuffer<EntityStore> accessor, PedestalBlockComponent pedestal, World world) {
        if (pedestal.isPerPlayer())
            return;

        if (pedestal.getStoredBook(null) == null || pedestal.getEssenceItemId(null) == null
                || pedestal.getStoredBook(null).isEmpty()) {
            // updateState(accessor, pedestal, world, PedestalState.IDLE);
            return;
        }

        // Update the state
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

        for (Vector3i obeliskPos : obeliskPositions) {
            // update the state of every obelisk
            if (obeliskPos == null)
                continue;
            PedestalBlockUtil.changeBlockState(world, obeliskPos, blockState);
        }
    }

    public static void CleanupObelisks(CommandBuffer<EntityStore> accessor, World world, List<Vector3i> obelisks) {
        // Remove them
        if (obelisks.isEmpty())
            return;
        UnbreakableBlockComponent.unprotectBlocks(world, obelisks);

        for (Vector3i obeliskPos : obelisks) {
            // update the state of every obelisk
            if (obeliskPos == null)
                continue;
            PedestalBlockUtil.changeBlockState(world, obeliskPos, "Idle");
        }
    }
}
