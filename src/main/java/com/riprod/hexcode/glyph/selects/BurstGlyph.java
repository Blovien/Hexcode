package com.riprod.hexcode.glyph.selects;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.execution.SpellContext;
import com.riprod.hexcode.glyph.GlyphVisual;

import java.util.ArrayList;
import java.util.List;

/**
 * Burst select glyph - selects all entities in a radius around current target/origin.
 *
 * <p>Instant - no travel time.
 *
 * <p>Asset-driven properties:
 * <ul>
 *   <li>radius - base selection radius in blocks (default: 5.0)</li>
 * </ul>
 */
public class BurstGlyph extends SelectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Create a burst glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing glyph properties
     */
    public BurstGlyph(GlyphAssetDefinition assetDefinition) {
        super(assetDefinition, GlyphVisual.select("burst"), false);
    }

    @Override
    protected void selectTargets(SpellContext context) {
        float radius = getModifiedRadius(context);
        Vector3d origin = context.getCastOrigin();
        Store<EntityStore> store = context.getStore();
        Ref<EntityStore> caster = context.getCaster();

        // If there are existing targets from a parent select, use the first one as origin
        if (!context.getTargets().isEmpty()) {
            Ref<EntityStore> firstTarget = context.getTargets().get(0);
            var transform = store.getComponent(firstTarget,
                com.hypixel.hytale.server.core.modules.entity.component.TransformComponent.getComponentType());
            if (transform != null) {
                origin = transform.getPosition();
            }
        }

        LOGGER.atInfo().log("Burst selecting targets within %.1f radius of (%.1f, %.1f, %.1f)",
                radius, origin.x, origin.y, origin.z);

        // Find all entities within radius
        List<Ref<EntityStore>> entitiesInRange = findEntitiesInRadius(store, origin, radius, caster);

        for (Ref<EntityStore> entity : entitiesInRange) {
            context.addTarget(entity);
        }

        // Also add the origin as a target position
        context.addTargetPosition(origin);

        LOGGER.atInfo().log("Burst found %d targets", entitiesInRange.size());
    }

    /**
     * Find all entities within a radius of an origin point.
     */
    private List<Ref<EntityStore>> findEntitiesInRadius(Store<EntityStore> store, Vector3d origin,
                                                         float radius, Ref<EntityStore> excludeCaster) {
        List<Ref<EntityStore>> results = new ArrayList<>();

        // In a real implementation, we'd query the spatial index:
        // SpatialResource<Ref<EntityStore>, EntityStore> spatialResource =
        //     store.getResource(entitySpatialResourceType);
        // spatialResource.getSpatialStructure().ordered(origin, radius, results);

        LOGGER.atInfo().log("Burst selection would query spatial index for entities within radius");

        return results;
    }
}
