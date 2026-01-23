package com.riprod.hexcode.glyph.effects;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.execution.SpellContext;
import com.riprod.hexcode.glyph.GlyphVisual;

/**
 * Shield effect glyph - applies damage absorption buff.
 *
 * <p>Asset-driven properties:
 * <ul>
 *   <li>baseAbsorption - amount of damage to absorb (default: 20.0)</li>
 *   <li>shieldDuration - duration of the shield effect in seconds (default: 10.0)</li>
 *   <li>shieldEffectId - ID of the shield effect to apply (default: "hexcode:shield")</li>
 * </ul>
 */
public class ShieldGlyph extends EffectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Create a shield glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing glyph properties
     */
    public ShieldGlyph(GlyphAssetDefinition assetDefinition) {
        super(assetDefinition, GlyphVisual.effect(GlyphVisual.COLOR_SHIELD, "shield"));
    }

    @Override
    protected void applyEffect(SpellContext context, Ref<EntityStore> target, float power) {
        Store<EntityStore> store = context.getStore();

        // Get asset-driven properties
        float baseAbsorption = getProperty("baseAbsorption", 20.0f);
        float shieldDuration = getProperty("shieldDuration", 10.0f);
        String shieldEffectId = getProperty("shieldEffectId", "hexcode:shield");

        // Calculate final values with power and context multipliers
        float actualAbsorption = baseAbsorption * power;
        float actualDuration = getModifiedDuration(shieldDuration, context);

        LOGGER.atInfo().log("Applying shield effect: %.1f absorption, %.1f duration",
                actualAbsorption, actualDuration);

        // Apply shield buff
        applyShieldEffect(target, store, actualDuration, actualAbsorption, shieldEffectId);
    }

    @Override
    protected void applyEffectAtPosition(SpellContext context, Vector3d position, float power) {
        // Shield doesn't make sense at a position without a target
        LOGGER.atInfo().log("Shield effect at position (%.1f, %.1f, %.1f) - no target to shield",
                position.x, position.y, position.z);
    }

    /**
     * Apply the shield buff to a target.
     */
    private void applyShieldEffect(Ref<EntityStore> targetRef, Store<EntityStore> store,
                                    float duration, float absorption, String shieldEffectId) {
        EffectControllerComponent effectController = store.getComponent(
                targetRef,
                EffectControllerComponent.getComponentType()
        );

        if (effectController == null) {
            LOGGER.atWarning().log("Target has no EffectControllerComponent, cannot apply shield");
            return;
        }

        // Try to get the shield effect asset
        EntityEffect shieldEffect = EntityEffect.getAssetMap().getAsset(shieldEffectId);
        if (shieldEffect == null) {
            // Fallback to absorption or damage_resistance effect
            shieldEffect = EntityEffect.getAssetMap().getAsset("absorption");
            if (shieldEffect == null) {
                shieldEffect = EntityEffect.getAssetMap().getAsset("damage_resistance");
                if (shieldEffect == null) {
                    LOGGER.atWarning().log("No shield/absorption effect found, skipping effect application");
                    return;
                }
            }
        }

        // Get the effect index from the asset map
        int effectIndex = EntityEffect.getAssetMap().getIndex(shieldEffect.getId());

        // Apply the shield effect with duration
        effectController.addEffect(targetRef, effectIndex, shieldEffect, duration, OverlapBehavior.EXTEND, store);
        LOGGER.atInfo().log("Applied shield effect for %.1f seconds with %.1f absorption", duration, absorption);
    }
}
