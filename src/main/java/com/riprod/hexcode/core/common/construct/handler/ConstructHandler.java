package com.riprod.hexcode.core.common.construct.handler;

import com.riprod.hexcode.core.common.construct.component.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.component.HexStatus;

public interface ConstructHandler {

    default boolean onTick(float dt, HexStatus statusEffect, ConstructTickContext ctx) {
        // By default, consume one unit of volatility per second
        statusEffect.getHexContext().getVolatilityTracker().consumeVolatility(dt);
        return false;
    }

    default void onFirstTick(HexStatus construct, ConstructTickContext ctx) {
    }

    default void onCleanup(HexStatus construct, ConstructTickContext ctx) {
    }
}
