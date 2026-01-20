package com.riprod.hexcode.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.entity.OrbitalGlyphEntity;

import java.util.List;

/**
 * Utility methods for raycasting and glyph hover detection.
 */
public class RaycastUtil {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Default glyph bounding sphere radius for hit detection
    private static final float DEFAULT_GLYPH_HIT_RADIUS = 0.4f;

    private RaycastUtil() {}

    /**
     * Find the orbital glyph entity that the player is looking at.
     *
     * @param store The entity store
     * @param playerRef The player entity reference
     * @param orbitalEntities List of orbital glyph entities to check
     * @param maxDistance Maximum raycast distance
     * @return The hovered orbital glyph entity, or null if none
     */
    public static OrbitalGlyphEntity findHoveredGlyph(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                                       List<OrbitalGlyphEntity> orbitalEntities, float maxDistance) {
        if (orbitalEntities == null || orbitalEntities.isEmpty()) {
            return null;
        }

        // Get player eye position and look direction
        TransformComponent playerTransform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (playerTransform == null) {
            return null;
        }

        Vector3d eyePosition = getPlayerEyePosition(playerTransform);
        Vector3d lookDirection = getPlayerLookDirection(playerTransform);

        OrbitalGlyphEntity closest = null;
        double closestDistance = maxDistance;

        for (OrbitalGlyphEntity orbitalEntity : orbitalEntities) {
            if (orbitalEntity.isDragging()) {
                continue; // Skip entities being dragged
            }

            Ref<EntityStore> entityRef = orbitalEntity.getEntityRef();
            if (entityRef == null) {
                continue;
            }

            TransformComponent glyphTransform = store.getComponent(entityRef, TransformComponent.getComponentType());
            if (glyphTransform == null) {
                continue;
            }

            Vector3d glyphPosition = glyphTransform.getPosition();

            // Check ray-sphere intersection
            double hitDistance = rayIntersectsSphere(eyePosition, lookDirection, glyphPosition, DEFAULT_GLYPH_HIT_RADIUS);
            if (hitDistance >= 0 && hitDistance < closestDistance) {
                closest = orbitalEntity;
                closestDistance = hitDistance;
            }
        }

        return closest;
    }

    /**
     * Get the player's eye position (position + eye height offset).
     */
    public static Vector3d getPlayerEyePosition(TransformComponent transform) {
        Vector3d pos = transform.getPosition();
        // Typical eye height is around 1.62 blocks for players
        return new Vector3d(pos.x, pos.y + 1.62, pos.z);
    }

    /**
     * Get the player's look direction from rotation.
     * Rotation is stored as (yaw, pitch, roll) in degrees.
     */
    public static Vector3d getPlayerLookDirection(TransformComponent transform) {
        // Get rotation in degrees
        float yaw = transform.getRotation().y;
        float pitch = transform.getRotation().x;

        // Convert to radians
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        // Calculate direction vector
        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);

        return new Vector3d(x, y, z).normalize();
    }

    /**
     * Check if a ray intersects a sphere.
     *
     * @param rayOrigin The ray origin
     * @param rayDirection The ray direction (normalized)
     * @param sphereCenter The sphere center
     * @param sphereRadius The sphere radius
     * @return The distance to intersection, or -1 if no intersection
     */
    public static double rayIntersectsSphere(Vector3d rayOrigin, Vector3d rayDirection,
                                             Vector3d sphereCenter, float sphereRadius) {
        // Vector from ray origin to sphere center
        double ocX = sphereCenter.x - rayOrigin.x;
        double ocY = sphereCenter.y - rayOrigin.y;
        double ocZ = sphereCenter.z - rayOrigin.z;

        // Project OC onto ray direction
        double tca = ocX * rayDirection.x + ocY * rayDirection.y + ocZ * rayDirection.z;

        // If projection is behind ray origin and center is not inside sphere
        if (tca < 0) {
            // Check if ray origin is inside sphere
            double d2 = ocX * ocX + ocY * ocY + ocZ * ocZ;
            if (d2 > sphereRadius * sphereRadius) {
                return -1;
            }
        }

        // Distance squared from sphere center to closest point on ray
        double d2 = (ocX * ocX + ocY * ocY + ocZ * ocZ) - tca * tca;

        // If distance > radius, no intersection
        double r2 = sphereRadius * sphereRadius;
        if (d2 > r2) {
            return -1;
        }

        // Half-chord distance
        double thc = Math.sqrt(r2 - d2);

        // Return closest intersection distance
        double t0 = tca - thc;
        double t1 = tca + thc;

        if (t0 > 0) {
            return t0;
        }
        if (t1 > 0) {
            return t1;
        }
        return -1;
    }

    /**
     * Check if a ray intersects an axis-aligned bounding box.
     *
     * @param rayOrigin The ray origin
     * @param rayDirection The ray direction (normalized)
     * @param boxMin The minimum corner of the box
     * @param boxMax The maximum corner of the box
     * @return The distance to intersection, or -1 if no intersection
     */
    public static double rayIntersectsAABB(Vector3d rayOrigin, Vector3d rayDirection,
                                           Vector3d boxMin, Vector3d boxMax) {
        double tMin = Double.NEGATIVE_INFINITY;
        double tMax = Double.POSITIVE_INFINITY;

        // X axis
        if (Math.abs(rayDirection.x) > 1e-8) {
            double t1 = (boxMin.x - rayOrigin.x) / rayDirection.x;
            double t2 = (boxMax.x - rayOrigin.x) / rayDirection.x;
            if (t1 > t2) { double temp = t1; t1 = t2; t2 = temp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
        } else if (rayOrigin.x < boxMin.x || rayOrigin.x > boxMax.x) {
            return -1;
        }

        // Y axis
        if (Math.abs(rayDirection.y) > 1e-8) {
            double t1 = (boxMin.y - rayOrigin.y) / rayDirection.y;
            double t2 = (boxMax.y - rayOrigin.y) / rayDirection.y;
            if (t1 > t2) { double temp = t1; t1 = t2; t2 = temp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
        } else if (rayOrigin.y < boxMin.y || rayOrigin.y > boxMax.y) {
            return -1;
        }

        // Z axis
        if (Math.abs(rayDirection.z) > 1e-8) {
            double t1 = (boxMin.z - rayOrigin.z) / rayDirection.z;
            double t2 = (boxMax.z - rayOrigin.z) / rayDirection.z;
            if (t1 > t2) { double temp = t1; t1 = t2; t2 = temp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
        } else if (rayOrigin.z < boxMin.z || rayOrigin.z > boxMax.z) {
            return -1;
        }

        if (tMin > tMax || tMax < 0) {
            return -1;
        }

        return tMin > 0 ? tMin : tMax;
    }

    /**
     * Calculate a 3D position from screen coordinates along the player's view ray.
     *
     * @param playerTransform The player's transform
     * @param distance Distance along the ray
     * @return The 3D position
     */
    public static Vector3d getPointAlongLookRay(TransformComponent playerTransform, float distance) {
        Vector3d eyePos = getPlayerEyePosition(playerTransform);
        Vector3d lookDir = getPlayerLookDirection(playerTransform);

        return new Vector3d(
                eyePos.x + lookDir.x * distance,
                eyePos.y + lookDir.y * distance,
                eyePos.z + lookDir.z * distance
        );
    }
}
