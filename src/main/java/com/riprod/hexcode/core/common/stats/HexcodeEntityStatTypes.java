package com.riprod.hexcode.core.common.stats;

import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;

public abstract class HexcodeEntityStatTypes {
    private static int VOLATILITY = Integer.MIN_VALUE;

    public static int getVolatility() {
        return VOLATILITY;
    }

    public static void update() {
        VOLATILITY = EntityStatType.getAssetMap().getIndex("Volatility");
    }
}
