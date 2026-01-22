package com.riprod.hexcode.glyph.effects;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.GlyphVisual;

import java.util.Set;

/**
 * Shield effect glyph - applies damage absorption buff.
 */
public class ShieldGlyph extends EffectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "hexcode:shield";
    public static final int BASE_COST = 20;
    public static final float BASE_ABSORPTION = 20.0f;
    public static final float SHIELD_DURATION = 10.0f;
    public static final String SHIELD_EFFECT_ID = "hexcode:shield";

    public ShieldGlyph() {
        super(
            ID,
            "Shield",
            BASE_COST,
            GlyphVisual.effect(GlyphVisual.COLOR_SHIELD, "shield"),
            Set.of("hexcode:power", "hexcode:duration")
        );
    }

    @Override
    public void applyEffect(ExecutionContext ctx, TargetSet targets) {
        float absorption = getModifiedAmount(ctx, BASE_ABSORPTION);
        float duration = getModifiedDuration(ctx, SHIELD_DURATION);

        Store<EntityStore> store = ctx.getStore();

        LOGGER.atInfo().log("Applying shield effect: %.1f absorption, %.1f duration to %d targets",
                absorption, duration, targets.getEntityCount());

        // Apply shield buff to each target entity
        for (Ref<EntityStore> targetRef : targets.getEntities()) {
            applyShieldEffect(targetRef, store, duration, absorption);
        }
    }

    /**
     * Apply the shield buff to a target.
     */
    private void applyShieldEffect(Ref<EntityStore> targetRef, Store<EntityStore> store,
                                    float duration, float absorption) {
        EffectControllerComponent effectController = store.getComponent(
                targetRef,
                EffectControllerComponent.getComponentType()
        );

        if (effectController == null) {
            LOGGER.atWarning().log("Target has no EffectControllerComponent, cannot apply shield");
            return;
        }

        // Try to get the shield effect asset
        EntityEffect shieldEffect = EntityEffect.getAssetMap().getAsset(SHIELD_EFFECT_ID);
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
        // The absorption amount would typically be set via effect parameters
        effectController.addEffect(targetRef, effectIndex, shieldEffect, duration, OverlapBehavior.EXTEND, store);
        LOGGER.atInfo().log("Applied shield effect for %.1f seconds with %.1f absorption", duration, absorption);
    }
}
