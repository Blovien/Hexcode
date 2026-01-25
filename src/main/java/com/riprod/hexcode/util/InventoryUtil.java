package com.riprod.hexcode.util;

import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;

import javax.annotation.Nonnull;

/**
 * Utility methods for inventory operations.
 *
 * <p>Since Hytale's ItemStack is immutable, these helpers assist with
 * the common pattern of updating inventory slots after ItemStack modifications.
 *
 * @see HexBookMetadata
 */
public class InventoryUtil {

    private InventoryUtil() {} // Utility class

    /**
     * Update the offhand (utility) slot with a new ItemStack.
     *
     * <p>Used when a book UUID is created for the first time and the
     * ItemStack needs to be replaced with the new metadata-bearing stack.
     *
     * <h3>Example Usage</h3>
     * <pre>
     * BookUUIDResult result = HexBookMetadata.getOrCreateBookUUID(bookStack);
     * if (result.wasCreated()) {
     *     InventoryUtil.updateOffhandItem(inventory, result.stack());
     * }
     * </pre>
     *
     * @param inventory The player's inventory
     * @param newStack The new ItemStack to place in the utility slot
     */
    public static void updateOffhandItem(@Nonnull Inventory inventory, @Nonnull ItemStack newStack) {
        short inventorySlot = inventory.getActiveUtilitySlot();
        inventory.getUtility().setItemStackForSlot(inventorySlot, newStack);
    }

    /**
     * Update the main hand (hotbar active slot) with a new ItemStack.
     *
     * <p>Used when an item's metadata is modified and the ItemStack needs
     * to be replaced in the player's hand.
     *
     * @param inventory The player's inventory
     * @param newStack The new ItemStack to place in the main hand
     */
    public static void updateMainHandItem(@Nonnull Inventory inventory, @Nonnull ItemStack newStack) {
        short mainHandSlot = inventory.getActiveHotbarSlot();
        inventory.getHotbar().setItemStackForSlot(mainHandSlot, newStack);
    }
}
