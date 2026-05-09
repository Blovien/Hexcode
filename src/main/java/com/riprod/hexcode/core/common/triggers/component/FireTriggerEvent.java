package com.riprod.hexcode.core.common.triggers.component;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.system.EcsEvent;

// proxy event so non-buffered sources (global PlayerEvents, command threads, etc.)
// can dispatch into the trigger bus by invoking through Store. FireTriggerSystem
// catches it on the world tick and forwards to TriggerListenerRegistry.fire with
// the buffer.
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
