package com.riprod.hexcode.core.common.imbuement.block;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockBreakingDropType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockGathering;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemToolSpec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class BlockImbuementCapacity {

    public static final int MIN_TIER = 1;
    public static final int MAX_TIER = 5;

    private static final float DEFAULT_POWER = 1.0f;
    private static final float VOL_COEFFICIENT = 6.0f;
    private static final float RECHARGE_COEFFICIENT_TICKS = 1200.0f;

    private BlockImbuementCapacity() {
    }

    @Nonnull
    public static Capacity forBlock(@Nonnull BlockType blockType) {
        float power = resolvePower(blockType);
        int tier = clampTier(Math.round((float) (Math.log(1f / power) / Math.log(2)) / 2f) + 1);
        int slots = tier;
        int volPerSlot = Math.round(VOL_COEFFICIENT * (float) Math.pow(tier, 1.5));
        int rechargeTicks = Math.round(RECHARGE_COEFFICIENT_TICKS * (float) Math.pow(tier, 1.5));
        return new Capacity(tier, slots, volPerSlot, rechargeTicks);
    }

    private static float resolvePower(@Nonnull BlockType blockType) {
        BlockGathering gathering = blockType.getGathering();
        if (gathering == null) return DEFAULT_POWER;
        BlockBreakingDropType breaking = gathering.getBreaking();
        if (breaking == null) return DEFAULT_POWER;
        String gatherType = breaking.getGatherType();
        if (gatherType == null || gatherType.isEmpty()) return DEFAULT_POWER;
        ItemToolSpec spec = ItemToolSpec.getAssetMap().getAsset(gatherType);
        if (spec == null) return DEFAULT_POWER;
        float power = spec.getPower();
        if (power <= 0f) return DEFAULT_POWER;
        return power;
    }

    private static int clampTier(int tier) {
        if (tier < MIN_TIER) return MIN_TIER;
        if (tier > MAX_TIER) return MAX_TIER;
        return tier;
    }

    public static final class Capacity {
        private final int tier;
        private final int slots;
        private final int volatilityPerSlot;
        private final int rechargeIntervalTicks;

        Capacity(int tier, int slots, int volatilityPerSlot, int rechargeIntervalTicks) {
            this.tier = tier;
            this.slots = slots;
            this.volatilityPerSlot = volatilityPerSlot;
            this.rechargeIntervalTicks = rechargeIntervalTicks;
        }

        public int getTier() { return tier; }
        public int getSlots() { return slots; }
        public int getVolatilityPerSlot() { return volatilityPerSlot; }
        public int getRechargeIntervalTicks() { return rechargeIntervalTicks; }

        @Override
        public String toString() {
            return "Capacity{tier=" + tier
                    + ", slots=" + slots
                    + ", volPerSlot=" + volatilityPerSlot
                    + ", rechargeTicks=" + rechargeIntervalTicks + "}";
        }
    }

    @Nullable
    public static Capacity tryFor(@Nullable BlockType blockType) {
        if (blockType == null) return null;
        return forBlock(blockType);
    }
}
