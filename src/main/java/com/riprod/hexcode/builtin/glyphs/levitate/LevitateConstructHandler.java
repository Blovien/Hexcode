package com.riprod.hexcode.builtin.glyphs.levitate;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.levitate.style.LevitateStyle;
import com.riprod.hexcode.core.common.construct.component.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.component.HexStatus;
import com.riprod.hexcode.core.common.construct.handler.ConstructHandler;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.state.execution.HexExecuter;

public class LevitateConstructHandler implements ConstructHandler<LevitateState> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String LEVITATE_EFFECT_ID = "Hexcode_Levitate";
    private static final float TICK_INTERVAL = 0.5f;

    @Override
    public boolean onTick(float dt, HexStatus<LevitateState> status, ConstructTickContext ctx) {
        LevitateState state = status.getState();
        if (state == null)
            return true;
        if (state.isExpired())
            return true;
        state.tick(dt);

        emitTickVfx(dt, state, status, ctx);

        return !drainSustain(dt, status);
    }

    private void emitTickVfx(float dt, LevitateState state, HexStatus<LevitateState> status,
            ConstructTickContext ctx) {
        Ref<EntityStore> target = ctx.getEntityRef();
        if (target == null || !target.isValid())
            return;

        float accum = state.getTickAccum() + dt;
        while (accum >= TICK_INTERVAL) {
            accum -= TICK_INTERVAL;
            TransformComponent transform = ctx.getBuffer().getComponent(
                    target, TransformComponent.getComponentType());
            if (transform == null)
                break;
            LevitateStyle.renderTick(transform.getPosition(), status.getHexContext(), ctx.getBuffer());
        }
        state.setTickAccum(accum);
    }

    @Override
    public void onCleanup(HexStatus<LevitateState> status, ConstructTickContext ctx) {
        LevitateState state = status.getState();
        CommandBuffer<EntityStore> buffer = ctx.getBuffer();
        Ref<EntityStore> target = ctx.getEntityRef();
        if (target == null || !target.isValid())
            return;

        if (state != null && state.getOriginalPhysicsValues() != null) {
            buffer.putComponent(target, PhysicsValues.getComponentType(),
                    new PhysicsValues(state.getOriginalPhysicsValues()));
        }

        EffectControllerComponent controller = buffer.getComponent(
                target, EffectControllerComponent.getComponentType());
        if (controller != null) {
            int effectIndex = EntityEffect.getAssetMap().getIndex(LEVITATE_EFFECT_ID);
            if (effectIndex != Integer.MIN_VALUE) {
                controller.removeEffect(target, effectIndex, buffer);
            }
        }

        HexExecuter.continueFromSlot(status.getTriggeringGlyph(), Glyph.NEXT_SLOT, status.getHexContext());

        LOGGER.atInfo().log("levitate: cleaned up, physics restored");
    }
}
