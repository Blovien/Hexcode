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
import com.riprod.hexcode.util.RaycastUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Cone select glyph - selects entities in a cone in front of the caster.
 * Instant - no travel time.
 */
public class ConeGlyph extends SelectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "hexcode:cone";
    public static final float BASE_RANGE = 8.0f;
    public static final float CONE_ANGLE = 45.0f; // degrees (half-angle)

    public ConeGlyph() {
        super(
            ID,
            "Cone",
            false, // instant
            Set.of("hexcode:range")
        );
    }

    @Override
    public TargetSet selectTargets(ExecutionContext ctx) {
        float range = getModifiedRange(ctx, BASE_RANGE);
        Store<EntityStore> store = ctx.getStore();
        Ref<EntityStore> caster = ctx.getCaster();
        Vector3d origin = ctx.getCurrentOrigin();
        Vector3d direction = ctx.getCastDirection();

        LOGGER.atInfo().log("Cone selecting targets within %.1f range, %.1f degree cone", range, CONE_ANGLE);

        // Get caster position and look direction
        TransformComponent casterTransform = store.getComponent(caster, TransformComponent.getComponentType());
        if (casterTransform == null) {
            LOGGER.atWarning().log("Caster has no TransformComponent");
            return TargetSet.empty();
        }

        Vector3d casterPos = RaycastUtil.getPlayerEyePosition(casterTransform);

        // Find all entities within the cone
        List<Ref<EntityStore>> targetsInCone = findEntitiesInCone(store, caster, casterPos, direction, range, CONE_ANGLE);

        if (targetsInCone.isEmpty()) {
            LOGGER.atInfo().log("Cone found no targets");
            return TargetSet.empty();
        }

        LOGGER.atInfo().log("Cone found %d targets", targetsInCone.size());
        return TargetSet.ofEntities(targetsInCone).withOrigin(casterPos);
    }

    /**
     * Find all entities within a cone.
     *
     * @param store The entity store
     * @param caster The caster (excluded from results)
     * @param coneOrigin The cone's origin point
     * @param coneDirection The cone's direction (normalized)
     * @param range The cone's length
     * @param halfAngle The cone's half-angle in degrees
     * @return List of entities within the cone
     */
    private List<Ref<EntityStore>> findEntitiesInCone(Store<EntityStore> store, Ref<EntityStore> caster,
                                                       Vector3d coneOrigin, Vector3d coneDirection,
                                                       float range, float halfAngle) {
        List<Ref<EntityStore>> results = new ArrayList<>();

        // In a real implementation, we would:
        // 1. Query the spatial index for all entities within a sphere of radius = range
        // 2. Filter to those within the cone angle

        // The filtering logic would be:
        // for each candidate entity:
        //   - Get entity position
        //   - Calculate vector from cone origin to entity
        //   - If distance > range, skip
        //   - Calculate angle between cone direction and entity vector
        //   - If angle <= halfAngle, add to results

        // Convert half angle to radians for comparison
        double cosHalfAngle = Math.cos(Math.toRadians(halfAngle));

        // Placeholder - in real implementation, iterate over spatial query results
        LOGGER.atInfo().log("Cone selection would query spatial index for entities within range, then filter by angle");

        return results;
    }

    /**
     * Check if an entity position is within the cone.
     *
     * @param coneOrigin The cone's origin point
     * @param coneDirection The cone's direction (normalized)
     * @param entityPos The entity's position
     * @param range The cone's length
     * @param cosHalfAngle The cosine of the cone's half-angle
     * @return true if the entity is within the cone
     */
    private boolean isInCone(Vector3d coneOrigin, Vector3d coneDirection, Vector3d entityPos,
                              float range, double cosHalfAngle) {
        // Vector from cone origin to entity
        Vector3d toEntity = HexMathUtil.sub(entityPos, coneOrigin);
        double distance = toEntity.length();

        // Check range
        if (distance > range || distance < 0.01) {
            return false;
        }

        // Normalize the vector
        Vector3d toEntityNorm = new Vector3d(toEntity).normalize();

        // Calculate dot product (cosine of angle between directions)
        double dotProduct = toEntityNorm.x * coneDirection.x +
                           toEntityNorm.y * coneDirection.y +
                           toEntityNorm.z * coneDirection.z;

        // Entity is in cone if angle is within half-angle
        return dotProduct >= cosHalfAngle;
    }
}
