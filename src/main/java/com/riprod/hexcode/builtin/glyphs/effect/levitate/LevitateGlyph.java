package com.riprod.hexcode.builtin.glyphs.effect.levitate;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.levitate.component.LevitateComponent;
import com.riprod.hexcode.builtin.glyphs.effect.levitate.style.LevitateStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.SpellVarUtil;

public class LevitateGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Levitate";

    private static final String LEVITATE_EFFECT_ID = "Hexcode_Levitate";
    private static final double DEFAULT_INTENSITY = 0.0;
    private static final double DEFAULT_DURATION = 100.0;
    private static final double MAX_INTENSITY = 10.0;
    private static final double MAX_DURATION = 600.0;
    // high drag for weightless mode — near-zero terminal velocity
    private static final double WEIGHTLESS_DRAG = 50.0;

    @Override
    public boolean resolveMana(Glyph glyph, HexContext hexContext) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return true;

        double intensity = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("intensity", hexContext), DEFAULT_INTENSITY);
        double duration = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("duration", hexContext), DEFAULT_DURATION);

        float baseCost = asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;

        float intensityScale = (float) Math.max(1.0, intensity / Math.max(1.0, DEFAULT_INTENSITY));
        float durationScale = (float) (duration / DEFAULT_DURATION);
        float finalCost = baseCost * castMultiplier * intensityScale * durationScale;

        boolean consumed = hexContext.getRoot().tryConsumeMana(finalCost, hexContext.getAccessor());
        if (!consumed) {
            LOGGER.atInfo().log("levitate: insufficient mana, need %.1f", finalCost);
        }
        return consumed;
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.resolveInput("target", hexContext);
        if (!(targets instanceof EntityVar entityVar)) {
            if (targets instanceof BlockVar) {
                LOGGER.atInfo().log("levitate: block targets not yet implemented");
            }
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        double intensity = Math.max(0, Math.min(MAX_INTENSITY,
                SpellVarUtil.resolveNumberOrDefault(
                        glyph.resolveInput("intensity", hexContext), DEFAULT_INTENSITY)));
        double duration = Math.max(1, Math.min(MAX_DURATION,
                SpellVarUtil.resolveNumberOrDefault(
                        glyph.resolveInput("duration", hexContext), DEFAULT_DURATION)));
        float durationSeconds = (float) duration;

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();
        applyToEntity(entityVar, (float) intensity, durationSeconds, hexContext, accessor);

        Executor.continueExecution(glyph.getNext(), hexContext);
    }

    private void applyToEntity(EntityVar entityVar, float intensity,
            float durationSeconds, HexContext hexContext, CommandBuffer<EntityStore> accessor) {
        Ref<EntityStore> ref = entityVar.getRef(accessor);
        if (ref == null || !ref.isValid()) return;

        PhysicsValues currentPhysics = accessor.getComponent(ref, PhysicsValues.getComponentType());
        PhysicsValues originalCopy = currentPhysics != null
                ? new PhysicsValues(currentPhysics) : null;

        LevitateComponent existing = accessor.getComponent(ref, LevitateComponent.getComponentType());
        if (existing != null) {
            existing.setIntensity(intensity);
            existing.setRemainingDuration(durationSeconds);
            existing.setColors(hexContext.getColors());
        } else {
            accessor.addComponent(ref, LevitateComponent.getComponentType(),
                    new LevitateComponent(intensity, durationSeconds,
                            hexContext.getColors(), originalCopy));
        }

        if (currentPhysics != null) {
            double mass = currentPhysics.getMass();
            double drag = intensity <= 0 ? WEIGHTLESS_DRAG : currentPhysics.getDragCoefficient();
            PhysicsValues levitatePhysics = new PhysicsValues(mass, drag, true);
            accessor.putComponent(ref, PhysicsValues.getComponentType(), levitatePhysics);
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
