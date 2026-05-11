package com.riprod.hexcode.api.event;

import com.hypixel.hytale.component.system.CancellableEcsEvent;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class HexCastEvent extends CancellableEcsEvent {

    private final HexContext context;

    public HexCastEvent(HexContext context) {
        this.context = context;
    }

    public HexContext getContext() {
        return context;
    }
}
