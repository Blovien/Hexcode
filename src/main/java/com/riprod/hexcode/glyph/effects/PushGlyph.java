package com.riprod.hexcode.glyph.effects;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.GlyphVisual;
import com.riprod.hexcode.util.HexMathUtil;

import java.util.Set;

/**
 * Push effect glyph - applies knockback without damage.
 */
public class PushGlyph extends EffectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "hexcode:push";
    public static final int BASE_COST = 10;
    public static final float BASE_KNOCKBACK = 10.0f;
    public static final float KNOCKBACK_DURATION = 0.5f;

    public PushGlyph() {
        super(
            ID,
            "Push",
            BASE_COST,
            GlyphVisual.effect(GlyphVisual.COLOR_UTILITY),
            Set.of("hexcode:power")
        );
    }

    @Override
    public void applyEffect(ExecutionContext ctx, TargetSet targets) {
        float knockbackStrength = getModifiedAmount(ctx, BASE_KNOCKBACK);
        Store<EntityStore> store = ctx.getStore();
        Vector3d origin = ctx.getCurrentOrigin();

        LOGGER.atInfo().log("Applying push effect: %.1f knockback to %d targets",
                knockbackStrength, targets.getEntityCount());

        // Apply to each target entity
        for (Ref<EntityStore> targetRef : targets.getEntities()) {
            // Get target position to calculate push direction
            TransformComponent targetTransform = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (targetTransform == null) {
                continue;
            }

            Vector3d targetPos = targetTransform.getPosition();

            // Calculate push direction (away from origin)
            Vector3d pushDir = HexMathUtil.sub(targetPos, origin);
            if (pushDir.length() < 0.01) {
                // If too close, push away from caster position
                pushDir = new Vector3d(0, 1, 0); // Default upward if positions overlap
            } else {
                pushDir = pushDir.normalize();
            }

            // Add slight upward component for better knockback feel
            Vector3d knockbackVelocity = new Vector3d(
                    pushDir.x * knockbackStrength,
                    pushDir.y * knockbackStrength + knockbackStrength * 0.3,
                    pushDir.z * knockbackStrength
            );

            // Get knockback component - entities that can receive knockback should already have it
            KnockbackComponent knockback = store.getComponent(targetRef, KnockbackComponent.getComponentType());
            if (knockback == null) {
                LOGGER.atWarning().log("Target has no KnockbackComponent, cannot apply knockback");
                continue;
            }

            // Apply knockback
            knockback.setVelocity(knockbackVelocity);
            knockback.setVelocityType(ChangeVelocityType.Add);
            knockback.setDuration(KNOCKBACK_DURATION);
            knockback.setTimer(0.0f);

            LOGGER.atInfo().log("Applied knockback to target: (%.1f, %.1f, %.1f)",
                    knockbackVelocity.x, knockbackVelocity.y, knockbackVelocity.z);
        }
    }
}
