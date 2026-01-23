package com.riprod.hexcode.glyph.selects;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.asset.GlyphAssetDefinition;
import com.riprod.hexcode.execution.SpellContext;
import com.riprod.hexcode.glyph.GlyphVisual;
import com.riprod.hexcode.util.RaycastUtil;

/**
 * Gaze select glyph - targets first entity in line of sight.
 *
 * <p>Instant - no travel time, but longer range than Touch.
 *
 * <p>Asset-driven properties:
 * <ul>
 *   <li>range - base gaze range in blocks (default: 50.0)</li>
 *   <li>hitRadius - entity hit detection radius (default: 0.5)</li>
 * </ul>
 */
public class GazeGlyph extends SelectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Create a gaze glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing glyph properties
     */
    public GazeGlyph(GlyphAssetDefinition assetDefinition) {
        super(assetDefinition, GlyphVisual.select("gaze"), false);
    }

    @Override
    protected void selectTargets(SpellContext context) {
        float range = getModifiedRange(context);
        Store<EntityStore> store = context.getStore();
        Ref<EntityStore> caster = context.getCaster();
        Vector3d direction = context.getCastDirection();

        LOGGER.atInfo().log("Gaze selecting target within %.1f range", range);

        // Get caster position and look direction
        TransformComponent casterTransform = store.getComponent(caster, TransformComponent.getComponentType());
        if (casterTransform == null) {
            LOGGER.atWarning().log("Caster has no TransformComponent");
            return;
        }

        Vector3d eyePos = RaycastUtil.getPlayerEyePosition(casterTransform);

        // Find first entity in line of sight
        Ref<EntityStore> hitEntity = findFirstEntityInLineOfSight(store, caster, eyePos, direction, range);

        if (hitEntity != null) {
            // Calculate hit position
            TransformComponent hitTransform = store.getComponent(hitEntity, TransformComponent.getComponentType());
            Vector3d hitPosition = hitTransform != null ? hitTransform.getPosition() : eyePos;

            LOGGER.atInfo().log("Gaze found target");
            context.addTarget(hitEntity);
            context.addTargetPosition(hitPosition);
        } else {
            // If no entity hit, target the point at max range (for position-based effects)
            Vector3d endPoint = RaycastUtil.getPointAlongLookRay(casterTransform, range);
            LOGGER.atInfo().log("Gaze found no entity, targeting end point");
            context.addTargetPosition(endPoint);
        }
    }

    /**
     * Find the first entity in line of sight.
     */
    private Ref<EntityStore> findFirstEntityInLineOfSight(Store<EntityStore> store, Ref<EntityStore> caster,
                                                           Vector3d rayOrigin, Vector3d rayDirection, float maxDistance) {
        // In a real implementation, we would:
        // 1. Query the spatial index for entities along the ray
        // 2. Check for block occlusion between caster and each candidate
        // 3. Return the closest unoccluded entity

        LOGGER.atInfo().log("Gaze raycast would query spatial index for entities in line of sight");
        return null; // Placeholder - spatial query needed
    }
}
