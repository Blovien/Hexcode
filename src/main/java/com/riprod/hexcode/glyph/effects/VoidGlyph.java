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
 * Void effect glyph - deals void damage and applies brief blindness.
 */
public class VoidGlyph extends EffectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "hexcode:void";
    public static final int BASE_COST = 15;
    public static final float BASE_DAMAGE = 11.0f;
    public static final float BLIND_DURATION = 2.0f;
    public static final String BLIND_EFFECT_ID = "hexcode:blindness";

    public VoidGlyph() {
        super(
            ID,
            "Void",
            BASE_COST,
            GlyphVisual.effect(GlyphVisual.COLOR_VOID, "void"),
            Set.of("hexcode:power", "hexcode:duration")
        );
    }

    @Override
    public void applyEffect(ExecutionContext ctx, TargetSet targets) {
        float damage = getModifiedAmount(ctx, BASE_DAMAGE);
        float blindDuration = getModifiedDuration(ctx, BLIND_DURATION);

        Store<EntityStore> store = ctx.getStore();
        Ref<EntityStore> caster = ctx.getCaster();

        LOGGER.atInfo().log("Applying void effect: %.1f damage, %.1f blindness duration to %d targets",
                damage, blindDuration, targets.getEntityCount());

        // Apply to each target entity
        for (Ref<EntityStore> targetRef : targets.getEntities()) {
            // Skip self-damage
            if (targetRef.equals(caster)) {
                continue;
            }

            // Apply instant void damage
            int damageCauseIndex = DamageCause.getAssetMap().getIndex("void");
            Damage voidDamage = new Damage(
                    new Damage.EntitySource(caster),
                    damageCauseIndex,
                    damage
            );
            DamageSystems.executeDamage(targetRef, store, voidDamage);

            // Apply blindness effect
            applyBlindnessEffect(targetRef, store, blindDuration);

            LOGGER.atInfo().log("Applied void damage and blindness to target");
        }
    }

    /**
     * Apply the blindness status effect to a target.
     */
    private void applyBlindnessEffect(Ref<EntityStore> targetRef, Store<EntityStore> store, float duration) {
        EffectControllerComponent effectController = store.getComponent(
                targetRef,
                EffectControllerComponent.getComponentType()
        );

        if (effectController == null) {
            LOGGER.atWarning().log("Target has no EffectControllerComponent, cannot apply blindness");
            return;
        }

        // Try to get the blindness effect asset
        EntityEffect blindEffect = EntityEffect.getAssetMap().getAsset(BLIND_EFFECT_ID);
        if (blindEffect == null) {
            // Fallback to generic blindness effect
            blindEffect = EntityEffect.getAssetMap().getAsset("blindness");
            if (blindEffect == null) {
                LOGGER.atWarning().log("No blindness effect found, skipping effect application");
                return;
            }
        }

        // Get the effect index from the asset map
        int effectIndex = EntityEffect.getAssetMap().getIndex(blindEffect.getId());

        // Apply the blindness effect with duration
        effectController.addEffect(targetRef, effectIndex, blindEffect, duration, OverlapBehavior.EXTEND, store);
        LOGGER.atInfo().log("Applied blindness effect for %.1f seconds", duration);
    }
}
