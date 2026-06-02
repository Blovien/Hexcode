package com.riprod.hexcode.builtin.imbuement.dispatch;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.imbuement.dispatch.CastRootDispatcher;
import com.riprod.hexcode.core.common.triggers.component.TriggerEvent;
import com.riprod.hexcode.core.common.triggers.registry.Trigger;

// placeholder for ENTITY_SELF dispatch (e.g. trinket-style imbuements that read
// from a per-entity slot rather than a held/equipped item). currently no source
// fires; reserved so registering an ENTITY_SELF trigger doesn't NPE the
// dispatcher map lookup.
public final class EntitySelfCastDispatcher implements CastRootDispatcher {
    @Override
    public void dispatch(@Nonnull Trigger trigger, @Nonnull TriggerEvent event,
            @Nonnull CommandBuffer<EntityStore> buffer) {
    }
}
