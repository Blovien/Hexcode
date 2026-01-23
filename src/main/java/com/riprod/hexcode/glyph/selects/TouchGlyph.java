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
 * Touch select glyph - targets entity in melee range.
 *
 * <p>Instant - no travel time.
 *
 * <p>Asset-driven properties:
 * <ul>
 *   <li>range - base touch range in blocks (default: 3.0)</li>
 *   <li>hitRadius - entity hit detection radius (default: 0.5)</li>
 * </ul>
 */
public class TouchGlyph extends SelectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Create a touch glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing glyph properties
     */
    public TouchGlyph(GlyphAssetDefinition assetDefinition) {
        super(assetDefinition, GlyphVisual.select("touch"), false);
    }

    @Override
    protected void selectTargets(SpellContext context) {
        float range = getModifiedRange(context);
        Store<EntityStore> store = context.getStore();
        Ref<EntityStore> caster = context.getCaster();
        Vector3d direction = context.getCastDirection();

        LOGGER.atInfo().log("Touch selecting target within %.1f range", range);

        // Get caster position and look direction
        TransformComponent casterTransform = store.getComponent(caster, TransformComponent.getComponentType());
        if (casterTransform == null) {
            LOGGER.atWarning().log("Caster has no TransformComponent");
            return;
        }

        Vector3d eyePos = RaycastUtil.getPlayerEyePosition(casterTransform);

        // Find closest entity along the ray within range
        Ref<EntityStore> hitEntity = findEntityAlongRay(store, caster, eyePos, direction, range);

        if (hitEntity != null) {
            // Calculate hit position
            TransformComponent hitTransform = store.getComponent(hitEntity, TransformComponent.getComponentType());
            Vector3d hitPosition = hitTransform != null ? hitTransform.getPosition() : eyePos;

            LOGGER.atInfo().log("Touch found target");
            context.addTarget(hitEntity);
            context.addTargetPosition(hitPosition);
        } else {
            LOGGER.atInfo().log("Touch found no target in range");
        }
    }

    /**
     * Find the first entity hit along a ray.
     */
    private Ref<EntityStore> findEntityAlongRay(Store<EntityStore> store, Ref<EntityStore> caster,
                                                  Vector3d rayOrigin, Vector3d rayDirection, float maxDistance) {
        // In a real implementation, we would:
        // 1. Query the spatial index for entities in a cylinder along the ray
        // 2. Sort by distance
        // 3. Return the closest one that passes collision test

        LOGGER.atInfo().log("Touch raycast would query spatial index for entities along ray");
        return null; // Placeholder - spatial query needed
    }
}
