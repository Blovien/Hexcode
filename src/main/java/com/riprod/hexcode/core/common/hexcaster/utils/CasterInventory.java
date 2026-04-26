package com.riprod.hexcode.core.common.hexcaster.utils;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
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
        Pair<HexSlot, HexBookComponent> result = getHexBookComponent(store, playerRef, HexSlot.OffHand);
        return result != null ? result.getSecond() : null;
    }

    @Nullable
    public static Pair<HexSlot, HexBookComponent> getHexBookComponent(ComponentAccessor<EntityStore> store,
            Ref<EntityStore> playerRef, HexSlot slot) {
        Pair<ItemStack, HexSlot> inventoryPair = PlayerUtils.getItemFromInventory(store, playerRef, slot, true);
        if (inventoryPair == null) {
            LOGGER.atWarning().log("could not read inventory for entity ref: " + playerRef);
            return null;
        }
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
            return new Pair<>(inventoryPair.getSecond(), existingComponent);
        }

        // Check if this item is a registered HexBook asset
        HexBookAsset bookAsset = getHexBookAsset(inventoryItem);
        if (bookAsset == null) {
            LOGGER.atInfo().log("No HexBook asset found for item in main hand");
            return null;
        }

        // Create and initialize new component
        HexBookComponent newComponent = new HexBookComponent(bookAsset);
        ItemStack newStack = inventoryItem.withMetadata(METADATA_KEY_HEX_BOOK, HexBookComponent.CODEC, newComponent);

        PlayerUtils.setHandItem(store, playerRef, slot, newStack);

        return new Pair<>(slot, newComponent);
    }

    public static void saveHexBookComponent(ComponentAccessor<EntityStore> store,
            Ref<EntityStore> playerRef, HexBookComponent component) {
        saveHexBookComponent(store, playerRef, component, HexSlot.OffHand);
    }

    public static void saveHexBookComponent(ComponentAccessor<EntityStore> store,
            Ref<EntityStore> playerRef, HexBookComponent component, HexSlot slot) {
        ItemStack item = PlayerUtils.getHandItem(store, playerRef, slot);
        if (item == null || item.isEmpty()) return;
        ItemStack newStack = item.withMetadata(METADATA_KEY_HEX_BOOK, HexBookComponent.CODEC, component);
        PlayerUtils.setHandItem(store, playerRef, slot, newStack);
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
        ItemStack mainHandItem = PlayerUtils.getHandItem(store, playerRef, HexSlot.MainHand);
        if (mainHandItem == null || mainHandItem.isEmpty()) return;
        ItemStack newStack = mainHandItem.withMetadata(METADATA_KEY_HEX_STAFF, HexStaffComponent.CODEC, component);
        PlayerUtils.setHandItem(store, playerRef, HexSlot.MainHand, newStack);
    }

    @Nullable
    public static HexStaffComponent getHexStaffComponent(ComponentAccessor<EntityStore> store,
            Ref<EntityStore> playerRef) {
        ItemStack mainHandItem = InventoryComponent.getItemInHand(store, playerRef);
        if (mainHandItem == null || mainHandItem.isEmpty()) {
            return null;
        }

        HexStaffComponent existingComponent = mainHandItem.getFromMetadataOrNull(METADATA_KEY_HEX_STAFF,
                HexStaffComponent.CODEC);
        if (existingComponent != null) {
            return existingComponent;
        }

        HexStaffAsset staffAsset = getHexStaffAsset(mainHandItem);
        if (staffAsset == null) {
            return null;
        }

        HexStaffComponent newComponent = new HexStaffComponent(staffAsset);
        ItemStack newStack = mainHandItem.withMetadata(METADATA_KEY_HEX_STAFF, HexStaffComponent.CODEC, newComponent);

        PlayerUtils.setHandItem(store, playerRef, HexSlot.MainHand, newStack);

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
