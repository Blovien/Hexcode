package com.riprod.hexcode.api.event;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.system.CancellableEcsEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.state.execution.events.CastingEventData;

/**
 * Emitted while a hex is being cast, prior to any mana being consumed or effects being applied. This is cancellable, and any changes to the event's data will be applied to the cast if not cancelled.
 */
public class HexCastEvent extends CancellableEcsEvent {

    private final CastingEventData castingData;

    public HexCastEvent(Ref<EntityStore> targetRef, CastingEventData castingData) {
        castingData.hydrate(targetRef, castingData.getHexRoot());
        this.castingData = castingData;
    }

    public Ref<EntityStore> getWielderRef() {
        return castingData.getHexRoot().getSourceRef();
    }

    public Ref<EntityStore> getTargetRef() {
        return castingData.getTargetRef();
    }

    public Hex getHex() {
        return castingData.getHex();
    }

    public CastingEventData getCastingData() {
        return castingData;
    }
}
