package com.riprod.hexcode.core.common.pedestal.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexcaster.utils.PlayerUtils;
import com.riprod.hexcode.core.common.pedestal.component.PedestalBlockComponent;
import com.riprod.hexcode.core.common.pedestal.events.PedestalSystem;
import com.riprod.hexcode.core.state.crafting.component.CraftingDataComponent;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;
import com.riprod.hexcode.core.state.crafting.entity.PedestalEntity;
import com.riprod.hexcode.core.state.crafting.utils.CraftingDataUtil;
import com.riprod.hexcode.core.state.crafting.utils.PedestalItemUtil;
import com.riprod.hexcode.utils.HexSlot;

import io.sentry.util.Pair;

public class PedestalInteractionEvent {
    private static HytaleLogger logger = HytaleLogger.forEnclosingClass();

    public static void HandleInteraction(CommandBuffer<EntityStore> accessor, Ref<EntityStore> playerRef,
            BlockPosition targetBlock) {

        Vector3i blockPos = new Vector3i(targetBlock.x, targetBlock.y, targetBlock.z);
        World world = accessor.getExternalData().getWorld();

        Player player = accessor.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            return;
        }

        PedestalBlockComponent pedestalComponent = BlockModule.getComponent(
                PedestalBlockComponent.getComponentType(), world,
                blockPos.x, blockPos.y, blockPos.z);

        if (pedestalComponent == null) {
            logger.atInfo().log("pedestal interaction failed: no PedestalBlockComponent at %s", blockPos);
            return;
        }

        if (pedestalComponent.getLocation() == null || !pedestalComponent.getLocation().equals(blockPos)) {
            pedestalComponent.setLocation(blockPos);
        }

        CraftingDataComponent playerData = CraftingDataUtil.getPedestalData(accessor, playerRef,
                pedestalComponent.isPerPlayer());
        if (playerData == null)
            return;

        playerData.updatePedestal(blockPos, pedestalComponent.getMaxRadius(), pedestalComponent.isPerPlayer());
        ensureAnchor(accessor, pedestalComponent, playerData, blockPos);

        Pair<ItemStack, HexSlot> held = PlayerUtils.getItemFromInventory(player, HexSlot.Both);
        ItemStack item = held.getFirst();
        HexSlot slot = held.getSecond();

        if (PedestalItemUtil.isEssence(item) && playerData.getEssence() == null) {
            PedestalSystem.handleEssencePlacement(accessor, player, item, slot, pedestalComponent, playerData,
                    blockPos);
            PedestalSystem.handleReady(accessor, playerData, pedestalComponent, world);
            return;
        }

        if (PedestalItemUtil.isHexBook(item) && playerData.getStoredBook().isEmpty()) {
            PedestalSystem.handleBookPlacement(accessor, player, item, slot, pedestalComponent, playerData, blockPos);
            PedestalSystem.handleReady(accessor, playerData, pedestalComponent, world);
            return;
        }

        // holding a relevant item the pedestal can't accept
        if (!PedestalItemUtil.isEmptyHand(item)) {
            return;
        }

        boolean hasEssence = playerData.getEssence() != null;
        boolean hasBook = playerData.getStoredBook() != null && !playerData.getStoredBook().isEmpty();

        // partial state: return the lone item
        if (hasBook && !hasEssence) {
            PedestalSystem.enterIdle(accessor, player, pedestalComponent, world);
            return;
        }

        if (hasEssence && !hasBook) {
            PedestalSystem.enterIdle(accessor, player, pedestalComponent, world);
            return;
        }

        // both items present: tiered step-back
        if (hasBook && hasEssence) {
            PedestalState state = playerData.getState();
            logger.atInfo().log("pedestal: hasBook && hasEssence, state=%s", state);
            if (state == PedestalState.CRAFTING) {
                PedestalSystem.exitCrafting(accessor, playerRef, pedestalComponent, playerData);
                PedestalSystem.enterSelecting(pedestalComponent, player, world, accessor);
            } else if (state == PedestalState.SELECTING) {
                PedestalSystem.enterIdle(accessor, player, pedestalComponent, world);
            } else {
                logger.atInfo().log("pedestal: entering selecting + obelisk flow");
                PedestalSystem.enterSelecting(pedestalComponent, player, world, accessor);
            }
        }
    }

    private static void ensureAnchor(CommandBuffer<EntityStore> buffer,
            PedestalBlockComponent pedestal, CraftingDataComponent playerData, Vector3i blockPos) {
        Ref<EntityStore> anchorRef = playerData.getAnchorRef();
        if (anchorRef != null && anchorRef.isValid()) {
            return;
        }

        anchorRef = PedestalEntity.spawnAnchorEntity(buffer, blockPos);
        if (anchorRef == null) {
            logger.atWarning().log("pedestal: ensureAnchor — spawnAnchorEntity returned null at %s", blockPos);
        }
        playerData.setAnchorEntityRef(anchorRef);
    }
}
