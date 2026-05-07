package com.riprod.hexcode.core.common.triggers;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

@FunctionalInterface
public interface TriggerCallback {

    void onFire(@Nonnull CommandBuffer<EntityStore> buffer,
                @Nonnull TriggerSubscription subscription,
                @Nonnull TriggerEvent event);
}
