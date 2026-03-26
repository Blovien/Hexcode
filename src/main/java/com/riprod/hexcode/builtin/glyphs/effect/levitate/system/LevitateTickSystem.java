package com.riprod.hexcode.builtin.glyphs.effect.levitate.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.levitate.component.LevitateComponent;

public class LevitateTickSystem extends EntityTickingSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String LEVITATE_EFFECT_ID = "Hexcode_Levitate";

    // upward acceleration to cancel hytale gravity (tune by testing)
    private static final double GRAVITY_CANCEL = 32.0;
    // multiplier for intensity -> additional upward speed
    private static final double LIFT_SCALE = 1.0;

    @Override
    public Query<EntityStore> getQuery() {
        return LevitateComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        LevitateComponent levitate = chunk.getComponent(index, LevitateComponent.getComponentType());
        if (levitate == null) return;

        Ref<EntityStore> ref = chunk.getReferenceTo(index);

        Velocity vel = chunk.getComponent(index, Velocity.getComponentType());
        if (vel != null) {
            double upwardForce;
            if (levitate.getIntensity() <= 0) {
                // weightless: exactly cancel gravity
                upwardForce = GRAVITY_CANCEL * dt;
            } else {
                // active lift: cancel gravity + additional upward drift
                upwardForce = (GRAVITY_CANCEL + levitate.getIntensity() * LIFT_SCALE) * dt;
            }
            vel.addInstruction(new Vector3d(0, upwardForce, 0), new VelocityConfig(), ChangeVelocityType.Add);
        }

        if (!levitate.tick(dt)) return;

        EffectControllerComponent controller = buffer.getComponent(
                ref, EffectControllerComponent.getComponentType());
        if (controller != null) {
            int effectIndex = EntityEffect.getAssetMap().getIndex(LEVITATE_EFFECT_ID);
            if (effectIndex != Integer.MIN_VALUE) {
                controller.removeEffect(ref, effectIndex, buffer);
            }
        }

        buffer.removeComponent(ref, LevitateComponent.getComponentType());
        LOGGER.atInfo().log("levitate: effect expired on entity");
    }
}
