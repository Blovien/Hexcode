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
import com.riprod.hexcode.executing.SpellContext;
import com.riprod.hexcode.glyph.GlyphVisual;

/**
 * Void effect glyph - deals void damage and applies brief blindness.
 *
 * <p>Asset-driven properties:
 * <ul>
 *   <li>baseDamage - base void damage amount (default: 11.0)</li>
 *   <li>blindDuration - duration of blindness effect in seconds (default: 2.0)</li>
 *   <li>blindEffectId - ID of the blindness effect to apply (default: "hexcode:blindness")</li>
 *   <li>damageType - damage type ID (default: "Void")</li>
 * </ul>
 */
public class VoidGlyph extends EffectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Create a void glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing glyph properties
     */
    public VoidGlyph(GlyphAssetDefinition assetDefinition) {
        super(assetDefinition, GlyphVisual.effect(GlyphVisual.COLOR_VOID, "Void"));
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
        float baseDamage = getProperty("baseDamage", 11.0f);
        float blindDuration = getProperty("blindDuration", 2.0f);
        String damageType = getProperty("damageType", "Void");
        String blindEffectId = getProperty("blindEffectId", "hexcode:blindness");

        // Calculate final values with power and context multipliers
        float actualDamage = baseDamage * power;
        float actualDuration = getModifiedDuration(blindDuration, context);

        LOGGER.atInfo().log("Applying void effect: %.1f damage, %.1f blindness duration",
                actualDamage, actualDuration);

        // Apply instant void damage
        int damageCauseIndex = DamageCause.getAssetMap().getIndex(damageType);
        Damage voidDamage = new Damage(
                new Damage.EntitySource(caster),
                damageCauseIndex,
                actualDamage
        );
        DamageSystems.executeDamage(target, store, voidDamage);

        // Apply blindness effect
        applyBlindnessEffect(target, store, actualDuration, blindEffectId);

        LOGGER.atInfo().log("Applied void damage and blindness to target");
    }

    @Override
    protected void applyEffectAtPosition(SpellContext context, Vector3d position, float power) {
        // Void can create a dark zone at a position
        LOGGER.atInfo().log("Void effect at position (%.1f, %.1f, %.1f) with power %.2f",
                position.x, position.y, position.z, power);
        // Future: spawn void particles at position
    }

    /**
     * Apply the blindness status effect to a target.
     */
    private void applyBlindnessEffect(Ref<EntityStore> targetRef, Store<EntityStore> store,
                                       float duration, String blindEffectId) {
        EffectControllerComponent effectController = store.getComponent(
                targetRef,
                EffectControllerComponent.getComponentType()
        );

        if (effectController == null) {
            LOGGER.atWarning().log("Target has no EffectControllerComponent, cannot apply blindness");
            return;
        }

        // Try to get the blindness effect asset
        EntityEffect blindEffect = EntityEffect.getAssetMap().getAsset(blindEffectId);
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
