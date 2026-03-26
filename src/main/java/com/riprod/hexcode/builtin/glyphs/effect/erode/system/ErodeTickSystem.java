package com.riprod.hexcode.builtin.glyphs.effect.erode.system;

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
import com.riprod.hexcode.builtin.glyphs.effect.erode.component.ErodeComponent;

public class ErodeTickSystem extends EntityTickingSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String ERODE_EFFECT_ID = "Hexcode_Erode";

    @Override
    public Query<EntityStore> getQuery() {
        return ErodeComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        ErodeComponent erode = chunk.getComponent(index, ErodeComponent.getComponentType());
        if (erode == null) return;

        if (!erode.tick(dt)) return;

        Ref<EntityStore> ref = chunk.getReferenceTo(index);

        EffectControllerComponent controller = buffer.getComponent(
                ref, EffectControllerComponent.getComponentType());
        if (controller != null) {
            int effectIndex = EntityEffect.getAssetMap().getIndex(ERODE_EFFECT_ID);
            if (effectIndex != Integer.MIN_VALUE) {
                controller.removeEffect(ref, effectIndex, buffer);
            }
        }

        buffer.removeComponent(ref, ErodeComponent.getComponentType());
        LOGGER.atInfo().log("erode: effect expired on entity");
    }
}
