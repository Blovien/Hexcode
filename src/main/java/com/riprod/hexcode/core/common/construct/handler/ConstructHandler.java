package com.riprod.hexcode.core.common.construct.handler;

import com.riprod.hexcode.core.common.construct.component.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.component.HexStatus;
import com.riprod.hexcode.core.common.construct.state.ConstructState;

public interface ConstructHandler<S extends ConstructState> {

    default S createInitialState(HexStatus<S> status, ConstructTickContext ctx) {
        return null;
    }

    default void onFirstTick(HexStatus<S> status, ConstructTickContext ctx) {
    }

    default boolean onTick(float dt, HexStatus<S> status, ConstructTickContext ctx) {
        // default: consume one unit of volatility per second
        status.getHexContext().getVolatilityTracker().consumeVolatility(dt);
        return false;
    }

    default void onCleanup(HexStatus<S> status, ConstructTickContext ctx) {
    }
}
