package com.riprod.hexcode.core.common.imbuement.handler;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.imbuement.component.ImbuementData;
import com.riprod.hexcode.core.common.imbuement.utils.ImbuementUtils;

public interface ImbuementHandler {

    default ItemStack onImbue(ItemStack stack, String slotKey, Hex hex, Holder<EntityStore> player) {
        ImbuementData data = ImbuementUtils.fromHex(hex);
        return ImbuementUtils.write(stack, slotKey, data);
    }

    default void onSlotEnter(ItemStack stack, String slotKey, Holder<EntityStore> player) {
    }
}
