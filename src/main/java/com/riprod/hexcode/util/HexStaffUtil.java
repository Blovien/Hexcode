package com.riprod.hexcode.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.entities.Player;

/**
 * Utility methods for detecting and working with the Hex Staff and Hex Book.
 *
 * Required setup for Hexcode:
 * - Hex Staff must be in the main hand (hotbar active slot)
 * - Hex Book must be in the offhand (utility slot)
 */
public class HexStaffUtil {
    public static final String HEX_STAFF_ID = "hexcode:hex_staff";
    public static final String HEX_BOOK_ID = "hexcode:hex_book";

    private HexStaffUtil() {}

    /**
     * Check if the given item is a Hex Staff.
     *
     * @param itemStack The item stack to check
     * @return true if this is a Hex Staff
     */
    public static boolean isHexStaff(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        String itemId = itemStack.getItem().getId();
        return HEX_STAFF_ID.equals(itemId) || itemId.contains("Hex_Staff");
    }

    /**
     * Check if the given item is a Hex Book.
     *
     * @param itemStack The item stack to check
     * @return true if this is a Hex Book
     */
    public static boolean isHexBook(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        String itemId = itemStack.getItem().getId();
        return HEX_BOOK_ID.equals(itemId) || itemId.contains("Hex_Book");
    }

    /**
     * Check if the player has the required Hexcode equipment:
     * - Hex Staff in main hand (hotbar active slot)
     * - Hex Book in offhand (utility slot)
     *
     * @param store The entity store
     * @param playerRef The player entity reference
     * @return true if the player has both staff in main hand and book in offhand
     */
    public static boolean hasHexcodeEquipment(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            return false;
        }
        return hasHexcodeEquipment(player.getInventory());
    }

    /**
     * Check if the inventory has the required Hexcode equipment:
     * - Hex Staff in main hand (hotbar active slot)
     * - Hex Book in offhand (utility slot)
     *
     * @param inventory The player's inventory
     * @return true if staff is in main hand and book is in offhand
     */
    public static boolean hasHexcodeEquipment(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        return hasHexStaffInMainHand(inventory) && hasHexBookInOffhand(inventory);
    }

    /**
     * Check if the player has a Hex Staff in their main hand (hotbar active slot).
     *
     * @param inventory The player's inventory
     * @return true if there's a Hex Staff in main hand
     */
    public static boolean hasHexStaffInMainHand(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        ItemStack mainHandItem = inventory.getItemInHand();
        return isHexStaff(mainHandItem);
    }

    /**
     * Check if the player has a Hex Book in their offhand (utility slot).
     *
     * @param inventory The player's inventory
     * @return true if there's a Hex Book in offhand
     */
    public static boolean hasHexBookInOffhand(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        ItemStack utilityItem = inventory.getUtilityItem();
        return isHexBook(utilityItem);
    }

    /**
     * Get the Hex Staff from the player's main hand.
     *
     * @param inventory The player's inventory
     * @return The Hex Staff item stack, or null if not equipped
     */
    public static ItemStack getHexStaffFromMainHand(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        ItemStack mainHandItem = inventory.getItemInHand();
        if (isHexStaff(mainHandItem)) {
            return mainHandItem;
        }
        return null;
    }

    /**
     * Get the Hex Book from the player's offhand.
     *
     * @param inventory The player's inventory
     * @return The Hex Book item stack, or null if not equipped
     */
    public static ItemStack getHexBookFromOffhand(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        ItemStack utilityItem = inventory.getUtilityItem();
        if (isHexBook(utilityItem)) {
            return utilityItem;
        }
        return null;
    }

    // ========== Deprecated methods for backwards compatibility ==========

    /**
     * @deprecated Use {@link #hasHexStaffInMainHand(Inventory)} instead
     */
    @Deprecated
    public static boolean hasHexStaffInOffhand(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        return hasHexcodeEquipment(store, playerRef);
    }

    /**
     * @deprecated Use {@link #hasHexcodeEquipment(Inventory)} instead
     */
    @Deprecated
    public static boolean hasHexStaffInOffhand(Inventory inventory) {
        return hasHexcodeEquipment(inventory);
    }

    /**
     * @deprecated Use {@link #getHexStaffFromMainHand(Inventory)} instead
     */
    @Deprecated
    public static ItemStack getHexStaffFromOffhand(Inventory inventory) {
        return getHexStaffFromMainHand(inventory);
    }
}
