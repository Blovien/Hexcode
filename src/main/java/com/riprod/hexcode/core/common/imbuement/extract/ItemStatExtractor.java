package com.riprod.hexcode.core.common.imbuement.extract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemArmor;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemWeapon;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public final class ItemStatExtractor {

    private ItemStatExtractor() {
    }

    public static float extractVolatility(@Nullable ItemStack stack) {
        return extractStat(stack, "Volatility");
    }

    public static float extractPower(@Nullable ItemStack stack) {
        return extractStat(stack, "Magic_Power");
    }

    public static float extractMana(@Nullable ItemStack stack) {
        return extractStat(stack, "Mana");
    }

    private static float extractStat(@Nullable ItemStack stack, @Nonnull String statName) {
        Int2ObjectMap<StaticModifier[]> modifiers = modifiersOf(stack);
        if (modifiers == null || modifiers.isEmpty()) return 0f;
        int idx = EntityStatType.getAssetMap().getIndex(statName);
        if (idx == Integer.MIN_VALUE) return 0f;
        return sumAdditive(modifiers.get(idx));
    }

    @Nullable
    private static Int2ObjectMap<StaticModifier[]> modifiersOf(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() == null) return null;
        Item def = stack.getItem();
        ItemWeapon weapon = def.getWeapon();
        ItemArmor armor = def.getArmor();
        if (weapon != null) return weapon.getStatModifiers();
        if (armor != null) return armor.getStatModifiers();
        return null;
    }

    private static float sumAdditive(@Nullable StaticModifier[] mods) {
        if (mods == null) return 0f;
        float sum = 0f;
        for (StaticModifier m : mods) {
            if (m != null && m.getCalculationType() == StaticModifier.CalculationType.ADDITIVE) {
                sum += m.getAmount();
            }
        }
        return sum;
    }
}
