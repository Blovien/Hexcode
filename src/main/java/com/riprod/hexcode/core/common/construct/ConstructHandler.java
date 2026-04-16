package com.riprod.hexcode.core.common.construct;

import com.riprod.hexcode.core.common.construct.component.HexConstruct;

public interface ConstructHandler {

    boolean onTick(float dt, HexConstruct construct, ConstructTickContext ctx);

    default void onFirstTick(HexConstruct construct, ConstructTickContext ctx) {
    }

    default void onCleanup(HexConstruct construct, ConstructTickContext ctx) {
    }
}
