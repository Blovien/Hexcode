package com.riprod.hexcode.builtin.glyphs.fortify.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.fortify.component.FortifyComponent;

public class FortifyTickSystem extends EntityTickingSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String FORTIFY_EFFECT_ID = "Hexcode_Fortify";

    @Override
    public Query<EntityStore> getQuery() {
        return FortifyComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        try {
            FortifyComponent fortify = chunk.getComponent(index, FortifyComponent.getComponentType());
            if (fortify == null) return;

            boolean manaConsumed = true;

            // periodicall reduce mana based on fortify level
            if (fortify.shouldConsumeMana()) {
                float finalCost = fortify.getManaCost();
                manaConsumed = fortify.getHexContext().getRoot().tryConsumeMana(finalCost, buffer);
            }

            if (!fortify.tick(dt) && manaConsumed) return;

            Ref<EntityStore> ref = chunk.getReferenceTo(index);

            EffectControllerComponent controller = buffer.getComponent(
                    ref, EffectControllerComponent.getComponentType());
            if (controller != null) {
                int effectIndex = EntityEffect.getAssetMap().getIndex(FORTIFY_EFFECT_ID);
                if (effectIndex != Integer.MIN_VALUE) {
                    controller.removeEffect(ref, effectIndex, buffer);
                }
            }

            

            buffer.removeComponent(ref, FortifyComponent.getComponentType());
            LOGGER.atInfo().log("fortify: effect expired on entity");
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] FortifyTickSystem failed: %s", e.getMessage());
        }
    }
}
