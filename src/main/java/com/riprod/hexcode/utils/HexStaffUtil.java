package com.riprod.hexcode.utils;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexcaster.utils.PlayerUtils;

import javax.annotation.Nullable;
import java.util.Map;

public class HexStaffUtil {
    public static final String HEX_STAFF_ID = "hexcode:hex_staff";
    public static final String HEX_BOOK_ID = "hexcode:hex_book";

    public static final String TAG_FAMILY_HEX_BOOK = "HexBook";

    public static final String TAG_FAMILY_HEX_STAFF = "HexStaff";

    private HexStaffUtil() {}

    public static boolean isHexBook(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }

        Item item = itemStack.getItem();
        if (item == null) {
            return false;
        }

        if (hasTagFamily(item, TAG_FAMILY_HEX_BOOK)) {
            return true;
        }

        String itemId = item.getId();
        return HEX_BOOK_ID.equals(itemId) || itemId.contains("Hex_Book");
    }

    public static boolean isHexStaff(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }

        Item item = itemStack.getItem();
        if (item == null) {
            return false;
        }

        if (hasTagFamily(item, TAG_FAMILY_HEX_STAFF)) {
            return true;
        }

        String itemId = item.getId();
        return HEX_STAFF_ID.equals(itemId) || itemId.contains("Hex_Staff");
    }

    private static boolean hasTagFamily(@Nullable Item item, String familyValue) {
        if (item == null) {
            return false;
        }

        try {
            var data = item.getData();
            if (data == null) {
                return false;
            }

            Map<String, String[]> rawTags = data.getRawTags();
            if (rawTags == null) {
                return false;
            }

            String[] familyTags = rawTags.get("Family");
            if (familyTags != null) {
                for (String tag : familyTags) {
                    if (familyValue.equals(tag)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
        }

        return false;
    }

    public static boolean hasHexcodeEquipment(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        return hasHexcodeEquipment((ComponentAccessor<EntityStore>) store, playerRef);
    }

    public static boolean hasHexcodeEquipment(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> playerRef) {
        ItemStack mainHand = PlayerUtils.getHandItem(accessor, playerRef, HexSlot.MainHand);
        ItemStack offHand = PlayerUtils.getHandItem(accessor, playerRef, HexSlot.OffHand);
        return isHexStaff(mainHand) && isHexBook(offHand);
    }

    @Nullable
    public static ItemStack getHexStaffFromMainHand(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref) {
        ItemStack mainHandItem = PlayerUtils.getHandItem(accessor, ref, HexSlot.MainHand);
        return isHexStaff(mainHandItem) ? mainHandItem : null;
    }

    @Nullable
    public static ItemStack getHexBookFromOffhand(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref) {
        ItemStack utilityItem = PlayerUtils.getHandItem(accessor, ref, HexSlot.OffHand);
        return isHexBook(utilityItem) ? utilityItem : null;
    }

    @Nullable
    public static ItemStack getHexBookFromMainHand(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref) {
        ItemStack mainHandItem = PlayerUtils.getHandItem(accessor, ref, HexSlot.MainHand);
        return isHexBook(mainHandItem) ? mainHandItem : null;
    }
}
