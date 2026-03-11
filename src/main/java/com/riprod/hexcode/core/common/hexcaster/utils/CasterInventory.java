package com.riprod.hexcode.core.common.hexcaster.utils;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexbook.component.HexBookAsset;
import com.riprod.hexcode.core.common.hexbook.component.HexBookComponent;
import com.riprod.hexcode.core.common.hexstaff.component.HexStaffAsset;
import com.riprod.hexcode.core.common.hexstaff.component.HexStaffComponent;
import com.riprod.hexcode.utils.HexSlot;

import io.sentry.util.Pair;

public class CasterInventory {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String METADATA_KEY_HEX_STAFF = "HexStaff";
    public static final String METADATA_KEY_HEX_BOOK = "HexBook";

    @Nullable
    public static HexBookComponent getHexBookComponent(ComponentAccessor<EntityStore> store,
            Ref<EntityStore> playerRef) {
        return getHexBookComponent(store, playerRef, HexSlot.OffHand).getSecond();
    }

    @Nullable
    public static Pair<HexSlot, HexBookComponent> getHexBookComponent(ComponentAccessor<EntityStore> store,
            Ref<EntityStore> playerRef, HexSlot slot) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            LOGGER.atWarning().log("Player component not found for entity ref: " + playerRef);
            return null;
        }

        Pair<ItemStack, HexSlot> inventoryPair = PlayerUtils.getItemFromInventory(player, slot, true);
        slot = inventoryPair.getSecond();
        ItemStack inventoryItem = inventoryPair.getFirst();

        if (inventoryItem == null || inventoryItem.isEmpty()) {
            LOGGER.atInfo().log("No item in hand");
            return null;
        }

        // Check if component already exists
        HexBookComponent existingComponent = inventoryItem.getFromMetadataOrNull(METADATA_KEY_HEX_BOOK,
                HexBookComponent.CODEC);
        if (existingComponent != null) {
            LOGGER.atInfo().log("Found existing HexBookComponent in item metadata");
            return new Pair<>(inventoryPair.getSecond(), existingComponent);
        }
        LOGGER.atInfo().log("Creating HexBookComponent...");

        // Check if this item is a registered HexBook asset
        HexBookAsset bookAsset = getHexBookAsset(inventoryItem);
        if (bookAsset == null) {
            LOGGER.atInfo().log("No HexBook asset found for item in main hand");
            return null;
        }

        // Create and initialize new component
        HexBookComponent newComponent = new HexBookComponent(bookAsset);
        ItemStack newStack = inventoryItem.withMetadata(METADATA_KEY_HEX_BOOK, HexBookComponent.CODEC, newComponent);

        PlayerUtils.setHandItem(player, slot, newStack);

        return new Pair<>(slot, newComponent);
    }

    public static void saveHexBookComponent(ComponentAccessor<EntityStore> store,
            Ref<EntityStore> playerRef, HexBookComponent component) {
        saveHexBookComponent(store, playerRef, component, HexSlot.OffHand);
    }

    public static void saveHexBookComponent(ComponentAccessor<EntityStore> store,
            Ref<EntityStore> playerRef, HexBookComponent component, HexSlot slot) {

        Player player = store.getComponent(playerRef, Player.getComponentType());
        ItemStack item = PlayerUtils.getHandItem(player, slot);
        ItemStack newStack = item.withMetadata(METADATA_KEY_HEX_BOOK, HexBookComponent.CODEC, component);
        PlayerUtils.setHandItem(player, slot, newStack);
    }

    public static boolean withHexBook(ComponentAccessor<EntityStore> store,
            Ref<EntityStore> playerRef, HexSlot slot,
            java.util.function.Consumer<HexBookComponent> mutator) {

        Pair<HexSlot, HexBookComponent> result = getHexBookComponent(store, playerRef, slot);
        if (result == null) return false;

        mutator.accept(result.getSecond());
        saveHexBookComponent(store, playerRef, result.getSecond(), result.getFirst());
        return true;
    }

    public static void saveHexStaffComponent(ComponentAccessor<EntityStore> store,
            Ref<EntityStore> playerRef, HexStaffComponent component) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        ItemStack mainHandItem = PlayerUtils.getHandItem(player, HexSlot.MainHand);
        ItemStack newStack = mainHandItem.withMetadata(METADATA_KEY_HEX_STAFF, HexStaffComponent.CODEC, component);
        PlayerUtils.setHandItem(player, HexSlot.MainHand, newStack);
    }

    @Nullable
    public static HexStaffComponent getHexStaffComponent(ComponentAccessor<EntityStore> store,
            Ref<EntityStore> playerRef) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            return null;
        }
        ItemStack mainHandItem = player.getInventory().getItemInHand();
        if (mainHandItem == null || mainHandItem.isEmpty()) {
            return null;
        }

        // Check if component already exists
        HexStaffComponent existingComponent = mainHandItem.getFromMetadataOrNull(METADATA_KEY_HEX_STAFF,
                HexStaffComponent.CODEC);
        if (existingComponent != null) {
            LOGGER.atInfo().log("Found existing HexStaffComponent in item metadata");
            return existingComponent;
        }
        LOGGER.atInfo().log("Creating HexStaffComponent...");

        // Check if this item is a registered HexStaff asset
        HexStaffAsset staffAsset = getHexStaffAsset(mainHandItem);
        if (staffAsset == null) {
            LOGGER.atInfo().log("No HexStaff asset found for item in main hand");
            return null;
        }

        // Create and initialize new component
        HexStaffComponent newComponent = new HexStaffComponent(staffAsset);
        ItemStack newStack = mainHandItem.withMetadata(METADATA_KEY_HEX_STAFF, HexStaffComponent.CODEC, newComponent);

        // replace item
        short activeSlot = player.getInventory().getActiveHotbarSlot();
        player.getInventory().getHotbar().setItemStackForSlot(activeSlot, newStack);

        return newComponent;
    }

    @Nullable
    public static HexStaffAsset getHexStaffAsset(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty())
            return null;
        Item item = itemStack.getItem();
        if (item == null)
            return null;
        return HexStaffAsset.getAssetMap().getAsset(item.getId());
    }

    @Nullable
    public static HexBookAsset getHexBookAsset(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty())
            return null;
        Item item = itemStack.getItem();
        if (item == null)
            return null;
        return HexBookAsset.getAssetMap().getAsset(item.getId());
    }
}
