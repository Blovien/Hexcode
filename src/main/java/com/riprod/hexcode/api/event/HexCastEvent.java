package com.riprod.hexcode.api.event;

import com.hypixel.hytale.component.system.CancellableEcsEvent;
import com.riprod.hexcode.core.state.execution.component.HexContext;

// emitted while a hex is being cast, prior to any mana being consumed or effects being applied.
// cancellable; any changes to the context (style, default variable, mana cost, etc.) are applied
// to the cast if not cancelled. caster identity comes from context.getHexRoot();
// default-variable target comes from context.getDefaultVariable() (or HexRoot.getRootVar
// fallback in HexExecutor when unset).
public class HexCastEvent extends CancellableEcsEvent {

    private final HexContext context;

    public HexCastEvent(HexContext context) {
        this.context = context;
    }

    public HexContext getContext() {
        return context;
    }
}
