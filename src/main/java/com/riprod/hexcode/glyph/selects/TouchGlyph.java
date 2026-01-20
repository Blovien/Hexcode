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

import java.util.Set;

/**
 * Touch select glyph - targets entity in melee range (3 blocks).
 * Instant - no travel time.
 */
public class TouchGlyph extends SelectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String ID = "hexcode:touch";
    public static final float BASE_RANGE = 3.0f;
    public static final float HIT_RADIUS = 0.5f; // Entity hit radius for raycast

    public TouchGlyph() {
        super(
            ID,
            "Touch",
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

        LOGGER.atInfo().log("Touch selecting target within %.1f range", range);

        // Get caster position and look direction
        TransformComponent casterTransform = store.getComponent(caster, TransformComponent.getComponentType());
        if (casterTransform == null) {
            LOGGER.atWarning().log("Caster has no TransformComponent");
            return TargetSet.empty();
        }

        Vector3d eyePos = RaycastUtil.getPlayerEyePosition(casterTransform);

        // Find closest entity along the ray within range
        Ref<EntityStore> hitEntity = findEntityAlongRay(store, caster, eyePos, direction, range);

        if (hitEntity != null) {
            // Calculate hit position
            TransformComponent hitTransform = store.getComponent(hitEntity, TransformComponent.getComponentType());
            Vector3d hitPosition = hitTransform != null ? hitTransform.getPosition() : origin;

            LOGGER.atInfo().log("Touch found target");
            return TargetSet.of(hitEntity).withOrigin(hitPosition);
        }

        LOGGER.atInfo().log("Touch found no target in range");
        return TargetSet.empty();
    }

    /**
     * Find the first entity hit along a ray.
     * In a full implementation, this would use proper spatial queries and collision detection.
     */
    private Ref<EntityStore> findEntityAlongRay(Store<EntityStore> store, Ref<EntityStore> caster,
                                                  Vector3d rayOrigin, Vector3d rayDirection, float maxDistance) {
        // In a real implementation, we would:
        // 1. Query the spatial index for entities in a cylinder along the ray
        // 2. Sort by distance
        // 3. Return the closest one that passes collision test

        // For now, this is a placeholder that indicates the structure
        LOGGER.atInfo().log("Touch raycast would query spatial index for entities along ray");
        return null; // Placeholder - spatial query needed
    }
}
