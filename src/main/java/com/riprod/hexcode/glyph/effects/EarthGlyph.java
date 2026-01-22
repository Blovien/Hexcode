package com.riprod.hexcode.glyph.effects;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.GlyphVisual;
import com.riprod.hexcode.util.HexMathUtil;

import java.util.Set;

/**
 * Earth effect glyph - deals physical damage with knockback.
 */
public class EarthGlyph extends EffectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "hexcode:earth";
    public static final int BASE_COST = 15;
    public static final float BASE_DAMAGE = 15.0f;
    public static final float KNOCKBACK_STRENGTH = 8.0f;
    public static final float KNOCKBACK_DURATION = 0.4f;

    public EarthGlyph() {
        super(
            ID,
            "Earth",
            BASE_COST,
            GlyphVisual.effect(GlyphVisual.COLOR_EARTH, "earth"),
            Set.of("hexcode:power")
        );
    }

    @Override
    public void applyEffect(ExecutionContext ctx, TargetSet targets) {
        float damage = getModifiedAmount(ctx, BASE_DAMAGE);
        float knockback = getModifiedAmount(ctx, KNOCKBACK_STRENGTH);

        Store<EntityStore> store = ctx.getStore();
        Ref<EntityStore> caster = ctx.getCaster();
        Vector3d origin = ctx.getCurrentOrigin();

        LOGGER.atInfo().log("Applying earth effect: %.1f damage, %.1f knockback to %d targets",
                damage, knockback, targets.getEntityCount());

        // Apply to each target entity
        for (Ref<EntityStore> targetRef : targets.getEntities()) {
            // Skip self-damage
            if (targetRef.equals(caster)) {
                continue;
            }

            // Apply instant physical damage
            int damageCauseIndex = DamageCause.getAssetMap().getIndex("physical");
            Damage earthDamage = new Damage(
                    new Damage.EntitySource(caster),
                    damageCauseIndex,
                    damage
            );
            DamageSystems.executeDamage(targetRef, store, earthDamage);

            // Apply knockback away from origin
            applyKnockback(targetRef, store, origin, knockback);

            LOGGER.atInfo().log("Applied earth damage and knockback to target");
        }
    }

    /**
     * Apply knockback to a target, pushing them away from the origin.
     */
    private void applyKnockback(Ref<EntityStore> targetRef, Store<EntityStore> store,
                                 Vector3d origin, float strength) {
        // Get target position to calculate knockback direction
        TransformComponent targetTransform = store.getComponent(targetRef, TransformComponent.getComponentType());
        if (targetTransform == null) {
            return;
        }

        Vector3d targetPos = targetTransform.getPosition();

        // Calculate knockback direction (away from origin)
        Vector3d knockbackDir = HexMathUtil.sub(targetPos, origin);
        if (knockbackDir.length() < 0.01) {
            // If too close, push straight up
            knockbackDir = new Vector3d(0, 1, 0);
        } else {
            knockbackDir = knockbackDir.normalize();
        }

        // Calculate knockback velocity with upward component
        Vector3d knockbackVelocity = new Vector3d(
                knockbackDir.x * strength,
                knockbackDir.y * strength + strength * 0.4, // Strong upward for earth
                knockbackDir.z * strength
        );

        // Get knockback component - entities that can receive knockback should already have it
        KnockbackComponent knockbackComp = store.getComponent(targetRef, KnockbackComponent.getComponentType());
        if (knockbackComp == null) {
            LOGGER.atWarning().log("Target has no KnockbackComponent, cannot apply knockback");
            return;
        }

        // Apply knockback
        knockbackComp.setVelocity(knockbackVelocity);
        knockbackComp.setVelocityType(ChangeVelocityType.Add);
        knockbackComp.setDuration(KNOCKBACK_DURATION);
        knockbackComp.setTimer(0.0f);

        LOGGER.atInfo().log("Applied earth knockback: (%.1f, %.1f, %.1f)",
                knockbackVelocity.x, knockbackVelocity.y, knockbackVelocity.z);
    }
}
