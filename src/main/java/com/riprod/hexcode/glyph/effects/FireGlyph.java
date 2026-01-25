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
 * Fire effect glyph - deals fire damage and applies burn DOT.
 *
 * <p>Asset-driven properties:
 * <ul>
 *   <li>baseDamage - base fire damage amount (default: 10.0)</li>
 *   <li>burnDuration - duration of burn DOT in seconds (default: 3.0)</li>
 *   <li>burnEffectId - ID of the burn effect to apply (default: "hexcode:burn")</li>
 *   <li>damageType - damage type ID (default: "fire")</li>
 * </ul>
 */
public class FireGlyph extends EffectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Create a fire glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing glyph properties
     */
    public FireGlyph(GlyphAssetDefinition assetDefinition) {
        super(assetDefinition, GlyphVisual.effect(GlyphVisual.COLOR_FIRE, "Fire"));
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
        float baseDamage = getProperty("baseDamage", 10.0f);
        float burnDuration = getProperty("burnDuration", 3.0f);
        String damageType = getProperty("damageType", "fire");
        String burnEffectId = getProperty("burnEffectId", "hexcode:burn");

        // Calculate final values with power and context multipliers
        float actualDamage = baseDamage * power;
        float actualDuration = getModifiedDuration(burnDuration, context);

        LOGGER.atInfo().log("Applying fire effect: %.1f damage, %.1f burn duration",
                actualDamage, actualDuration);

        // Apply instant fire damage
        int damageCauseIndex = DamageCause.getAssetMap().getIndex(damageType);
        Damage fireDamage = new Damage(
                new Damage.EntitySource(caster),
                damageCauseIndex,
                actualDamage
        );
        DamageSystems.executeDamage(target, store, fireDamage);

        // Apply burn DOT effect
        applyBurnEffect(target, store, actualDuration, burnEffectId);

        LOGGER.atInfo().log("Applied fire damage to target");
    }

    @Override
    protected void applyEffectAtPosition(SpellContext context, Vector3d position, float power) {
        // Fire can create a fire effect at a position
        LOGGER.atInfo().log("Fire effect at position (%.1f, %.1f, %.1f) with power %.2f",
                position.x, position.y, position.z, power);
        // Future: spawn fire particles/blocks at position
    }

    /**
     * Apply the burn status effect to a target.
     */
    private void applyBurnEffect(Ref<EntityStore> targetRef, Store<EntityStore> store,
                                  float duration, String burnEffectId) {
        EffectControllerComponent effectController = store.getComponent(
                targetRef,
                EffectControllerComponent.getComponentType()
        );

        if (effectController == null) {
            LOGGER.atWarning().log("Target has no EffectControllerComponent, cannot apply burn");
            return;
        }

        // Try to get the burn effect asset
        EntityEffect burnEffect = EntityEffect.getAssetMap().getAsset(burnEffectId);
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
