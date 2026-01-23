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
import com.riprod.hexcode.util.HexMathUtil;
import com.riprod.hexcode.util.RaycastUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Cone select glyph - selects entities in a cone in front of the caster.
 *
 * <p>Instant - no travel time.
 *
 * <p>Asset-driven properties:
 * <ul>
 *   <li>range - cone length in blocks (default: 8.0)</li>
 *   <li>coneAngle - half-angle of cone in degrees (default: 45.0)</li>
 * </ul>
 */
public class ConeGlyph extends SelectGlyph {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Create a cone glyph from an asset definition.
     *
     * @param assetDefinition The asset definition containing glyph properties
     */
    public ConeGlyph(GlyphAssetDefinition assetDefinition) {
        super(assetDefinition, GlyphVisual.select("cone"), false);
    }

    @Override
    protected void selectTargets(SpellContext context) {
        float range = getModifiedRange(context);
        float coneAngle = getProperty("coneAngle", 45.0f);
        Store<EntityStore> store = context.getStore();
        Ref<EntityStore> caster = context.getCaster();
        Vector3d direction = context.getCastDirection();

        LOGGER.atInfo().log("Cone selecting targets within %.1f range, %.1f degree cone", range, coneAngle);

        // Get caster position and look direction
        TransformComponent casterTransform = store.getComponent(caster, TransformComponent.getComponentType());
        if (casterTransform == null) {
            LOGGER.atWarning().log("Caster has no TransformComponent");
            return;
        }

        Vector3d casterPos = RaycastUtil.getPlayerEyePosition(casterTransform);

        // Find all entities within the cone
        List<Ref<EntityStore>> targetsInCone = findEntitiesInCone(store, caster, casterPos, direction, range, coneAngle);

        if (targetsInCone.isEmpty()) {
            LOGGER.atInfo().log("Cone found no targets");
            return;
        }

        for (Ref<EntityStore> entity : targetsInCone) {
            context.addTarget(entity);
        }

        // Add origin as target position
        context.addTargetPosition(casterPos);

        LOGGER.atInfo().log("Cone found %d targets", targetsInCone.size());
    }

    /**
     * Find all entities within a cone.
     */
    private List<Ref<EntityStore>> findEntitiesInCone(Store<EntityStore> store, Ref<EntityStore> caster,
                                                       Vector3d coneOrigin, Vector3d coneDirection,
                                                       float range, float halfAngle) {
        List<Ref<EntityStore>> results = new ArrayList<>();

        // In a real implementation, we would:
        // 1. Query the spatial index for all entities within a sphere of radius = range
        // 2. Filter to those within the cone angle

        // Convert half angle to radians for comparison
        double cosHalfAngle = Math.cos(Math.toRadians(halfAngle));

        LOGGER.atInfo().log("Cone selection would query spatial index for entities within range, then filter by angle");

        return results;
    }

    /**
     * Check if an entity position is within the cone.
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
