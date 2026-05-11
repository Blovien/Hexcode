package com.riprod.hexcode.core.common.triggers.component;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.system.EcsEvent;

public class FireTriggerEvent extends EcsEvent {

    @Nonnull
    private final TriggerEvent triggerEvent;

    public FireTriggerEvent(@Nonnull TriggerEvent triggerEvent) {
        this.triggerEvent = triggerEvent;
    }

    @Nonnull
    public TriggerEvent getTriggerEvent() {
        return triggerEvent;
    }
}
