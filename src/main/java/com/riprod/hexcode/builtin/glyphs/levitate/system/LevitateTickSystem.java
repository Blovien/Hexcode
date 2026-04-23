package com.riprod.hexcode.builtin.glyphs.levitate.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.levitate.component.LevitateComponent;

public class LevitateTickSystem extends EntityTickingSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String LEVITATE_EFFECT_ID = "Hexcode_Levitate";

    @Override
    public Query<EntityStore> getQuery() {
        return LevitateComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        try {
            LevitateComponent levitate = chunk.getComponent(index, LevitateComponent.getComponentType());
            if (levitate == null) return;

            if (!levitate.tick(dt)) return;

            Ref<EntityStore> ref = chunk.getReferenceTo(index);

            // restore original physics values
            PhysicsValues original = levitate.getOriginalPhysicsValues();
            if (original != null) {
                buffer.putComponent(ref, PhysicsValues.getComponentType(), new PhysicsValues(original));
            }

            EffectControllerComponent controller = buffer.getComponent(
                    ref, EffectControllerComponent.getComponentType());
            if (controller != null) {
                int effectIndex = EntityEffect.getAssetMap().getIndex(LEVITATE_EFFECT_ID);
                if (effectIndex != Integer.MIN_VALUE) {
                    controller.removeEffect(ref, effectIndex, buffer);
                }
            }

            buffer.removeComponent(ref, LevitateComponent.getComponentType());
            LOGGER.atInfo().log("levitate: effect expired, restored physics");
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] LevitateTickSystem failed: %s", e.getMessage());
        }
    }
}
