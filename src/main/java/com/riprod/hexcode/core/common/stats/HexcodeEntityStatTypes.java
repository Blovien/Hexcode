package com.riprod.hexcode.core.common.stats;

import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;

public abstract class HexcodeEntityStatTypes {
    private static int VOLATILITY = Integer.MIN_VALUE;
    private static int MAGIC_POWER = Integer.MIN_VALUE;
    private static int MAGIC_CHARGES = Integer.MIN_VALUE;

    public static int getVolatility() {
        return VOLATILITY;
    }

    public static int getMagicPower() {
        return MAGIC_POWER;
    }

    public static int getMagicCharges() {
        return MAGIC_CHARGES;
    }

    public static void update() {
        VOLATILITY = EntityStatType.getAssetMap().getIndex("Volatility");
        MAGIC_POWER = EntityStatType.getAssetMap().getIndex("Magic_Power");
        MAGIC_CHARGES = EntityStatType.getAssetMap().getIndex("MagicCharges");
    }
}
