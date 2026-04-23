package com.riprod.hexcode.core.common.hexcaster.utils;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.utils.HexSlot;

import io.sentry.util.Pair;

public class PlayerUtils {
    public static Vector3d getPlayerEyePosition(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> playerRef) {
        // Get the player's transform component to determine their position and eye
        // height
        TransformComponent playerTransform = accessor.getComponent(playerRef,
                TransformComponent.getComponentType());
        if (playerTransform == null) {
            return new Vector3d(0, 0, 0); // Fallback to origin if we can't get the transform
        }

        ModelComponent modelComp = accessor.getComponent(playerRef, ModelComponent.getComponentType());
        float eyeHeight = modelComp != null ? modelComp.getModel().getEyeHeight(playerRef, accessor) : 1.68f;

        Vector3d playerPos = playerTransform.getPosition();

        return new Vector3d(playerPos.x, playerPos.y + eyeHeight, playerPos.z);
    }

    public static void consumeOneFromHand(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref, HexSlot slot) {
        ItemStack current = getHandItem(accessor, ref, slot);
        if (current == null || current.isEmpty()) {
            return;
        }

        if (current.getQuantity() <= 1) {
            setHandItem(accessor, ref, slot, ItemStack.EMPTY);
        } else {
            setHandItem(accessor, ref, slot, current.withQuantity(current.getQuantity() - 1));
        }
    }

    public static Pair<ItemStack, HexSlot> getItemFromInventory(ComponentAccessor<EntityStore> accessor,
            Ref<EntityStore> ref, HexSlot slot) {
        return getItemFromInventory(accessor, ref, slot, false);
    }

    public static Pair<ItemStack, HexSlot> getItemFromInventory(ComponentAccessor<EntityStore> accessor,
            Ref<EntityStore> ref, HexSlot slot, boolean preferOffhand) {
        if (slot == HexSlot.MainHand) {
            return new Pair<>(getActiveHotbarItem(accessor, ref), HexSlot.MainHand);
        }
        if (slot == HexSlot.OffHand) {
            return new Pair<>(getActiveUtilityItem(accessor, ref), HexSlot.OffHand);
        }

        HexSlot first = preferOffhand ? HexSlot.OffHand : HexSlot.MainHand;
        HexSlot second = preferOffhand ? HexSlot.MainHand : HexSlot.OffHand;

        ItemStack item = getHandItem(accessor, ref, first);
        if (item == null || item.isEmpty()) {
            return new Pair<>(getHandItem(accessor, ref, second), second);
        }
        return new Pair<>(item, first);
    }

    public static Pair<ItemStack, ItemStack> getItemFromHands(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref) {
        ItemStack mainHand = getActiveHotbarItem(accessor, ref);
        ItemStack offHand = getActiveUtilityItem(accessor, ref);
        return new Pair<>(mainHand, offHand);
    }

    public static ItemStack getHandItem(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref, HexSlot slot) {
        return slot == HexSlot.OffHand
                ? getActiveUtilityItem(accessor, ref)
                : getActiveHotbarItem(accessor, ref);
    }

    public static void setHandItem(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref,
            HexSlot slot, ItemStack item) {
        if (slot == HexSlot.OffHand) {
            InventoryComponent.Utility utility = accessor.getComponent(ref, InventoryComponent.Utility.getComponentType());
            if (utility == null) return;
            byte activeSlot = utility.getActiveSlot();
            if (activeSlot < 0) return;
            utility.getInventory().setItemStackForSlot(activeSlot, item, false);
        } else {
            InventoryComponent.Hotbar hotbar = accessor.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
            if (hotbar == null) return;
            byte activeSlot = hotbar.getActiveSlot();
            if (activeSlot < 0) return;
            hotbar.getInventory().setItemStackForSlot(activeSlot, item);
        }
    }

    public static void addHandItem(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref,
            HexSlot slot, ItemStack item) {
        if (item == null || item.isEmpty()) return;

        ItemStack currentHandItem = getHandItem(accessor, ref, slot);
        if (currentHandItem == null || currentHandItem.isEmpty()) {
            setHandItem(accessor, ref, slot, item);
            return;
        }

        CombinedItemContainer combined = InventoryComponent.getCombined(
                accessor, ref, InventoryComponent.HOTBAR_STORAGE_BACKPACK);
        ItemStackTransaction transaction = combined.addItemStack(item);
        ItemStack remainder = transaction.getRemainder();
        if (!ItemStack.isEmpty(remainder)) {
            ItemUtils.dropItem(ref, remainder, accessor);
        }
    }

    private static ItemStack getActiveHotbarItem(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref) {
        InventoryComponent.Hotbar hotbar = accessor.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        return hotbar != null ? hotbar.getActiveItem() : null;
    }

    private static ItemStack getActiveUtilityItem(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref) {
        InventoryComponent.Utility utility = accessor.getComponent(ref, InventoryComponent.Utility.getComponentType());
        return utility != null ? utility.getActiveItem() : null;
    }

}
