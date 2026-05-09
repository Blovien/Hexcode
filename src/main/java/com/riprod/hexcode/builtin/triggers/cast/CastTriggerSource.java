package com.riprod.hexcode.builtin.triggers.cast;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.WorldEventSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.HexCastEvent;
import com.riprod.hexcode.builtin.triggers.TriggerKey;
import com.riprod.hexcode.core.common.triggers.component.TriggerEvent;
import com.riprod.hexcode.core.common.triggers.registry.TriggerListenerRegistry;

// fires ON_CAST whenever a HexCastEvent is invoked. subject = caster.
// re-entry safe: the trigger glyph spawns a sustain construct and registers
// via AbstractTriggerGlyph before this source observes the event, so a cast
// that contains an OnCast glyph won't recursively fire on its own creation.
// the one-shot nature of the subscription prevents infinite chains in practice.
public class CastTriggerSource extends WorldEventSystem<EntityStore, HexCastEvent> {

    public CastTriggerSource() {
        super(HexCastEvent.class);
    }

    @Override
    public void handle(@Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> buffer,
                       @Nonnull HexCastEvent event) {
        if (event.isCancelled()) return;
        TriggerListenerRegistry registry = buffer.getResource(TriggerListenerRegistry.getResourceType());
        if (registry == null || registry.countListeners(TriggerKey.CAST) == 0) return;

        Ref<EntityStore> caster = event.getWielderRef();
        if (caster == null || !caster.isValid()) return;
        UUIDComponent uuidComp = buffer.getComponent(caster, UUIDComponent.getComponentType());
        if (uuidComp == null) return;

        CastPayload payload = new CastPayload(caster);
        registry.fire(buffer, new TriggerEvent(TriggerKey.CAST, uuidComp.getUuid(), caster, payload));
    }
}
