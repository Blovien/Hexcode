package com.riprod.hexcode.core.common.pedestal.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexcaster.utils.PlayerUtils;
import com.riprod.hexcode.core.common.pedestal.component.PedestalBlockComponent;
import com.riprod.hexcode.core.common.pedestal.events.PedestalSystem;
import com.riprod.hexcode.core.state.crafting.component.CraftingData;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;
import com.riprod.hexcode.core.state.crafting.utils.CraftingDataUtil;
import com.riprod.hexcode.core.state.crafting.utils.PedestalItemUtil;
import com.riprod.hexcode.state.HexState;
import com.riprod.hexcode.utils.HexSlot;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import io.sentry.util.Pair;

public class PedestalInteractionEvent {
    private static HytaleLogger logger = HytaleLogger.forEnclosingClass();

    public static void HandleInteraction(CommandBuffer<EntityStore> accessor, Ref<EntityStore> playerRef,
            BlockPosition targetBlock) {
        try {
            handleInteractionInternal(accessor, playerRef, targetBlock);
        } catch (Exception e) {
            logger.atSevere().withCause(e).log("pedestal interaction failed at %s", targetBlock);
        }
    }

    private static void handleInteractionInternal(CommandBuffer<EntityStore> accessor, Ref<EntityStore> playerRef,
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

        CraftingData playerData = pedestalComponent.getCraftingDataComponent();
        if (playerData == null) {
            logger.atWarning().log("pedestal interaction: no CraftingData at %s", blockPos);
            return;
        }

        if (playerData.getOwnerRef() == null || !playerData.getOwnerRef().isValid()) {
            playerData.setOwnerRef(playerRef);
        }

        // access control
        Ref<EntityStore> ownerRef = playerData.getOwnerRef();
        boolean hasOwner = ownerRef != null && ownerRef.isValid();
        boolean isOwner = hasOwner && ownerRef.equals(playerRef);

        if (hasOwner && !isOwner) {
            if (pedestalComponent.isPerPlayer()) {
                PlayerRef pr = accessor.getComponent(playerRef, PlayerRef.getComponentType());
                if (pr != null) {
                    pr.sendMessage(Message.raw("This pedestal is already in use!"));
                }
                return;
            }

            // collaborative mode: non-owner joins as collaborator
            PedestalState state = playerData.getState();
            if (state == PedestalState.CRAFTING || state == PedestalState.SELECTING) {
                joinAsCollaborator(accessor, playerRef, pedestalComponent, playerData);
                return;
            }

            PlayerRef pr = accessor.getComponent(playerRef, PlayerRef.getComponentType());
            if (pr != null) {
                pr.sendMessage(Message.raw("Waiting for the owner to finish setting up the pedestal."));
            }
            return;
        }

        if (!hasOwner) {
            playerData.setOwnerRef(playerRef);
        }

        Pair<ItemStack, ItemStack> held = PlayerUtils.getItemFromHands(accessor, playerRef);
        ItemStack mainHand = held.getFirst();
        ItemStack utilityHand = held.getSecond();

        if (PedestalItemUtil.anyEssence(mainHand, utilityHand) && playerData.getEssence() == null) {
            Pair<ItemStack, HexSlot> essence = PedestalItemUtil.getEssence(mainHand, utilityHand);
            PedestalSystem.handleEssencePlacement(accessor, player, essence.getFirst(), essence.getSecond(), pedestalComponent, playerData,
                    blockPos);
            PedestalSystem.handleReady(accessor, playerData, pedestalComponent, world);
            return;
        }

        if (PedestalItemUtil.anyHexBook(mainHand, utilityHand) && playerData.getStoredBook().isEmpty()) {
            Pair<ItemStack, HexSlot> book = PedestalItemUtil.getHexBook(mainHand, utilityHand);
            PedestalSystem.handleBookPlacement(accessor, player, book.getFirst(), book.getSecond(), pedestalComponent, playerData, blockPos);
            PedestalSystem.handleReady(accessor, playerData, pedestalComponent, world);
            return;
        }

        boolean hasEssence = playerData.getEssence() != null;
        boolean hasBook = playerData.getStoredBook() != null && !playerData.getStoredBook().isEmpty();

        if (hasBook && !hasEssence) {
            PedestalSystem.enterIdle(accessor, player, pedestalComponent, world);
            return;
        }

        if (hasEssence && !hasBook) {
            PedestalSystem.enterIdle(accessor, player, pedestalComponent, world);
            return;
        }

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

    private static void joinAsCollaborator(CommandBuffer<EntityStore> accessor, Ref<EntityStore> playerRef,
            PedestalBlockComponent pedestalComponent, CraftingData playerData) {

        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null) {
            craftingComp = new HexcasterCraftingComponent();
            accessor.putComponent(playerRef, HexcasterCraftingComponent.getComponentType(), craftingComp);
        }
        craftingComp.setPedestalLocation(playerData.getPedestalLocation());

        HexcasterComponent hexcaster = accessor.getComponent(playerRef, HexcasterComponent.getComponentType());
        if (hexcaster != null) {
            hexcaster.requestStateChange(HexState.CRAFTING);
        }

        pedestalComponent.addDetectedPlayer(accessor, playerRef);
    }

}
