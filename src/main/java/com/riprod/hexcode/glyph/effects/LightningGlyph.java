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
import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.glyph.GlyphVisual;
import com.riprod.hexcode.util.HexMathUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lightning effect glyph - deals shock damage and chains to nearby entities.
 */
public class LightningGlyph extends EffectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "hexcode:lightning";
    public static final int BASE_COST = 15;
    public static final float BASE_DAMAGE = 12.0f;
    public static final int CHAIN_COUNT = 2;
    public static final float CHAIN_RANGE = 5.0f;
    public static final float CHAIN_DAMAGE_FALLOFF = 0.7f; // 70% damage per chain

    public LightningGlyph() {
        super(
            ID,
            "Lightning",
            BASE_COST,
            GlyphVisual.effect(GlyphVisual.COLOR_LIGHTNING, "lightning"),
            Set.of("hexcode:power")
        );
    }

    @Override
    public void applyEffect(ExecutionContext ctx, TargetSet targets) {
        float damage = getModifiedAmount(ctx, BASE_DAMAGE);

        Store<EntityStore> store = ctx.getStore();
        Ref<EntityStore> caster = ctx.getCaster();

        LOGGER.atInfo().log("Applying lightning effect: %.1f damage with %d chains to %d targets",
                damage, CHAIN_COUNT, targets.getEntityCount());

        // Track all entities that have been hit to avoid hitting twice
        Set<Ref<EntityStore>> alreadyHit = new HashSet<>();

        // Apply to each target entity
        for (Ref<EntityStore> targetRef : targets.getEntities()) {
            // Skip self-damage
            if (targetRef.equals(caster)) {
                continue;
            }

            if (alreadyHit.contains(targetRef)) {
                continue;
            }

            // Apply initial lightning damage
            applyLightningDamage(targetRef, store, caster, damage);
            alreadyHit.add(targetRef);

            // Chain to nearby entities
            chainLightning(targetRef, store, caster, damage, alreadyHit);
        }
    }

    /**
     * Apply lightning damage to a single target.
     */
    private void applyLightningDamage(Ref<EntityStore> targetRef, Store<EntityStore> store,
                                       Ref<EntityStore> caster, float damage) {
        int damageCauseIndex = DamageCause.getAssetMap().getIndex("lightning");
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
                                 Set<Ref<EntityStore>> alreadyHit) {
        Ref<EntityStore> currentTarget = initialTarget;
        float currentDamage = initialDamage;

        for (int chain = 0; chain < CHAIN_COUNT; chain++) {
            // Reduce damage for each chain
            currentDamage *= CHAIN_DAMAGE_FALLOFF;

            // Get position of current target
            TransformComponent transform = store.getComponent(currentTarget, TransformComponent.getComponentType());
            if (transform == null) {
                break;
            }
            Vector3d currentPos = transform.getPosition();

            // Find nearest entity within chain range that hasn't been hit
            Ref<EntityStore> nextTarget = findNearestUnhitEntity(store, currentPos, caster, alreadyHit);
            if (nextTarget == null) {
                LOGGER.atInfo().log("No more chain targets found after %d chains", chain);
                break;
            }

            // Apply chain damage
            applyLightningDamage(nextTarget, store, caster, currentDamage);
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
                                                     Ref<EntityStore> caster,
                                                     Set<Ref<EntityStore>> alreadyHit) {
        // In a real implementation, we'd query the spatial index:
        // SpatialResource<Ref<EntityStore>, EntityStore> spatialResource =
        //     store.getResource(entitySpatialResourceType);
        // List<Ref<EntityStore>> nearby = new ArrayList<>();
        // spatialResource.getSpatialStructure().ordered(origin, CHAIN_RANGE, nearby);
        //
        // Then filter out caster and already hit entities, return closest

        LOGGER.atInfo().log("Chain lightning would query spatial index for nearby entities");
        return null; // Placeholder - spatial query needed
    }
}
