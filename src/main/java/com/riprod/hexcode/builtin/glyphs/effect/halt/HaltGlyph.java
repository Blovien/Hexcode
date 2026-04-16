package com.riprod.hexcode.builtin.glyphs.effect.halt;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.halt.component.HaltProjectileComponent;
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

    // cost scales exponentially past the knee duration
    private static final double VOLATILITY_KNEE = 4.0;
    private static final double VOLATILITY_STEEPNESS = 0.8;

    @Override
    public boolean resolveVolatility(Glyph glyph, HexContext hexContext) {
        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker == null) return true;

        double duration = SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot("duration", hexContext), DEFAULT_DURATION);

        float expScale = (float) Math.max(1.0, Math.exp(VOLATILITY_STEEPNESS * (duration - VOLATILITY_KNEE)));
        float cost = VolatilityTracker.computeGlyphCost(glyph) * expScale;
        return tracker.consumeVolatility(cost);
    }

    @Override
    public boolean resolveMana(Glyph glyph, HexContext hexContext) {
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return true;

        double duration = SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot("duration", hexContext), DEFAULT_DURATION);

        float baseCost = asset.getManaConsumption()
                * ((1 - glyph.getEfficiency()) * 0.25f + 0.75f);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        float castMultiplier = (tracker != null) ? tracker.getManaCostMultiplier() : 1.0f;
        float finalCost = (float) (baseCost * castMultiplier);

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
        HexVar targets = glyph.readSlot("target", hexContext);
        EntityVar entityVar = SpellVarUtil.resolveEntityVar(targets, hexContext);
        if (entityVar == null) {
            Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        CommandBuffer<EntityStore> accessor = hexContext.getAccessor();
        Ref<EntityStore> ref = entityVar.getRef(accessor);
        if (ref == null || !ref.isValid()) {
            Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
            return;
        }

        double duration = SpellVarUtil.resolveNumberOrDefault(
                glyph.readSlot("duration", hexContext), DEFAULT_DURATION);

        try {
            StandardPhysicsProvider physics = accessor.getComponent(ref,
                    StandardPhysicsProvider.getComponentType());
            if (physics != null) {
                physics.getForceProviderStandardState().nextTickVelocity.assign(Vector3d.ZERO);
                if (duration > 0) {
                    physics.setState(StandardPhysicsProvider.STATE.INACTIVE);
                    accessor.putComponent(ref, HaltProjectileComponent.getComponentType(),
                            new HaltProjectileComponent((float) duration, null));
                }
            } else {
                KnockbackComponent kb = new KnockbackComponent();
                kb.setVelocity(new Vector3d(0, 0, 0));
                kb.setVelocityType(ChangeVelocityType.Set);
                kb.setDuration((float) duration);
                accessor.putComponent(ref, KnockbackComponent.getComponentType(), kb);

                if (duration > 0) {
                    PhysicsValues original = EntityUtils.getPhysicsValues(ref, accessor);
                    PhysicsValues halted = new PhysicsValues(original.getMass(), 999.0, false);
                    accessor.putComponent(ref, PhysicsValues.getComponentType(), halted);
                    accessor.putComponent(ref, HaltProjectileComponent.getComponentType(),
                            new HaltProjectileComponent((float) duration, new PhysicsValues(original)));
                }
            }

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

        Executor.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
