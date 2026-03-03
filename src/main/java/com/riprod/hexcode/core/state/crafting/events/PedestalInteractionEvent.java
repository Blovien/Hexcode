package com.riprod.hexcode.core.state.crafting.events;

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
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.entity.PedestalEntity;
import com.riprod.hexcode.core.state.crafting.system.ObeliskSystem;
import com.riprod.hexcode.core.state.crafting.system.PedestalSystem;
import com.riprod.hexcode.core.state.crafting.utils.PedestalItemUtil;
import com.riprod.hexcode.core.state.crafting.utils.PedestalState;

public class PedestalInteractionEvent {
    private static HytaleLogger logger = HytaleLogger.forEnclosingClass();

    public static void HandleInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> playerRef,
            BlockPosition targetBlock) {

        Vector3i blockPos = new Vector3i(targetBlock.x, targetBlock.y, targetBlock.z);
        World world = buffer.getExternalData().getWorld();

        Player player = buffer.getComponent(playerRef, Player.getComponentType());
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

        ensureAnchor(buffer, pedestalComponent, blockPos);
        rebuildDisplays(buffer, pedestalComponent, blockPos);

        ItemStack itemInHand = player.getInventory().getItemInHand();

        if (PedestalItemUtil.isEssence(itemInHand) && pedestalComponent.getEssenceItemId() == null) {
            PedestalSystem.handleEssencePlacement(buffer, player, itemInHand, pedestalComponent, blockPos);
            PedestalSystem.handleReady(buffer, pedestalComponent, world);
            ObeliskSystem.handleReady(buffer, pedestalComponent, world);
            return;
        }

        if (PedestalItemUtil.isHexBook(itemInHand) && pedestalComponent.getStoredBook().isEmpty()) {
            PedestalSystem.handleBookPlacement(buffer, player, itemInHand, pedestalComponent, blockPos);
            PedestalSystem.handleReady(buffer, pedestalComponent, world);
            ObeliskSystem.handleReady(buffer, pedestalComponent, world);
            return;
        }

        if (PedestalItemUtil.isEmptyHand(itemInHand)) {
            PedestalState state = pedestalComponent.getState();
            boolean shouldDeactivate = state == PedestalState.SELECTING
                    || state == PedestalState.CRAFTING
                    || pedestalComponent.getEssenceItemId() == null
                    || pedestalComponent.getStoredBook() == null
                    || pedestalComponent.getStoredBook().isEmpty();

            if (shouldDeactivate) {
                PedestalSystem.handleDeactivation(buffer, player, pedestalComponent, world);
                ObeliskSystem.handleDeactivation(buffer, pedestalComponent, world);
            } else {
                PedestalSystem.handleActivation(pedestalComponent, world, buffer);
                ObeliskSystem.handleActivation(pedestalComponent, world, buffer);
            }

            return;
        }
    }

    private static void ensureAnchor(CommandBuffer<EntityStore> buffer,
            PedestalBlockComponent pedestal, Vector3i blockPos) {
        Ref<EntityStore> anchorRef = pedestal.getAnchorRef();
        if (anchorRef != null && anchorRef.isValid()) {
            return;
        }

        logger.atInfo().log("pedestal: rebuilding anchor at %s", blockPos);
        anchorRef = PedestalEntity.spawnAnchorEntity(buffer, blockPos);
        pedestal.setAnchorEntityRef(anchorRef);
    }

    private static void rebuildDisplays(CommandBuffer<EntityStore> buffer,
            PedestalBlockComponent pedestal, Vector3i blockPos) {

        Vector3d anchorPos = PedestalEntity.getAnchorPosition(blockPos);

        ItemStack storedBook = pedestal.getStoredBook();
        Ref<EntityStore> bookRef = pedestal.getBookDisplayRef();
        if (storedBook != null && !storedBook.isEmpty() && (bookRef == null || !bookRef.isValid())) {
            Item bookItem = storedBook.getItem();
            if (bookItem != null) {
                logger.atInfo().log("pedestal: rebuilding book display at %s", blockPos);
                Ref<EntityStore> newBookRef = PedestalEntity.spawnBookDisplay(
                        buffer, pedestal, anchorPos, bookItem);
                pedestal.setBookDisplayRef(newBookRef);
            }
        }

        String essenceId = pedestal.getEssenceItemId();
        Ref<EntityStore> essenceRef = pedestal.getEssenceDisplayRef();
        if (essenceId != null && (essenceRef == null || !essenceRef.isValid())) {
            Item essenceItem = Item.getAssetMap().getAsset(essenceId);
            if (essenceItem != null) {
                logger.atInfo().log("pedestal: rebuilding essence display at %s", blockPos);
                Ref<EntityStore> newEssenceRef = PedestalEntity.spawnEssenceDisplay(
                        buffer, pedestal, anchorPos, essenceItem, pedestal.getReferenceHolder());
                pedestal.setEssenceDisplayRef(newEssenceRef);
            }
        }

        PedestalState state = pedestal.getState();
        if (state != PedestalState.IDLE && state != null) {
            World world = buffer.getExternalData().getWorld();
            PedestalSystem.updateState(buffer, pedestal, world, state);
            ObeliskSystem.updateState(buffer, pedestal, world, state);
        }
    }
}
