package com.riprod.hexcode.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.entities.Player;

/**
 * Utility methods for detecting and working with the Hex Staff.
 */
public class HexStaffUtil {
    public static final String HEX_STAFF_ID = "hexcode:hex_staff";
    public static final String HEX_STAFF_TAG = "Hexcode";

    private HexStaffUtil() {}

    /**
     * Check if the given item is a Hex Staff.
     *
     * @param itemStack The item stack to check
     * @return true if this is a Hex Staff
     */
    public static boolean isHexStaff(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        // Check by item ID or by tag
        String itemId = itemStack.getItem().getId();
        return HEX_STAFF_ID.equals(itemId) || itemId.contains("Hex_Staff");
    }

    /**
     * Check if the player has a Hex Staff in their offhand (utility slot).
     *
     * @param store The entity store
     * @param playerRef The player entity reference
     * @return true if the player has a Hex Staff in offhand
     */
    public static boolean hasHexStaffInOffhand(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            return false;
        }
        return hasHexStaffInOffhand(player.getInventory());
    }

    /**
     * Check if the inventory has a Hex Staff in the offhand (utility slot).
     *
     * @param inventory The player's inventory
     * @return true if there's a Hex Staff in offhand
     */
    public static boolean hasHexStaffInOffhand(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        ItemStack utilityItem = inventory.getUtilityItem();
        return isHexStaff(utilityItem);
    }

    /**
     * Get the Hex Staff from the player's offhand.
     *
     * @param inventory The player's inventory
     * @return The Hex Staff item stack, or null if not equipped
     */
    public static ItemStack getHexStaffFromOffhand(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        ItemStack utilityItem = inventory.getUtilityItem();
        if (isHexStaff(utilityItem)) {
            return utilityItem;
        }
        return null;
    }
}
