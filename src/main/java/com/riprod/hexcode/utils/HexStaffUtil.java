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

/**
 * Utility methods for detecting and working with the Hex Staff and Hex Book.
 *
 * <p>Detection uses a two-phase approach:
 * <ol>
 *   <li><b>Tag-based detection</b>: Checks for Tags.Family = "HexBook" or "HexStaff"</li>
 *   <li><b>Fallback ID matching</b>: Checks item ID contains "Hex_Staff" or "Hex_Book"</li>
 * </ol>
 *
 * <p>Tag-based detection allows third-party mods to create custom hex books by
 * simply setting Parent: "Template_HexBook" in their item JSON, without code changes.
 *
 * <p>Required setup for Hexcode:
 * <ul>
 *   <li>Hex Staff must be in the main hand (hotbar active slot)</li>
 *   <li>Hex Book must be in the offhand (utility slot)</li>
 * </ul>
 */
public class HexStaffUtil {
    public static final String HEX_STAFF_ID = "hexcode:hex_staff";
    public static final String HEX_BOOK_ID = "hexcode:hex_book";

    /** Tag family value for Hex Books */
    public static final String TAG_FAMILY_HEX_BOOK = "HexBook";

    /** Tag family value for Hex Staffs */
    public static final String TAG_FAMILY_HEX_STAFF = "HexStaff";

    private HexStaffUtil() {}

    // ==================== TAG-BASED DETECTION ====================

    /**
     * Check if the given item is a Hex Book using tag-based detection.
     *
     * <p>First checks for Tags.Family containing "HexBook", then falls back to ID matching.
     * This allows third-party books that inherit from Template_HexBook to be auto-detected.
     *
     * @param itemStack The item stack to check
     * @return true if this is a Hex Book
     */
    public static boolean isHexBook(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }

        Item item = itemStack.getItem();
        if (item == null) {
            return false;
        }

        // Primary: Tag-based detection
        if (hasTagFamily(item, TAG_FAMILY_HEX_BOOK)) {
            return true;
        }

        // Fallback: ID-based detection for backwards compatibility
        String itemId = item.getId();
        return HEX_BOOK_ID.equals(itemId) || itemId.contains("Hex_Book");
    }

    /**
     * Check if the given item is a Hex Staff using tag-based detection.
     *
     * <p>First checks for Tags.Family containing "HexStaff", then falls back to ID matching.
     *
     * @param itemStack The item stack to check
     * @return true if this is a Hex Staff
     */
    public static boolean isHexStaff(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }

        Item item = itemStack.getItem();
        if (item == null) {
            return false;
        }

        // Primary: Tag-based detection
        if (hasTagFamily(item, TAG_FAMILY_HEX_STAFF)) {
            return true;
        }

        // Fallback: ID-based detection for backwards compatibility
        String itemId = item.getId();
        return HEX_STAFF_ID.equals(itemId) || itemId.contains("Hex_Staff");
    }

    /**
     * Check if an item has a specific tag family value.
     *
     * <p>Checks the "Family" tag category for the given value.
     * Tags are accessed via item.getData().getRawTags() which returns Map&lt;String, String[]&gt;.
     *
     * @param item The item to check
     * @param familyValue The family tag value to look for (e.g., "HexBook")
     * @return true if the item has this family tag
     */
    private static boolean hasTagFamily(@Nullable Item item, String familyValue) {
        if (item == null) {
            return false;
        }

        try {
            // Hytale's Item API provides tag access through getData().getRawTags()
            // Tags are defined in JSON as: "Tags": { "Family": ["HexBook"] }
            // getRawTags() returns Map<String, String[]>
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
            // Tag API not available or different signature, fall back to ID check
        }

        return false;
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
