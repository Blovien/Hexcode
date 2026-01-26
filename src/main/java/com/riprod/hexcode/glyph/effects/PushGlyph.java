package com.riprod.hexcode.glyph.effects;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.executing.SpellContext;
import com.riprod.hexcode.glyph.GlyphVisual;
import com.riprod.hexcode.util.HexMathUtil;

/**
 * Push effect glyph - applies knockback without damage.
 *
 * <p>Asset-driven properties:
 * <ul>
 *   <li>baseKnockback - base knockback force (default: 10.0)</li>
 *   <li>knockbackDuration - knockback effect duration (default: 0.5)</li>
 *   <li>upwardMultiplier - vertical knockback component (default: 0.3)</li>
 * </ul>
 */
public class PushGlyph extends EffectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Create a push glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing glyph properties
     */
    public PushGlyph(GlyphAssetDefinition assetDefinition) {
        super(assetDefinition, GlyphVisual.effect(GlyphVisual.COLOR_PUSH, "Push"));
    }

    @Override
    protected void applyEffect(SpellContext context, Ref<EntityStore> target, float power) {
        Store<EntityStore> store = context.getStore();
        Vector3d origin = context.getCastOrigin();

        // Get asset-driven properties
        float baseKnockback = getProperty("baseKnockback", 10.0f);
        float knockbackDuration = getProperty("knockbackDuration", 0.5f);
        float upwardMultiplier = getProperty("upwardMultiplier", 0.3f);

        // Calculate final knockback with power
        float actualKnockback = baseKnockback * power;

        LOGGER.atInfo().log("Applying push effect: %.1f knockback", actualKnockback);

        // Get target position to calculate push direction
        TransformComponent targetTransform = store.getComponent(target, TransformComponent.getComponentType());
        if (targetTransform == null) {
            LOGGER.atWarning().log("Target has no TransformComponent, cannot push");
            return;
        }

        Vector3d targetPos = targetTransform.getPosition();

        // Calculate push direction (away from origin)
        Vector3d pushDir = HexMathUtil.sub(targetPos, origin);
        if (pushDir.length() < 0.01) {
            // If too close, push upward
            pushDir = new Vector3d(0, 1, 0);
        } else {
            pushDir = pushDir.normalize();
        }

        // Add upward component for better knockback feel
        Vector3d knockbackVelocity = new Vector3d(
                pushDir.x * actualKnockback,
                pushDir.y * actualKnockback + actualKnockback * upwardMultiplier,
                pushDir.z * actualKnockback
        );

        // Get knockback component
        KnockbackComponent knockback = store.getComponent(target, KnockbackComponent.getComponentType());
        if (knockback == null) {
            LOGGER.atWarning().log("Target has no KnockbackComponent, cannot apply knockback");
            return;
        }

        // Apply knockback
        knockback.setVelocity(knockbackVelocity);
        knockback.setVelocityType(ChangeVelocityType.Add);
        knockback.setDuration(knockbackDuration);
        knockback.setTimer(0.0f);

        LOGGER.atInfo().log("Applied knockback to target: (%.1f, %.1f, %.1f)",
                knockbackVelocity.x, knockbackVelocity.y, knockbackVelocity.z);
    }

    @Override
    protected void applyEffectAtPosition(SpellContext context, Vector3d position, float power) {
        // Push doesn't make sense at a position without a target
        LOGGER.atInfo().log("Push effect at position (%.1f, %.1f, %.1f) - no target to push",
                position.x, position.y, position.z);
    }
}
