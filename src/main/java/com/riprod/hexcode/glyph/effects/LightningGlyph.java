package com.riprod.hexcode.glyph.effects;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.execution.SpellContext;
import com.riprod.hexcode.glyph.GlyphVisual;

import java.util.HashSet;
import java.util.Set;

/**
 * Lightning effect glyph - deals shock damage and chains to nearby entities.
 *
 * <p>Asset-driven properties:
 * <ul>
 *   <li>baseDamage - base lightning damage amount (default: 12.0)</li>
 *   <li>chainCount - number of times to chain (default: 2)</li>
 *   <li>chainRange - range to find chain targets (default: 5.0)</li>
 *   <li>chainFalloff - damage multiplier per chain (default: 0.7)</li>
 *   <li>damageType - damage type ID (default: "lightning")</li>
 * </ul>
 */
public class LightningGlyph extends EffectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Track hit entities across all targets in one cast
    private final Set<Ref<EntityStore>> alreadyHit = new HashSet<>();

    /**
     * Create a lightning glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing glyph properties
     */
    public LightningGlyph(GlyphAssetDefinition assetDefinition) {
        super(assetDefinition, GlyphVisual.effect(GlyphVisual.COLOR_LIGHTNING, "lightning"));
    }

    @Override
    public SpellContext cast(SpellContext context) {
        // Clear hit tracking for this cast
        alreadyHit.clear();
        return super.cast(context);
    }

    @Override
    protected void applyEffect(SpellContext context, Ref<EntityStore> target, float power) {
        Store<EntityStore> store = context.getStore();
        Ref<EntityStore> caster = context.getCaster();

        // Skip self-damage and already-hit targets
        if (target.equals(caster) || alreadyHit.contains(target)) {
            return;
        }

        // Get asset-driven properties
        float baseDamage = getProperty("baseDamage", 12.0f);
        int chainCount = getProperty("chainCount", 2);
        float chainRange = getProperty("chainRange", 5.0f);
        float chainFalloff = getProperty("chainFalloff", 0.7f);
        String damageType = getProperty("damageType", "lightning");

        // Calculate final damage
        float actualDamage = baseDamage * power;

        LOGGER.atInfo().log("Applying lightning effect: %.1f damage with %d chains",
                actualDamage, chainCount);

        // Apply initial lightning damage
        applyLightningDamage(target, store, caster, actualDamage, damageType);
        alreadyHit.add(target);

        // Chain to nearby entities
        chainLightning(target, store, caster, actualDamage, chainCount, chainRange, chainFalloff, damageType);
    }

    @Override
    protected void applyEffectAtPosition(SpellContext context, Vector3d position, float power) {
        // Lightning can strike a position
        LOGGER.atInfo().log("Lightning effect at position (%.1f, %.1f, %.1f) with power %.2f",
                position.x, position.y, position.z, power);
        // Future: spawn lightning visual at position
    }

    /**
     * Apply lightning damage to a single target.
     */
    private void applyLightningDamage(Ref<EntityStore> targetRef, Store<EntityStore> store,
                                       Ref<EntityStore> caster, float damage, String damageType) {
        int damageCauseIndex = DamageCause.getAssetMap().getIndex(damageType);
        Damage lightningDamage = new Damage(
                new Damage.EntitySource(caster),
                damageCauseIndex,
                damage
        );
        DamageSystems.executeDamage(targetRef, store, lightningDamage);
        LOGGER.atInfo().log("Applied %.1f lightning damage to target", damage);
    }

    /**
     * Chain lightning to nearby entities.
     */
    private void chainLightning(Ref<EntityStore> initialTarget, Store<EntityStore> store,
                                 Ref<EntityStore> caster, float initialDamage,
                                 int chainCount, float chainRange, float chainFalloff,
                                 String damageType) {
        Ref<EntityStore> currentTarget = initialTarget;
        float currentDamage = initialDamage;

        for (int chain = 0; chain < chainCount; chain++) {
            // Reduce damage for each chain
            currentDamage *= chainFalloff;

            // Get position of current target
            TransformComponent transform = store.getComponent(currentTarget, TransformComponent.getComponentType());
            if (transform == null) {
                break;
            }
            Vector3d currentPos = transform.getPosition();

            // Find nearest entity within chain range that hasn't been hit
            Ref<EntityStore> nextTarget = findNearestUnhitEntity(store, currentPos, caster, chainRange);
            if (nextTarget == null) {
                LOGGER.atInfo().log("No more chain targets found after %d chains", chain);
                break;
            }

            // Apply chain damage
            applyLightningDamage(nextTarget, store, caster, currentDamage, damageType);
            alreadyHit.add(nextTarget);
            currentTarget = nextTarget;

            LOGGER.atInfo().log("Chained lightning (chain %d) for %.1f damage", chain + 1, currentDamage);
        }
    }

    /**
     * Find the nearest entity within range that hasn't been hit yet.
     * Note: In a full implementation, this would use spatial queries.
     */
    private Ref<EntityStore> findNearestUnhitEntity(Store<EntityStore> store, Vector3d origin,
                                                     Ref<EntityStore> caster, float chainRange) {
        // In a real implementation, we'd query the spatial index:
        // SpatialResource<Ref<EntityStore>, EntityStore> spatialResource =
        //     store.getResource(entitySpatialResourceType);
        // List<Ref<EntityStore>> nearby = new ArrayList<>();
        // spatialResource.getSpatialStructure().ordered(origin, chainRange, nearby);
        //
        // Then filter out caster and already hit entities, return closest

        LOGGER.atInfo().log("Chain lightning would query spatial index for nearby entities");
        return null; // Placeholder - spatial query needed
    }
}
