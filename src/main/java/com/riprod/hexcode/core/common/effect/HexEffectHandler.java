package com.riprod.hexcode.core.common.effect;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public interface HexEffectHandler {

    boolean isPresent(CommandBuffer<EntityStore> buffer, Ref<EntityStore> target);

    void strip(CommandBuffer<EntityStore> buffer, Ref<EntityStore> target);
}
