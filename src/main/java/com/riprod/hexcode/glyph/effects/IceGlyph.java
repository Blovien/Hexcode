package com.riprod.hexcode.glyph.effects;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.GlyphVisual;

import java.util.Set;

/**
 * Ice effect glyph - deals cold damage and applies slow.
 */
public class IceGlyph extends EffectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "hexcode:ice";
    public static final int BASE_COST = 15;
    public static final float BASE_DAMAGE = 8.0f;
    public static final float SLOW_DURATION = 4.0f;
    public static final float SLOW_AMOUNT = 0.5f; // 50% speed reduction
    public static final String SLOW_EFFECT_ID = "hexcode:slow";

    public IceGlyph() {
        super(
            ID,
            "Ice",
            BASE_COST,
            GlyphVisual.effect(GlyphVisual.COLOR_ICE),
            Set.of("hexcode:power", "hexcode:duration")
        );
    }

    @Override
    public void applyEffect(ExecutionContext ctx, TargetSet targets) {
        float damage = getModifiedAmount(ctx, BASE_DAMAGE);
        float slowDuration = getModifiedDuration(ctx, SLOW_DURATION);

        Store<EntityStore> store = ctx.getStore();
        Ref<EntityStore> caster = ctx.getCaster();

        LOGGER.atInfo().log("Applying ice effect: %.1f damage, %.1f slow duration to %d targets",
                damage, slowDuration, targets.getEntityCount());

        // Apply to each target entity
        for (Ref<EntityStore> targetRef : targets.getEntities()) {
            // Skip self-damage unless targeting self
            if (targetRef.equals(caster)) {
                continue;
            }

            // Apply instant cold damage
            int damageCauseIndex = DamageCause.getAssetMap().getIndex("cold");
            Damage iceDamage = new Damage(
                    new Damage.EntitySource(caster),
                    damageCauseIndex,
                    damage
            );
            DamageSystems.executeDamage(targetRef, store, iceDamage);

            // Apply slow effect
            applySlowEffect(targetRef, store, slowDuration);

            LOGGER.atInfo().log("Applied ice damage and slow to target");
        }
    }

    /**
     * Apply the slow status effect to a target.
     */
    private void applySlowEffect(Ref<EntityStore> targetRef, Store<EntityStore> store, float duration) {
        EffectControllerComponent effectController = store.getComponent(
                targetRef,
                EffectControllerComponent.getComponentType()
        );

        if (effectController == null) {
            LOGGER.atWarning().log("Target has no EffectControllerComponent, cannot apply slow");
            return;
        }

        // Try to get the slow effect asset
        EntityEffect slowEffect = EntityEffect.getAssetMap().getAsset(SLOW_EFFECT_ID);
        if (slowEffect == null) {
            // Fallback to generic slow effect
            slowEffect = EntityEffect.getAssetMap().getAsset("slow");
            if (slowEffect == null) {
                LOGGER.atWarning().log("No slow effect found, skipping slow application");
                return;
            }
        }

        // Get the effect index from the asset map
        int effectIndex = EntityEffect.getAssetMap().getIndex(slowEffect.getId());

        // Apply the slow effect with duration
        effectController.addEffect(targetRef, effectIndex, slowEffect, duration, OverlapBehavior.EXTEND, store);
        LOGGER.atInfo().log("Applied slow effect for %.1f seconds", duration);
    }
}
