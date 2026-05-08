package com.riprod.hexcode.core.common.triggers.component;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.triggers.handler.TriggerCallback;

public record TriggerSubscription(@Nonnull UUID id,
                                   @Nonnull String key,
                                   @Nullable UUID subjectUuid,
                                   @Nullable Ref<EntityStore> subjectRef,
                                   @Nullable Ref<EntityStore> ownerRef,
                                   @Nullable ItemStack sourceItem,
                                   @Nonnull TriggerCallback callback,
                                   boolean oneShot) {

    public static TriggerSubscription glyph(@Nonnull String key,
                                            @Nonnull UUID subjectUuid,
                                            @Nonnull Ref<EntityStore> subjectRef,
                                            @Nonnull Ref<EntityStore> ownerRef,
                                            @Nonnull TriggerCallback callback) {
        return new TriggerSubscription(UUID.randomUUID(), key, subjectUuid, subjectRef, ownerRef, null, callback, true);
    }

    public static TriggerSubscription bootstrap(@Nonnull String key,
                                                @Nonnull TriggerCallback callback) {
        return new TriggerSubscription(UUID.randomUUID(), key, null, null, null, null, callback, false);
    }
}
