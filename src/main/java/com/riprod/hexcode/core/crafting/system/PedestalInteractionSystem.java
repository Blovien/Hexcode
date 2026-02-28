package com.riprod.hexcode.core.crafting.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.crafting.component.PedestalState;
import com.riprod.hexcode.core.crafting.spawners.AnchorSpawner;
import com.riprod.hexcode.core.crafting.spawners.PedestalSpawner;
import com.riprod.hexcode.core.crafting.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.crafting.utils.PedestalItemUtil;
import com.riprod.hexcode.core.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.state.HexState;

import java.util.List;

public class PedestalInteractionSystem {
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
            logger.atInfo().log("Pedestal interaction failed: no PedestalBlockComponent at %s", blockPos);
            return;
        }

        if (pedestalComponent.getLocation() == null || !pedestalComponent.getLocation().equals(blockPos)) {
            pedestalComponent.setLocation(blockPos);
        }

        Ref<EntityStore> anchorRef = pedestalComponent.getAnchorRef();
        boolean anchorExisted = anchorRef != null && anchorRef.isValid();

        if (!anchorExisted) {
            logger.atInfo().log("pedestal: no anchor at %s, spawning", blockPos);
            anchorRef = PedestalSpawner.spawnAnchorEntity(buffer, blockPos);
            pedestalComponent.setAnchorEntityRef(anchorRef);
        }

        ItemStack itemInHand = player.getInventory().getItemInHand();

        if (PedestalItemUtil.isEssence(itemInHand) && pedestalComponent.getEssenceItemId() == null) {
            handleEssencePlacement(buffer, player, itemInHand, pedestalComponent, anchorRef, blockPos);
            return;
        }

        if (PedestalItemUtil.isHexBook(itemInHand) && pedestalComponent.getStoredBook().isEmpty()) {
            handleBookPlacement(buffer, player, itemInHand, pedestalComponent, anchorRef, blockPos);
            return;
        }

        if (PedestalItemUtil.isEmptyHand(itemInHand)) {
            toggleActivation(buffer, player, pedestalComponent, world);
            return;
        }
    }

    private static void handleEssencePlacement(CommandBuffer<EntityStore> buffer,
            Player player, ItemStack essenceItem, PedestalBlockComponent pedestalComponent,
            Ref<EntityStore> anchorRef, Vector3i blockPos) {

        Vector3d anchorPos = PedestalSpawner.getAnchorPosition(blockPos);

        Ref<EntityStore> oldEssenceRef = pedestalComponent.getEssenceDisplayRef();
        if (oldEssenceRef != null && oldEssenceRef.isValid()) {
            buffer.removeEntity(oldEssenceRef, RemoveReason.REMOVE);
        }

        Ref<EntityStore> newEssenceRef = PedestalSpawner.spawnEssenceDisplay(
                buffer, anchorRef, anchorPos, essenceItem.getItem(), pedestalComponent.getReferenceHolder());
        pedestalComponent.setEssenceDisplayRef(newEssenceRef);
        pedestalComponent.setEssenceItemId(essenceItem.getItem().getId());

        logger.atInfo().log("pedestal: spawned essence display=%s for item=%s, anchor=%s",
                newEssenceRef, essenceItem.getItem().getId(), anchorRef);

        consumeOneFromHand(player);
    }

    private static void handleBookPlacement(CommandBuffer<EntityStore> buffer,
            Player player, ItemStack bookItem, PedestalBlockComponent pedestalComponent,
            Ref<EntityStore> anchorRef, Vector3i blockPos) {

        Vector3d anchorPos = PedestalSpawner.getAnchorPosition(blockPos);

        pedestalComponent.setStoredBook(bookItem);

        Ref<EntityStore> oldBookDisplay = pedestalComponent.getBookDisplayRef();
        if (oldBookDisplay != null && oldBookDisplay.isValid()) {
            buffer.removeEntity(oldBookDisplay, RemoveReason.REMOVE);
        }

        Ref<EntityStore> newBookDisplayRef = PedestalSpawner.spawnBookDisplay(
                buffer, anchorRef, anchorPos, bookItem.getItem(), pedestalComponent.getReferenceHolder());
        pedestalComponent.setBookDisplayRef(newBookDisplayRef);

        logger.atInfo().log("pedestal: spawned book display=%s for item=%s, anchor=%s",
                newBookDisplayRef, bookItem.getItem().getId(), anchorRef);

        consumeOneFromHand(player);
    }

    private static void toggleActivation(CommandBuffer<EntityStore> buffer,
            Player player, PedestalBlockComponent pedestalComponent,
            World world) {

        PedestalState state = pedestalComponent.getState();
        boolean shouldDeactivate = state == PedestalState.ACTIVE
                || state == PedestalState.CRAFTING
                || pedestalComponent.getEssenceItemId() == null
                || pedestalComponent.getStoredBook() == null
                || pedestalComponent.getStoredBook().isEmpty();

        if (shouldDeactivate) {
            handleDeactivation(buffer, player, pedestalComponent, world);
        } else {
            handleActivation(pedestalComponent, world, buffer);
        }
    }

    private static void handleActivation(PedestalBlockComponent pedestalComponent,
            World world, CommandBuffer<EntityStore> buffer) {

        // play book animation
        Ref<EntityStore> bookRef = pedestalComponent.getBookDisplayRef();
        if (bookRef != null && bookRef.isValid()) {
            AnimationUtils.playAnimation(bookRef, AnimationSlot.Action, "Pedestal_Active", buffer);
        }

        // play essence animation
        Ref<EntityStore> essenceRef = pedestalComponent.getEssenceDisplayRef();
        if (essenceRef != null && essenceRef.isValid()) {
            AnimationUtils.playAnimation(essenceRef, AnimationSlot.Action, "Pedestal_Active", buffer);
        }

        logger.atInfo().log("pedestal: activating at %s, essence=%s, book=%s",
                pedestalComponent.getLocation(), pedestalComponent.getEssenceItemId(),
                pedestalComponent.getStoredBook() != null ? pedestalComponent.getStoredBook().getItem().getId() : "null");

        Vector3i blockPos = pedestalComponent.getLocation();
        PedestalBlockUtil.changeBlockState(world, blockPos, "Active");
        pedestalComponent.setState(PedestalState.ACTIVE);

        Integer bookSlots = pedestalComponent.getBookSlots();
        logger.atInfo().log("pedestal: SpawnHexPreviews at %s, bookSlots=%s",
                pedestalComponent.getLocation(), bookSlots);

        PedestalSystem.SpawnHexPreviews(buffer, pedestalComponent);
    }

    private static void handleDeactivation(CommandBuffer<EntityStore> buffer,
            Player player, PedestalBlockComponent pedestalComponent,
            World world) {

        AnchorSpawner.DespawnHexPreviews(buffer, pedestalComponent);

        List<Ref<EntityStore>> activePlayers = pedestalComponent.getActivePlayerRefs();
        for (int i = 0; i < activePlayers.size(); i++) {
            Ref<EntityStore> activePlayerRef = activePlayers.get(i);
            if (activePlayerRef == null || !activePlayerRef.isValid()) {
                continue;
            }
            HexcasterComponent hexcaster = buffer.getComponent(activePlayerRef, HexcasterComponent.getComponentType());
            if (hexcaster != null && hexcaster.getState() == HexState.CRAFTING) {
                hexcaster.requestStateChange(HexState.IDLE);
                logger.atInfo().log("pedestal: kicking player %s from CRAFTING on deactivation", activePlayerRef);
            }
        }
        activePlayers.clear();

        Vector3i blockPos = pedestalComponent.getLocation();

        ObeliskProtectionSystem.unprotect(blockPos);

        ItemStack bookStack = pedestalComponent.getStoredBook();
        if (bookStack != null && !bookStack.isEmpty()) {
            player.getInventory().getHotbar().addItemStack(bookStack);
            pedestalComponent.setStoredBook(ItemStack.EMPTY);
        }

        Ref<EntityStore> bookRef = pedestalComponent.getBookDisplayRef();
        if (bookRef != null && bookRef.isValid()) {
            buffer.removeEntity(bookRef, RemoveReason.REMOVE);
            pedestalComponent.setBookDisplayRef(null);
        }

        Ref<EntityStore> essenceRef = pedestalComponent.getEssenceDisplayRef();
        if (essenceRef != null && essenceRef.isValid()) {
            buffer.removeEntity(essenceRef, RemoveReason.REMOVE);
            pedestalComponent.setEssenceDisplayRef(null);
        }

        pedestalComponent.setState(PedestalState.OFF);
        PedestalBlockUtil.changeBlockState(world, blockPos, "Inactive");
        pedestalComponent.setEssenceItemId(null);
    }

    private static void consumeOneFromHand(Player player) {
        ItemStack current = player.getInventory().getItemInHand();
        if (current == null || current.isEmpty()) {
            return;
        }

        short activeSlot = player.getInventory().getActiveHotbarSlot();
        if (current.getQuantity() <= 1) {
            player.getInventory().getHotbar().setItemStackForSlot(activeSlot, ItemStack.EMPTY);
        } else {
            player.getInventory().getHotbar().setItemStackForSlot(activeSlot,
                    current.withQuantity(current.getQuantity() - 1));
        }
    }
}
