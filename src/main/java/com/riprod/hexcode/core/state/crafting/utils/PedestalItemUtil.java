package com.riprod.hexcode.core.state.crafting.utils;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexbook.component.HexBookAsset;
import com.riprod.hexcode.core.common.hexbook.component.HexBookComponent;
import com.riprod.hexcode.core.common.hexcaster.utils.CasterInventory;

public class PedestalItemUtil {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static boolean isEssence(@Nullable ItemStack item) {
        if (item == null || item.isEmpty() || item.getItem() == null) {
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

    public static ItemStack ensureHexBookComponent(ItemStack bookStack) {
        if (bookStack == null || bookStack.isEmpty()) {
            return bookStack;
        }

        HexBookComponent existing = bookStack.getFromMetadataOrNull(
                CasterInventory.METADATA_KEY_HEX_BOOK, HexBookComponent.CODEC);
        if (existing != null) {
            return bookStack;
        }

        HexBookAsset bookAsset = CasterInventory.getHexBookAsset(bookStack);
        if (bookAsset == null) {
            LOGGER.atWarning().log("no HexBookAsset for item %s", bookStack.getItem().getId());
            return bookStack;
        }

        HexBookComponent newComponent = new HexBookComponent(bookAsset);
        return bookStack.withMetadata(
                CasterInventory.METADATA_KEY_HEX_BOOK, HexBookComponent.CODEC, newComponent);
    }

    public static ItemStack saveHexBookComponent(ItemStack bookStack, HexBookComponent component) {
        if (bookStack == null || bookStack.isEmpty()) {
            return bookStack;
        }
        return bookStack.withMetadata(
                CasterInventory.METADATA_KEY_HEX_BOOK, HexBookComponent.CODEC, component);
    }

    public static void returnBookToPlayer(Player player, ItemStack bookStack) {
        if (bookStack == null || bookStack.isEmpty()) {
            return;
        }
        player.getInventory().getHotbar().addItemStack(bookStack);
    }

    @Nullable
    public static Ref<EntityStore> dropBookAtPosition(
            ComponentAccessor<EntityStore> accessor, ItemStack bookStack, Vector3i blockPos) {
        if (bookStack == null || bookStack.isEmpty()) {
            return null;
        }

        Vector3d dropPos = new Vector3d(blockPos.x + 0.5, blockPos.y + 1.0, blockPos.z + 0.5);
        Holder<EntityStore> holder = ItemComponent.generateItemDrop(
                accessor, bookStack, dropPos, Vector3f.ZERO, 0f, 0.2f, 0f);
        if (holder == null) {
            return null;
        }

        return accessor.addEntity(holder, AddReason.SPAWN);
    }
}
