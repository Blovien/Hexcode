package com.riprod.hexcode.core.common.triggers;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record TriggerEvent(@Nonnull String key,
                           @Nullable UUID subjectUuid,
                           @Nullable Ref<EntityStore> subjectRef,
                           @Nonnull TriggerPayload payload) {
}
