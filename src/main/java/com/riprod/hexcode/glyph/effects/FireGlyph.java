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
 * Fire effect glyph - deals fire damage and applies burn DOT.
 */
public class FireGlyph extends EffectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "hexcode:fire";
    public static final int BASE_COST = 15;
    public static final float BASE_DAMAGE = 10.0f;
    public static final float BURN_DURATION = 3.0f;
    public static final float BURN_DAMAGE_PER_SECOND = 2.0f;
    public static final String BURN_EFFECT_ID = "hexcode:burn";

    public FireGlyph() {
        super(
            ID,
            "Fire",
            BASE_COST,
            GlyphVisual.effect(GlyphVisual.COLOR_FIRE, "fire"),
            Set.of("hexcode:power", "hexcode:duration")
        );
    }

    @Override
    public void applyEffect(ExecutionContext ctx, TargetSet targets) {
        float damage = getModifiedAmount(ctx, BASE_DAMAGE);
        float burnDuration = getModifiedDuration(ctx, BURN_DURATION);

        Store<EntityStore> store = ctx.getStore();
        Ref<EntityStore> caster = ctx.getCaster();

        LOGGER.atInfo().log("Applying fire effect: %.1f damage, %.1f burn duration to %d targets",
                damage, burnDuration, targets.getEntityCount());

        // Apply to each target entity
        for (Ref<EntityStore> targetRef : targets.getEntities()) {
            // Skip self-damage unless targeting self
            if (targetRef.equals(caster)) {
                continue;
            }

            

            // Apply instant fire damage
            int damageCauseIndex = DamageCause.getAssetMap().getIndex("fire");
            Damage fireDamage = new Damage(
                    new Damage.EntitySource(caster),
                    damageCauseIndex,
                    damage
            );
            DamageSystems.executeDamage(targetRef, store, fireDamage);

            // Apply burn DOT effect
            applyBurnEffect(targetRef, store, burnDuration);

            LOGGER.atInfo().log("Applied fire damage to target");
        }
    }

    /**
     * Apply the burn status effect to a target.
     */
    private void applyBurnEffect(Ref<EntityStore> targetRef, Store<EntityStore> store, float duration) {
        EffectControllerComponent effectController = store.getComponent(
                targetRef,
                EffectControllerComponent.getComponentType()
        );

        if (effectController == null) {
            LOGGER.atWarning().log("Target has no EffectControllerComponent, cannot apply burn");
            return;
        }

        // Try to get the burn effect asset
        EntityEffect burnEffect = EntityEffect.getAssetMap().getAsset(BURN_EFFECT_ID);
        if (burnEffect == null) {
            // Fallback to generic fire effect if custom burn doesn't exist
            burnEffect = EntityEffect.getAssetMap().getAsset("fire");
            if (burnEffect == null) {
                LOGGER.atWarning().log("No burn effect found, skipping DOT application");
                return;
            }
        }

        // Get the effect index from the asset map
        int effectIndex = EntityEffect.getAssetMap().getIndex(burnEffect.getId());

        // Apply the burn effect with duration
        effectController.addEffect(targetRef, effectIndex, burnEffect, duration, OverlapBehavior.EXTEND, store);
        LOGGER.atInfo().log("Applied burn effect for %.1f seconds", duration);
    }
}
