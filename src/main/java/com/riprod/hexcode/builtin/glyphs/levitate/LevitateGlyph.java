package com.riprod.hexcode.builtin.glyphs.levitate;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.GlyphFizzleEvent;
import com.riprod.hexcode.builtin.glyphs.levitate.style.LevitateStyle;
import com.riprod.hexcode.core.common.construct.state.ConstructStateUtil;
import com.riprod.hexcode.core.common.construct.system.HexConstructSpawner;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.HexDirectionUtil;
import com.riprod.hexcode.utils.HexVarUtil;

public class LevitateGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public String getId() {
        return ID;
    };

    public static final String ID = "Levitate";

    private static final String LEVITATE_EFFECT_ID = "Hexcode_Levitate";
    private static final double DEFAULT_INTENSITY = 0.0;
    private static final double DEFAULT_DURATION = 100.0;
    private static final double MAX_INTENSITY = 10.0;
    private static final double MAX_DURATION = 600.0;
    // high drag for weightless mode — near-zero terminal velocity
    private static final double WEIGHTLESS_DRAG = 50.0;

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.readSlot(LevitateGlyphSlots.TARGET, hexContext);
        EntityVar entityVar = HexVarUtil.resolveEntityVar(targets, hexContext);
        if (entityVar == null) {
            if (HexVarUtil.resolveBlockVar(targets, hexContext) != null) {
                LOGGER.atInfo().log("levitate: block targets not yet implemented");
            }
            HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        double intensity = Math.max(0, Math.min(MAX_INTENSITY,
                HexVarUtil.numberOrDefault(
                        glyph.readSlot(LevitateGlyphSlots.INTENSITY, hexContext), DEFAULT_INTENSITY)));
        double duration = Math.max(1, Math.min(MAX_DURATION,
                HexVarUtil.numberOrDefault(
                        glyph.readSlot(LevitateGlyphSlots.DURATION, hexContext), DEFAULT_DURATION)));
        float durationSeconds = (float) duration;

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();
        applyToEntity(entityVar, (float) intensity, durationSeconds, glyph, hexContext, accessor);

        HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }

    private void applyToEntity(EntityVar entityVar, float intensity, float durationSeconds,
            Glyph glyph, HexContext hexContext, CommandBuffer<EntityStore> accessor) {
        Ref<EntityStore> ref = entityVar.getRef(accessor);
        if (ref == null || !ref.isValid()) {
            LOGGER.atWarning().log("Levitate: target ref unresolved");
            HexExecuter.fail(glyph, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED,
                    "Levitate: target ref unresolved");
            return;
        }

        PhysicsValues currentPhysics = EntityUtils.getPhysicsValues(ref, accessor);

        LevitateState existing = ConstructStateUtil.findState(
                accessor, ref, LevitateGlyph.ID, LevitateState.class);
        PhysicsValues originalCopy = existing != null && existing.getOriginalPhysicsValues() != null
                ? new PhysicsValues(existing.getOriginalPhysicsValues())
                : new PhysicsValues(currentPhysics);

        if (existing != null) {
            existing.setIntensity(intensity);
            existing.setRemainingDuration(durationSeconds);
            existing.setColors(hexContext.getColors());
        } else {
            LevitateState state = new LevitateState(intensity, durationSeconds,
                    hexContext.getColors(), originalCopy);
            HexConstructSpawner.applyWithState(
                    accessor, ref, hexContext, glyph, LevitateGlyph.ID, state);
        }

        double mass = currentPhysics.getMass();
        double originalDrag = originalCopy.getDragCoefficient();
        double drag = intensity <= 0 ? WEIGHTLESS_DRAG : originalDrag;
        PhysicsValues levitatePhysics = new PhysicsValues(mass, drag, true);
        accessor.putComponent(ref, PhysicsValues.getComponentType(), levitatePhysics);

        Velocity vel = accessor.getComponent(ref, Velocity.getComponentType());
        if (vel != null && vel.getY() < 0) {
            vel.addInstruction(new Vector3d(0, -vel.getY(), 0), null, ChangeVelocityType.Add);
        }

        EntityEffect levitateEffect = EntityEffect.getAssetMap().getAsset(LEVITATE_EFFECT_ID);
        if (levitateEffect != null) {
            EffectControllerComponent controller = accessor.getComponent(
                    ref, EffectControllerComponent.getComponentType());
            if (controller != null) {
                controller.addEffect(ref, levitateEffect, durationSeconds,
                        OverlapBehavior.OVERWRITE, accessor);
            }
        } else {
            LOGGER.atWarning().log("levitate: %s effect asset not found", LEVITATE_EFFECT_ID);
        }

        TransformComponent tc = accessor.getComponent(ref, TransformComponent.getComponentType());
        if (tc != null) {
            LevitateStyle.renderActivation(tc.getPosition(), hexContext.getColors(), accessor);
        }

        LOGGER.atInfo().log("levitate: applied intensity=%.1f for %.1fs to entity",
                intensity, durationSeconds);
    }
}
