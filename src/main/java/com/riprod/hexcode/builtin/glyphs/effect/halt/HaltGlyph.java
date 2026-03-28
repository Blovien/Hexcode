package com.riprod.hexcode.builtin.glyphs.effect.halt;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.halt.style.HaltStyle;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.utils.SpellVarUtil;

public class HaltGlyph implements GlyphHandler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String ID = "Glyph_Halt";

    private static final String HALT_EFFECT_ID = "Hexcode_Halt";
    private static final double DEFAULT_DURATION = 0.0;

    // mana: cost(t) = base * (1 + e^(k_mana * (t - t_mana)))
    // at t=0 cost ≈ base, ramps steeply around t_mana
    private static final double MANA_KNEE = 2.0;
    private static final double MANA_STEEPNESS = 1.5;

    // volatility: extra rolls = floor(e^(k_vol * (t - t_vol)))
    // near-zero below t_vol, climbs fast above it
    private static final double VOLATILITY_KNEE = 4.0;
    private static final double VOLATILITY_STEEPNESS = 0.8;

    @Override
    public boolean resolveVolatility(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null) return true;

        double duration = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("duration", hexContext), DEFAULT_DURATION);

        int extraRolls = (int) Math.floor(Math.exp(VOLATILITY_STEEPNESS * (duration - VOLATILITY_KNEE)));

        if (!tracker.rollAndIncrement(glyph)) {
            LOGGER.atInfo().log("halt: fizzled on primary volatility roll");
            return false;
        }

        for (int i = 0; i < extraRolls; i++) {
            if (!tracker.rollAndIncrement(glyph)) {
                LOGGER.atInfo().log("halt: fizzled on extra volatility roll %d/%d", i + 1, extraRolls);
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean resolveMana(Glyph glyph, HexContext hexContext) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return true;

        double duration = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("duration", hexContext), DEFAULT_DURATION);

        float baseCost = asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        double durationMultiplier = 1.0 + Math.exp(MANA_STEEPNESS * (duration - MANA_KNEE));

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;
        float finalCost = (float) (baseCost * castMultiplier * durationMultiplier);

        boolean consumed = hexContext.getRoot().tryConsumeMana(finalCost, hexContext.getAccessor());
        if (!consumed) {
            float currentMana = hexContext.getRoot().getCurrentMana(hexContext.getAccessor());
            LOGGER.atInfo().log("halt: needs %.1f mana (duration=%.1f), has %.1f",
                    finalCost, duration, currentMana);
        }
        return consumed;
    }

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        HexVar targets = glyph.resolveInput("target", hexContext);
        if (!(targets instanceof EntityVar entityVar)) {
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();
        Ref<EntityStore> ref = entityVar.getRef(accessor);
        if (ref == null || !ref.isValid()) {
            Executor.continueExecution(glyph.getNext(), hexContext);
            return;
        }

        double duration = SpellVarUtil.resolveNumberOrDefault(
                glyph.resolveInput("duration", hexContext), DEFAULT_DURATION);

        try {
            KnockbackComponent kb = new KnockbackComponent();
            kb.setVelocity(new Vector3d(0, 0, 0));
            kb.setVelocityType(ChangeVelocityType.Set);
            kb.setDuration(0.0f);
            accessor.putComponent(ref, KnockbackComponent.getComponentType(), kb);

            if (duration > 0) {
                EntityEffect haltEffect = EntityEffect.getAssetMap().getAsset(HALT_EFFECT_ID);
                if (haltEffect != null) {
                    EffectControllerComponent controller = accessor.getComponent(
                            ref, EffectControllerComponent.getComponentType());
                    if (controller != null) {
                        controller.addEffect(ref, haltEffect, (float) duration,
                                OverlapBehavior.OVERWRITE, accessor);
                    }
                } else {
                    LOGGER.atWarning().log("halt: %s effect asset not found", HALT_EFFECT_ID);
                }
            }

            TransformComponent tc = accessor.getComponent(ref, TransformComponent.getComponentType());
            if (tc != null) {
                HaltStyle.render(tc.getPosition(), hexContext.getColors(), accessor);
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("halt: could not halt entity: %s", e.getMessage());
        }

        Executor.continueExecution(glyph.getNext(), hexContext);
    }
}
