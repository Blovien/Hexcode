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

public class CasterInventory {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String METADATA_KEY_HEX_STAFF = "HexStaff";
    public static final String METADATA_KEY_HEX_BOOK = "HexBook";

    @Nullable
    public static HexBookComponent getHexBookComponent(ComponentAccessor<EntityStore> store,
            Ref<EntityStore> playerRef) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            LOGGER.atWarning().log("Player component not found for entity ref: " + playerRef);
            return null;
        }

        ItemStack utilityItem = player.getInventory().getUtilityItem();
        if (utilityItem == null || utilityItem.isEmpty()) {
            LOGGER.atInfo().log("No item in main hand");
            return null;
        }

        // Check if component already exists
        HexBookComponent existingComponent = utilityItem.getFromMetadataOrNull(METADATA_KEY_HEX_BOOK,
                HexBookComponent.CODEC);
        if (existingComponent != null) {
            LOGGER.atInfo().log("Found existing HexBookComponent in item metadata");
            return existingComponent;
        }
        LOGGER.atInfo().log("Creating HexBookComponent...");

        // Check if this item is a registered HexBook asset
        HexBookAsset bookAsset = getHexBookAsset(utilityItem);
        if (bookAsset == null) {
            LOGGER.atInfo().log("No HexBook asset found for item in main hand");
            return null;
        }

        // Create and initialize new component
        HexBookComponent newComponent = new HexBookComponent(bookAsset);
        ItemStack newStack = utilityItem.withMetadata(METADATA_KEY_HEX_BOOK, HexBookComponent.CODEC, newComponent);

        // replace item
        short activeSlot = player.getInventory().getActiveUtilitySlot();
        player.getInventory().getUtility().setItemStackForSlot(activeSlot, newStack);

        return newComponent;
    }

    public static void saveHexBookComponent(ComponentAccessor<EntityStore> store,
            Ref<EntityStore> playerRef, HexBookComponent component) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        ItemStack utilityItem = player.getInventory().getUtilityItem();
        ItemStack newStack = utilityItem.withMetadata(METADATA_KEY_HEX_BOOK, HexBookComponent.CODEC, component);
        short activeSlot = player.getInventory().getActiveUtilitySlot();
        player.getInventory().getUtility().setItemStackForSlot(activeSlot, newStack);
    }

    public static void saveHexStaffComponent(ComponentAccessor<EntityStore> store,
            Ref<EntityStore> playerRef, HexStaffComponent component) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        ItemStack mainHandItem = player.getInventory().getItemInHand();
        ItemStack newStack = mainHandItem.withMetadata(METADATA_KEY_HEX_STAFF, HexStaffComponent.CODEC, component);
        short activeSlot = player.getInventory().getActiveHotbarSlot();
        player.getInventory().getHotbar().setItemStackForSlot(activeSlot, newStack);
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
