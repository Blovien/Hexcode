package com.riprod.hexcode.core.state.crafting.events;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexcaster.utils.PlayerUtils;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalDataComponent;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;
import com.riprod.hexcode.core.state.crafting.entity.PedestalEntity;
import com.riprod.hexcode.core.state.crafting.system.ObeliskSystem;
import com.riprod.hexcode.core.state.crafting.system.PedestalSystem;
import com.riprod.hexcode.core.state.crafting.utils.PedestalDataUtil;
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

        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(accessor, playerRef,
                pedestalComponent.isPerPlayer());
        if (playerData == null) return;

        playerData.updatePedestal(blockPos, pedestalComponent.getMaxRadius(), pedestalComponent.isPerPlayer());
        ensureAnchor(accessor, pedestalComponent, playerData, blockPos);
        rebuildDisplays(accessor, playerData, pedestalComponent, blockPos);

        Pair<ItemStack, HexSlot> held = PlayerUtils.getItemFromInventory(player, HexSlot.Both);
        ItemStack item = held.getFirst();
        HexSlot slot = held.getSecond();

        if (PedestalItemUtil.isEssence(item) && playerData.getEssence() == null) {
            PedestalSystem.handleEssencePlacement(accessor, player, item, slot, pedestalComponent, playerData, blockPos);
            PedestalSystem.handleReady(accessor, playerData, pedestalComponent, world);
            ObeliskSystem.handleReady(accessor, pedestalComponent, world);
            return;
        }

        if (PedestalItemUtil.isHexBook(item) && playerData.getStoredBook().isEmpty()) {
            PedestalSystem.handleBookPlacement(accessor, player, item, slot, pedestalComponent, playerData, blockPos);
            PedestalSystem.handleReady(accessor, playerData, pedestalComponent, world);
            ObeliskSystem.handleReady(accessor, pedestalComponent, world);
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
            playerData.setState(PedestalState.IDLE);
            PedestalSystem.enterIdle(accessor, player, pedestalComponent, world);
            ObeliskSystem.enterIdle(accessor, pedestalComponent, world);
            return;
        }

        if (hasEssence && !hasBook) {
            playerData.setState(PedestalState.IDLE);
            PedestalSystem.enterIdle(accessor, player, pedestalComponent, world);
            ObeliskSystem.enterIdle(accessor, pedestalComponent, world);
            return;
        }

        // both items present: toggle activation
        if (hasBook && hasEssence) {
            PedestalState state = playerData.getState();
            if (state == PedestalState.SELECTING || state == PedestalState.CRAFTING) {
                playerData.setState(PedestalState.IDLE);
                PedestalSystem.enterIdle(accessor, player, pedestalComponent, world);
                ObeliskSystem.enterIdle(accessor, pedestalComponent, world);
            } else {
                PedestalSystem.enterSelecting(pedestalComponent, player, world, accessor);
                ObeliskSystem.enterSelecting(pedestalComponent, world, accessor);
                playerData.setState(PedestalState.SELECTING);
            }
        }
    }

    private static void ensureAnchor(CommandBuffer<EntityStore> buffer,
            PedestalBlockComponent pedestal, PedestalDataComponent playerData, Vector3i blockPos) {
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

    private static void rebuildDisplays(CommandBuffer<EntityStore> buffer, PedestalDataComponent playerData,
            PedestalBlockComponent pedestal, Vector3i blockPos) {

        PedestalState state = playerData.getState();
        if (state != PedestalState.IDLE && state != null && pedestal.getActivePlayerRefs().isEmpty()) {
            logger.atWarning().log("pedestal: stale state %s with no active players at %s, resetting to IDLE",
                    state, blockPos);
            playerData.setState(PedestalState.IDLE);
            return;
        }

        Vector3d anchorPos = PedestalEntity.getAnchorPosition(blockPos);

        ItemStack storedBook = playerData.getStoredBook();
        Ref<EntityStore> bookRef = playerData.getBookDisplayRef();
        if (storedBook != null && !storedBook.isEmpty() && (bookRef == null || !bookRef.isValid())) {
            Item bookItem = storedBook.getItem();
            if (bookItem != null) {
                Ref<EntityStore> newBookRef = PedestalEntity.spawnBookDisplay(
                        buffer, pedestal, playerData, anchorPos, bookItem, playerData.getAnchorNodeRef());
                playerData.setBookDisplayRef(newBookRef);
            }
        }

        String essenceId = playerData.getEssence();
        Ref<EntityStore> essenceRef = playerData.getEssenceDisplayRef();
        if (essenceId != null && (essenceRef == null || !essenceRef.isValid())) {
            Item essenceItem = Item.getAssetMap().getAsset(essenceId);
            if (essenceItem != null) {
                Ref<EntityStore> newEssenceRef = PedestalEntity.spawnEssenceDisplay(
                        buffer, pedestal, playerData, anchorPos, essenceItem, pedestal.getReferenceHolder(), playerData.getAnchorNodeRef());
                playerData.setEssenceDisplayRef(newEssenceRef);
            }
        }

        if (state != PedestalState.IDLE && state != null) {
            World world = buffer.getExternalData().getWorld();
            PedestalSystem.updateState(buffer, pedestal, playerData, world, state);
            ObeliskSystem.updateState(buffer, pedestal, world, state);
        }
    }
}
