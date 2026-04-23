package com.riprod.hexcode.builtin.glyphs.domain;

import java.util.function.Consumer;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.SpellCastEvent;
import com.riprod.hexcode.builtin.glyphs.domain.component.DomainAuraComponent;

public class DomainSpellCastListener implements Consumer<SpellCastEvent> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public void accept(SpellCastEvent event) {
        Ref<EntityStore> casterRef = event.getCasterRef();
        if (casterRef == null || !casterRef.isValid()) return;

        Store<EntityStore> store = casterRef.getStore();
        if (store == null) return;

        DomainAuraComponent aura = store.getComponent(casterRef, DomainAuraComponent.getComponentType());
        if (aura == null) return;
        LOGGER.atInfo().log("domain cast: caster=%s aura=%s boost=%.2f",
                casterRef, aura != null,
                aura != null ? aura.getVolatilityBoost() : 1.0f);

        if (aura.getZoneRef() == null || !aura.getZoneRef().isValid()) return;

        event.setVolatilityMultiplier(aura.getVolatilityBoost());
    }
}
