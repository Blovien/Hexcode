package com.riprod.hexcode.builtin.glyphs.halt;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.construct.component.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.component.HexStatus;
import com.riprod.hexcode.core.common.construct.handler.ConstructHandler;

public class HaltConstructHandler implements ConstructHandler<HaltState> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String HALT_EFFECT_ID = "Hexcode_Halt";

    @Override
    public boolean onTick(float dt, HexStatus<HaltState> status, ConstructTickContext ctx) {
        HaltState state = status.getState();
        if (state == null) return true;
        if (state.isExpired()) return true;
        state.tick(dt);
        status.getHexContext().getVolatilityTracker().consumeVolatility(dt);
        return false;
    }

    @Override
    public void onCleanup(HexStatus<HaltState> status, ConstructTickContext ctx) {
        HaltState state = status.getState();
        CommandBuffer<EntityStore> buffer = ctx.getBuffer();
        Ref<EntityStore> target = ctx.getEntityRef();
        if (target == null || !target.isValid()) return;

        StandardPhysicsProvider physics = buffer.getComponent(
                target, StandardPhysicsProvider.getComponentType());
        if (physics != null) {
            physics.setState(StandardPhysicsProvider.STATE.ACTIVE);
        }

        if (state != null && state.getOriginalPhysicsValues() != null) {
            buffer.putComponent(target, PhysicsValues.getComponentType(),
                    new PhysicsValues(state.getOriginalPhysicsValues()));
        }

        EffectControllerComponent controller = buffer.getComponent(
                target, EffectControllerComponent.getComponentType());
        if (controller != null) {
            int effectIndex = EntityEffect.getAssetMap().getIndex(HALT_EFFECT_ID);
            if (effectIndex != Integer.MIN_VALUE) {
                controller.removeEffect(target, effectIndex, buffer);
            }
        }

        LOGGER.atInfo().log("halt: cleaned up, physics restored");
    }
}
