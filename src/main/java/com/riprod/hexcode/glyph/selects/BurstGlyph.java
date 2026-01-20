package com.riprod.hexcode.glyph.selects;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.util.HexMathUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Burst select glyph - selects all entities in a radius around current target/origin.
 * Instant - no travel time.
 */
public class BurstGlyph extends SelectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "hexcode:burst";
    public static final float BASE_RADIUS = 5.0f;

    public BurstGlyph() {
        super(
            ID,
            "Burst",
            false, // instant
            Set.of("hexcode:range")
        );
    }

    @Override
    public TargetSet selectTargets(ExecutionContext ctx) {
        float radius = getModifiedRange(ctx, BASE_RADIUS);
        Vector3d origin = ctx.getCurrentOrigin();
        Store<EntityStore> store = ctx.getStore();
        Ref<EntityStore> caster = ctx.getCaster();

        LOGGER.atInfo().log("Burst selecting targets within %.1f radius of (%.1f, %.1f, %.1f)",
                radius, origin.x, origin.y, origin.z);

        // Find all entities within radius
        List<Ref<EntityStore>> entitiesInRange = findEntitiesInRadius(store, origin, radius, caster);

        LOGGER.atInfo().log("Burst found %d targets", entitiesInRange.size());

        return TargetSet.ofEntities(entitiesInRange).withOrigin(origin);
    }

    /**
     * Find all entities within a radius of an origin point.
     * Note: This is a simplified implementation. In a real implementation,
     * you would use the SpatialResource for efficient spatial queries.
     */
    private List<Ref<EntityStore>> findEntitiesInRadius(Store<EntityStore> store, Vector3d origin,
                                                         float radius, Ref<EntityStore> excludeCaster) {
        List<Ref<EntityStore>> results = new ArrayList<>();

        // In a real implementation, we'd query the spatial index:
        // SpatialResource<Ref<EntityStore>, EntityStore> spatialResource =
        //     store.getResource(entitySpatialResourceType);
        // spatialResource.getSpatialStructure().ordered(origin, radius, results);

        // For now, this is a placeholder that shows the intent
        // The actual spatial query API would be used when available

        // Filter to entities with TransformComponent within radius
        // This would iterate over all entities (inefficient) or use the spatial index

        LOGGER.atInfo().log("Burst selection would query spatial index for entities within radius");

        return results;
    }
}
