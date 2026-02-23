package com.riprod.hexcode.core.crafting.utils;

import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.riprod.hexcode.core.hexcaster.utils.CasterInventory;

public class PedestalItemUtil {

    public static boolean isEssence(@Nullable ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }
        if (item.getItem() == null) {
            return false;
        }
        return item.getItem().getId().contains("Essence");
    }

    public static boolean isHexBook(@Nullable ItemStack item) {
        return CasterInventory.getHexBookAsset(item) != null;
    }

    public static boolean isEmptyHand(@Nullable ItemStack item) {
        return item == null || item.isEmpty();
    }
}
