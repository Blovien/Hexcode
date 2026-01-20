package com.riprod.hexcode.glyph.selects;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.execution.ExecutionContext;
import com.riprod.hexcode.execution.TargetSet;
import com.riprod.hexcode.util.RaycastUtil;

import java.util.Set;

/**
 * Gaze select glyph - targets first entity in line of sight.
 * Instant - no travel time, but longer range than Touch.
 */
public class GazeGlyph extends SelectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "hexcode:gaze";
    public static final float BASE_RANGE = 50.0f;
    public static final float HIT_RADIUS = 0.5f;

    public GazeGlyph() {
        super(
            ID,
            "Gaze",
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

        LOGGER.atInfo().log("Gaze selecting target within %.1f range", range);

        // Get caster position and look direction
        TransformComponent casterTransform = store.getComponent(caster, TransformComponent.getComponentType());
        if (casterTransform == null) {
            LOGGER.atWarning().log("Caster has no TransformComponent");
            return TargetSet.empty();
        }

        Vector3d eyePos = RaycastUtil.getPlayerEyePosition(casterTransform);

        // Find first entity in line of sight
        Ref<EntityStore> hitEntity = findFirstEntityInLineOfSight(store, caster, eyePos, direction, range);

        if (hitEntity != null) {
            // Calculate hit position
            TransformComponent hitTransform = store.getComponent(hitEntity, TransformComponent.getComponentType());
            Vector3d hitPosition = hitTransform != null ? hitTransform.getPosition() : origin;

            LOGGER.atInfo().log("Gaze found target");
            return TargetSet.of(hitEntity).withOrigin(hitPosition);
        }

        // If no entity hit, target the point at max range (for position-based effects)
        Vector3d endPoint = RaycastUtil.getPointAlongLookRay(casterTransform, range);
        LOGGER.atInfo().log("Gaze found no entity, targeting end point");
        return TargetSet.ofPosition(endPoint).withOrigin(endPoint);
    }

    /**
     * Find the first entity in line of sight.
     * In a full implementation, this would use proper spatial queries and occlusion testing.
     */
    private Ref<EntityStore> findFirstEntityInLineOfSight(Store<EntityStore> store, Ref<EntityStore> caster,
                                                           Vector3d rayOrigin, Vector3d rayDirection, float maxDistance) {
        // In a real implementation, we would:
        // 1. Query the spatial index for entities along the ray
        // 2. Check for block occlusion between caster and each candidate
        // 3. Return the closest unoccluded entity

        // For now, this is a placeholder that indicates the structure
        LOGGER.atInfo().log("Gaze raycast would query spatial index for entities in line of sight");
        return null; // Placeholder - spatial query needed
    }
}
