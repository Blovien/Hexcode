package com.riprod.hexcode.core.common.armor;

import java.util.Map;

import javax.annotation.Nonnull;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemArmor;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public final class ArmorManaPatcher {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // base weights for chest/legs/head/hands, summing to 100
    private static final int W_CHEST = 40;
    private static final int W_LEGS = 30;
    private static final int W_HEAD = 20;
    private static final int W_HANDS = 10;

    private ArmorManaPatcher() {
    }

    public static void onItemsLoaded(
            @Nonnull LoadedAssetsEvent<String, Item, DefaultAssetMap<String, Item>> event) {
        LOGGER.atInfo().log("ArmorManaPatcher: onItemsLoaded fired (initial=%s, loaded=%d)",
                event.isInitial(), event.getLoadedAssets().size());

        int manaIndex = DefaultEntityStatTypes.getMana();
        if (manaIndex == Integer.MIN_VALUE) {
            LOGGER.atWarning().log("ArmorManaPatcher: Mana stat index not resolved, skipping");
            return;
        }

        Map<String, Integer> tiers = loadTiers();
        LOGGER.atInfo().log("ArmorManaPatcher: loaded %d tiers from ArmorManaConfig", tiers.size());
        if (tiers.isEmpty()) {
            LOGGER.atWarning().log("ArmorManaPatcher: no tiers loaded from ArmorManaConfig, skipping");
            return;
        }

        Map<String, Item> items = event.getAssetMap().getAssetMap();
        int patched = 0;

        for (Map.Entry<String, Integer> entry : tiers.entrySet()) {
            String tier = entry.getKey();
            int total = entry.getValue();
            if (total <= 0) continue;

            Item chest = items.get("Armor_" + tier + "_Chest");
            Item legs = items.get("Armor_" + tier + "_Legs");
            Item head = items.get("Armor_" + tier + "_Head");
            Item hands = items.get("Armor_" + tier + "_Hands");

            patched += patch(chest, scale(total, W_CHEST), manaIndex);
            patched += patch(legs, scale(total, W_LEGS), manaIndex);
            patched += patch(head, scale(total, W_HEAD), manaIndex);
            patched += patch(hands, scale(total, W_HANDS), manaIndex);
        }

        LOGGER.atInfo().log("ArmorManaPatcher: patched %d armor pieces with Mana modifiers", patched);
    }

    private static Map<String, Integer> loadTiers() {
        try {
            DefaultAssetMap<String, ArmorManaConfig> map = ArmorManaConfig.getAssetMap();
            for (ArmorManaConfig config : map.getAssetMap().values()) {
                if (config != null && !config.getTiers().isEmpty()) {
                    return config.getTiers();
                }
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("ArmorManaPatcher: failed to load ArmorManaConfig: %s", e.getMessage());
        }
        return Map.of();
    }

    private static int scale(int total, int weight) {
        return Math.round((float) total * weight / 100f);
    }

    private static int patch(Item item, int amount, int manaIndex) {
        if (item == null || amount <= 0) return 0;
        ItemArmor armor = item.getArmor();
        if (armor == null) return 0;

        Int2ObjectMap<StaticModifier[]> existing = armor.getStatModifiers();
        if (existing != null) {
            StaticModifier[] current = existing.get(manaIndex);
            if (current != null && current.length > 0) return 0;
        }

        Int2ObjectMap<StaticModifier[]> target;
        if (existing == null) {
            target = new Int2ObjectOpenHashMap<>();
            setStatModifiers(armor, target);
        } else {
            target = existing;
        }

        StaticModifier manaMod = new StaticModifier(
                Modifier.ModifierTarget.MAX,
                StaticModifier.CalculationType.ADDITIVE,
                (float) amount);
        target.put(manaIndex, new StaticModifier[] { manaMod });
        return 1;
    }

    private static void setStatModifiers(ItemArmor armor, Int2ObjectMap<StaticModifier[]> map) {
        try {
            java.lang.reflect.Field f = ItemArmor.class.getDeclaredField("statModifiers");
            f.setAccessible(true);
            f.set(armor, map);
        } catch (ReflectiveOperationException e) {
            LOGGER.atWarning().log("ArmorManaPatcher: reflective set of statModifiers failed: %s",
                    e.getMessage());
        }
    }
}
