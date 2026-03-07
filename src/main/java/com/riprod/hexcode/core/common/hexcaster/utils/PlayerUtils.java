package com.riprod.hexcode.core.common.hexcaster.utils;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
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
            return Vector3d.ZERO; // Fallback to origin if we can't get the transform
        }

        ModelComponent modelComp = accessor.getComponent(playerRef, ModelComponent.getComponentType());
        float eyeHeight = modelComp != null ? modelComp.getModel().getEyeHeight(playerRef, accessor) : 1.68f;

        Vector3d playerPos = playerTransform.getPosition();

        return new Vector3d(playerPos.x, playerPos.y + eyeHeight, playerPos.z);
    }

    public static void consumeOneFromHand(Player player) {
        consumeOneFromHand(player, HexSlot.MainHand);
    }

    public static void consumeOneFromHand(Player player, HexSlot slot) {
        ItemStack current = getHandItem(player, slot);
        if (current == null || current.isEmpty()) {
            return;
        }

        if (current.getQuantity() <= 1) {
            setHandItem(player, slot, ItemStack.EMPTY);
        } else {
            setHandItem(player, slot, current.withQuantity(current.getQuantity() - 1));
        }
    }

    public static Pair<ItemStack, HexSlot> getItemFromInventory(Player player, HexSlot slot) {
        return getItemFromInventory(player, slot, false);
    }

    public static Pair<ItemStack, HexSlot> getItemFromInventory(Player player, HexSlot slot, boolean preferOffhand) {
        if (slot == HexSlot.MainHand) {
            return new Pair<>(player.getInventory().getActiveHotbarItem(), HexSlot.MainHand);
        }
        if (slot == HexSlot.OffHand) {
            return new Pair<>(player.getInventory().getUtilityItem(), HexSlot.OffHand);
        }

        HexSlot first = preferOffhand ? HexSlot.OffHand : HexSlot.MainHand;
        HexSlot second = preferOffhand ? HexSlot.MainHand : HexSlot.OffHand;

        ItemStack item = getHandItem(player, first);
        if (item == null || item.isEmpty()) {
            return new Pair<>(getHandItem(player, second), second);
        }
        return new Pair<>(item, first);
    }

    public static ItemStack getHandItem(Player player, HexSlot slot) {
        return slot == HexSlot.OffHand
                ? player.getInventory().getUtilityItem()
                : player.getInventory().getActiveHotbarItem();
    }

    public static void setHandItem(Player player, HexSlot slot, ItemStack item) {
        if (slot == HexSlot.OffHand) {
            short s = player.getInventory().getActiveUtilitySlot();
            // bypass slot filter — utility filter rejects empty/non-utility items
            player.getInventory().getUtility().setItemStackForSlot(s, item, false);
        } else {
            short s = player.getInventory().getActiveHotbarSlot();
            player.getInventory().getHotbar().setItemStackForSlot(s, item);
        }
    }

}
