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
import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.executing.SpellContext;
import com.riprod.hexcode.glyph.GlyphVisual;
import com.riprod.hexcode.util.HexMathUtil;

/**
 * Earth effect glyph - deals physical damage with knockback.
 *
 * <p>Asset-driven properties:
 * <ul>
 *   <li>baseDamage - base physical damage amount (default: 15.0)</li>
 *   <li>knockbackStrength - base knockback force (default: 8.0)</li>
 *   <li>knockbackDuration - knockback effect duration (default: 0.4)</li>
 *   <li>upwardMultiplier - vertical knockback component (default: 0.4)</li>
 *   <li>damageType - damage type ID (default: "physical")</li>
 * </ul>
 */
public class EarthGlyph extends EffectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Create an earth glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing glyph properties
     */
    public EarthGlyph(GlyphAssetDefinition assetDefinition) {
        super(assetDefinition, GlyphVisual.effect(GlyphVisual.COLOR_EARTH, "Earth"));
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
        float baseDamage = getProperty("baseDamage", 15.0f);
        float knockbackStrength = getProperty("knockbackStrength", 8.0f);
        float knockbackDuration = getProperty("knockbackDuration", 0.4f);
        float upwardMultiplier = getProperty("upwardMultiplier", 0.4f);
        String damageType = getProperty("damageType", "physical");

        // Calculate final values with power
        float actualDamage = baseDamage * power;
        float actualKnockback = knockbackStrength * power;

        LOGGER.atInfo().log("Applying earth effect: %.1f damage, %.1f knockback",
                actualDamage, actualKnockback);

        // Apply instant physical damage
        int damageCauseIndex = DamageCause.getAssetMap().getIndex(damageType);
        Damage earthDamage = new Damage(
                new Damage.EntitySource(caster),
                damageCauseIndex,
                actualDamage
        );
        DamageSystems.executeDamage(target, store, earthDamage);

        // Apply knockback away from cast origin
        applyKnockback(target, store, context.getCastOrigin(), actualKnockback, knockbackDuration, upwardMultiplier);

        LOGGER.atInfo().log("Applied earth damage and knockback to target");
    }

    @Override
    protected void applyEffectAtPosition(SpellContext context, Vector3d position, float power) {
        // Earth can create a tremor at a position
        LOGGER.atInfo().log("Earth effect at position (%.1f, %.1f, %.1f) with power %.2f",
                position.x, position.y, position.z, power);
        // Future: spawn rock/debris particles at position
    }

    /**
     * Apply knockback to a target, pushing them away from the origin.
     */
    private void applyKnockback(Ref<EntityStore> targetRef, Store<EntityStore> store,
                                 Vector3d origin, float strength, float duration, float upwardMult) {
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
                knockbackDir.y * strength + strength * upwardMult,
                knockbackDir.z * strength
        );

        // Get knockback component
        KnockbackComponent knockbackComp = store.getComponent(targetRef, KnockbackComponent.getComponentType());
        if (knockbackComp == null) {
            LOGGER.atWarning().log("Target has no KnockbackComponent, cannot apply knockback");
            return;
        }

        // Apply knockback
        knockbackComp.setVelocity(knockbackVelocity);
        knockbackComp.setVelocityType(ChangeVelocityType.Add);
        knockbackComp.setDuration(duration);
        knockbackComp.setTimer(0.0f);

        LOGGER.atInfo().log("Applied earth knockback: (%.1f, %.1f, %.1f)",
                knockbackVelocity.x, knockbackVelocity.y, knockbackVelocity.z);
    }
}
