package com.riprod.hexcode.glyph.effects;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.execution.SpellContext;
import com.riprod.hexcode.glyph.GlyphVisual;

/**
 * Ice effect glyph - deals cold damage and applies slow.
 *
 * <p>Asset-driven properties:
 * <ul>
 *   <li>baseDamage - base cold damage amount (default: 8.0)</li>
 *   <li>slowDuration - duration of slow effect in seconds (default: 4.0)</li>
 *   <li>slowAmount - movement speed reduction (default: 0.5 = 50%)</li>
 *   <li>slowEffectId - ID of the slow effect to apply (default: "hexcode:slow")</li>
 *   <li>damageType - damage type ID (default: "cold")</li>
 * </ul>
 */
public class IceGlyph extends EffectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Create an ice glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing glyph properties
     */
    public IceGlyph(GlyphAssetDefinition assetDefinition) {
        super(assetDefinition, GlyphVisual.effect(GlyphVisual.COLOR_ICE, "ice"));
    }

    @Override
    protected void applyEffect(SpellContext context, Ref<EntityStore> target, float power) {
        Store<EntityStore> store = context.getStore();
        Ref<EntityStore> caster = context.getCaster();

        // Skip self-damage
        if (target.equals(caster)) {
            return;
        }

        // Get asset-driven properties
        float baseDamage = getProperty("baseDamage", 8.0f);
        float slowDuration = getProperty("slowDuration", 4.0f);
        String damageType = getProperty("damageType", "cold");
        String slowEffectId = getProperty("slowEffectId", "hexcode:slow");

        // Calculate final values with power and context multipliers
        float actualDamage = baseDamage * power;
        float actualDuration = getModifiedDuration(slowDuration, context);

        LOGGER.atInfo().log("Applying ice effect: %.1f damage, %.1f slow duration",
                actualDamage, actualDuration);

        // Apply instant cold damage
        int damageCauseIndex = DamageCause.getAssetMap().getIndex(damageType);
        Damage iceDamage = new Damage(
                new Damage.EntitySource(caster),
                damageCauseIndex,
                actualDamage
        );
        DamageSystems.executeDamage(target, store, iceDamage);

        // Apply slow effect
        applySlowEffect(target, store, actualDuration, slowEffectId);

        LOGGER.atInfo().log("Applied ice damage and slow to target");
    }

    @Override
    protected void applyEffectAtPosition(SpellContext context, Vector3d position, float power) {
        // Ice can create a frost effect at a position
        LOGGER.atInfo().log("Ice effect at position (%.1f, %.1f, %.1f) with power %.2f",
                position.x, position.y, position.z, power);
        // Future: spawn ice particles/blocks at position
    }

    /**
     * Apply the slow status effect to a target.
     */
    private void applySlowEffect(Ref<EntityStore> targetRef, Store<EntityStore> store,
                                  float duration, String slowEffectId) {
        EffectControllerComponent effectController = store.getComponent(
                targetRef,
                EffectControllerComponent.getComponentType()
        );

        if (effectController == null) {
            LOGGER.atWarning().log("Target has no EffectControllerComponent, cannot apply slow");
            return;
        }

        // Try to get the slow effect asset
        EntityEffect slowEffect = EntityEffect.getAssetMap().getAsset(slowEffectId);
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
