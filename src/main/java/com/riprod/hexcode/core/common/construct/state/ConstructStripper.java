package com.riprod.hexcode.core.common.construct.state;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.effect.HexEffectHandler;

// HexEffectHandler backed by a typed construct state. lets HexEffectRegistry
// counterspell any target-attached construct glyph with one line per registration.
public record ConstructStripper<S extends ConstructState>(
        @Nonnull String handlerId,
        @Nonnull Class<S> stateClass) implements HexEffectHandler {

    @Override
    public boolean isPresent(CommandBuffer<EntityStore> buffer, Ref<EntityStore> target) {
        return ConstructStateUtil.findState(buffer, target, handlerId, stateClass) != null;
    }

    @Override
    public void strip(CommandBuffer<EntityStore> buffer, Ref<EntityStore> target) {
        ConstructStateUtil.requestKillByHandlerId(buffer, target, handlerId);
    }
}
