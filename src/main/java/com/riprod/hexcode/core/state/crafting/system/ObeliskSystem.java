package com.riprod.hexcode.core.state.crafting.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.block.component.UnbreakableBlockComponent;
import com.riprod.hexcode.core.common.block.event.BlockBreakEvent;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.entity.AnchorEntity;
import com.riprod.hexcode.core.state.crafting.entity.PedestalEntity;
import com.riprod.hexcode.core.state.crafting.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.state.crafting.utils.PedestalItemUtil;
import com.riprod.hexcode.core.state.crafting.utils.PedestalState;
import com.riprod.hexcode.core.state.crafting.utils.RadialPositionUtil;
import com.riprod.hexcode.state.HexState;

/** Handles obelisk-related logic and interactions */
public class ObeliskSystem {
    public static void ActivateHexSelection(CommandBuffer<EntityStore> buffer,
            PedestalBlockComponent pedestal, Ref<EntityStore> selectedHexRef) {

        // Activates hexes

        updateState(buffer, pedestal, buffer.getExternalData().getWorld(), PedestalState.CRAFTING);
    }

    public static void handleActivation(PedestalBlockComponent pedestalComponent,
            World world, CommandBuffer<EntityStore> buffer) {

        List<Vector3i> obeliskPositions = pedestalComponent.getActiveObelisks();

        UnbreakableBlockComponent.protectBlocks(world, obeliskPositions);

        // Handle state change
        updateState(buffer, pedestalComponent, world, PedestalState.SELECTING);
    }

    public static void handleDeactivation(CommandBuffer<EntityStore> buffer,
            PedestalBlockComponent pedestalComponent,
            World world) {

        List<Vector3i> obeliskPositions = pedestalComponent.getActiveObelisks();

        UnbreakableBlockComponent.unprotectBlocks(world, obeliskPositions);

        pedestalComponent.addObelisk(null);

        // handle state change
        updateState(buffer, pedestalComponent, world, PedestalState.IDLE);
    }

    public static void handleReady(CommandBuffer<EntityStore> accessor, PedestalBlockComponent pedestal, World world) {
        if (pedestal.getStoredBook() == null || pedestal.getEssenceItemId() == null
                || pedestal.getStoredBook().isEmpty()) {
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
            if (obeliskPos == null) continue;
            PedestalBlockUtil.changeBlockState(world, obeliskPos, blockState);
        }
    }

    public static void CleanupObelisks(CommandBuffer<EntityStore> accessor, World world, List<Vector3i> obelisks) {
        // Remove them
        if (obelisks.isEmpty()) return;
        UnbreakableBlockComponent.unprotectBlocks(world, obelisks);

        for (Vector3i obeliskPos : obelisks) {
            // update the state of every obelisk
            if (obeliskPos == null) continue;
            PedestalBlockUtil.changeBlockState(world, obeliskPos, "Idle");
        }
    }
}
