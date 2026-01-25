package com.riprod.hexcode.util;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.entity.OrbitalGlyphEntity;

import java.util.List;

/**
 * Utility methods for raycasting and glyph hover detection.
 *
 * <p>Performance optimizations:
 * <ul>
 *   <li>Component types are cached lazily to avoid repeated lookups</li>
 *   <li>Debug logging removed for hot path operations</li>
 *   <li>Ray-sphere intersection uses optimized math without allocations</li>
 * </ul>
 */
public class RaycastUtil {
    // Default glyph bounding sphere radius for hit detection
    private static final float DEFAULT_GLYPH_HIT_RADIUS = 0.4f;

    // Default eye height for players (in blocks)
    private static final double DEFAULT_EYE_HEIGHT = 1.62;

    // Cached component types for performance (lazy initialization)
    private static volatile ComponentType<EntityStore, TransformComponent> cachedTransformType;
    private static volatile ComponentType<EntityStore, HeadRotation> cachedHeadRotationType;

    private RaycastUtil() {}

    /**
     * Get the cached TransformComponent type.
     * Thread-safe lazy initialization.
     */
    private static ComponentType<EntityStore, TransformComponent> getTransformType() {
        if (cachedTransformType == null) {
            cachedTransformType = TransformComponent.getComponentType();
        }
        return cachedTransformType;
    }

    /**
     * Get the cached HeadRotation component type.
     * Thread-safe lazy initialization.
     */
    private static ComponentType<EntityStore, HeadRotation> getHeadRotationType() {
        if (cachedHeadRotationType == null) {
            cachedHeadRotationType = HeadRotation.getComponentType();
        }
        return cachedHeadRotationType;
    }

    /**
     * Find the orbital glyph entity that the player is looking at.
     *
     * <p>Performance: O(n) where n = number of orbital entities.
     * Uses cached component types and avoids allocations in hot path.
     *
     * <p>Uses HeadRotation component for accurate look direction
     * (not TransformComponent which only stores body rotation).
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

        ComponentType<EntityStore, TransformComponent> transformType = getTransformType();
        ComponentType<EntityStore, HeadRotation> headRotationType = getHeadRotationType();
        if (transformType == null || headRotationType == null) {
            return null;
        }

        // Get player position
        TransformComponent playerTransform = store.getComponent(playerRef, transformType);
        if (playerTransform == null) {
            return null;
        }

        // Get player head rotation (where they're actually looking)
        HeadRotation headRotation = store.getComponent(playerRef, headRotationType);
        if (headRotation == null) {
            return null;
        }

        Vector3d eyePosition = getPlayerEyePosition(playerTransform);
        // Use HeadRotation.getDirection() for accurate look direction
        Vector3d lookDirection = headRotation.getDirection();

        OrbitalGlyphEntity closest = null;
        double closestDistance = maxDistance;

        for (int i = 0, size = orbitalEntities.size(); i < size; i++) {
            OrbitalGlyphEntity orbitalEntity = orbitalEntities.get(i);

            if (orbitalEntity.isDragging()) {
                continue; // Skip entities being dragged
            }

            Ref<EntityStore> entityRef = orbitalEntity.getEntityRef();
            if (entityRef == null) {
                continue;
            }

            TransformComponent glyphTransform = store.getComponent(entityRef, transformType);
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
     * Get the player's look direction from HeadRotation component.
     *
     * <p>IMPORTANT: Use HeadRotation, NOT TransformComponent.getRotation().
     * TransformComponent stores body rotation, HeadRotation stores where
     * the player is actually looking.
     *
     * @param headRotation The player's head rotation component
     * @return The normalized look direction vector
     */
    public static Vector3d getPlayerLookDirection(HeadRotation headRotation) {
        // HeadRotation has a built-in getDirection() method
        return headRotation.getDirection();
    }

    /**
     * @deprecated Use {@link #getPlayerLookDirection(HeadRotation)} instead.
     * TransformComponent.getRotation() returns body rotation, not head rotation.
     */
    @Deprecated
    public static Vector3d getPlayerLookDirectionFromTransform(TransformComponent transform) {
        // Get rotation in degrees (NOTE: This is BODY rotation, not HEAD)
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
     * Calculate a 3D position along the player's view ray.
     *
     * <p>Uses HeadRotation component for accurate look direction.
     *
     * @param playerTransform The player's transform (for position)
     * @param headRotation The player's head rotation (for look direction)
     * @param distance Distance along the ray
     * @return The 3D position
     */
    public static Vector3d getPointAlongLookRay(TransformComponent playerTransform,
                                                  HeadRotation headRotation, float distance) {
        Vector3d eyePos = getPlayerEyePosition(playerTransform);
        Vector3d lookDir = headRotation.getDirection();

        return new Vector3d(
                eyePos.x + lookDir.x * distance,
                eyePos.y + lookDir.y * distance,
                eyePos.z + lookDir.z * distance
        );
    }

    /**
     * @deprecated Use {@link #getPointAlongLookRay(TransformComponent, HeadRotation, float)} instead.
     * This version uses body rotation instead of head rotation.
     */
    @Deprecated
    public static Vector3d getPointAlongLookRayFromTransform(TransformComponent playerTransform, float distance) {
        Vector3d eyePos = getPlayerEyePosition(playerTransform);
        Vector3d lookDir = getPlayerLookDirectionFromTransform(playerTransform);

        return new Vector3d(
                eyePos.x + lookDir.x * distance,
                eyePos.y + lookDir.y * distance,
                eyePos.z + lookDir.z * distance
        );
    }

    /**
     * Calculate the drag position for a glyph based on player's gaze.
     *
     * <p>This method calculates where a dragged glyph should appear
     * along the player's view ray at a fixed distance. Used for real-time
     * glyph movement during drag operations.
     *
     * <p>Uses HeadRotation component for accurate look direction
     * (not TransformComponent which only stores body rotation).
     *
     * @param store The entity store
     * @param playerRef The player entity reference
     * @param dragDistance Distance from player eye to glyph during drag
     * @return The drag position, or null if components unavailable
     */
    public static Vector3d calculateDragPosition(Store<EntityStore> store, Ref<EntityStore> playerRef, float dragDistance) {
        ComponentType<EntityStore, TransformComponent> transformType = getTransformType();
        ComponentType<EntityStore, HeadRotation> headRotationType = getHeadRotationType();
        if (transformType == null || headRotationType == null) {
            return null;
        }

        TransformComponent playerTransform = store.getComponent(playerRef, transformType);
        if (playerTransform == null) {
            return null;
        }

        HeadRotation headRotation = store.getComponent(playerRef, headRotationType);
        if (headRotation == null) {
            return null;
        }

        return getPointAlongLookRay(playerTransform, headRotation, dragDistance);
    }

    /**
     * Calculate drag position and write to existing Vector3d to avoid allocation.
     *
     * <p>Performance optimization: Reuses the output vector instead of allocating.
     *
     * <p>Uses HeadRotation component for accurate look direction
     * (not TransformComponent which only stores body rotation).
     *
     * @param store The entity store
     * @param playerRef The player entity reference
     * @param dragDistance Distance from player eye to glyph during drag
     * @param out The output vector to write position to
     * @return true if position was calculated, false if components unavailable
     */
    public static boolean calculateDragPositionNoAlloc(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                                        float dragDistance, Vector3d out) {
        ComponentType<EntityStore, TransformComponent> transformType = getTransformType();
        ComponentType<EntityStore, HeadRotation> headRotationType = getHeadRotationType();
        if (transformType == null || headRotationType == null) {
            return false;
        }

        TransformComponent playerTransform = store.getComponent(playerRef, transformType);
        if (playerTransform == null) {
            return false;
        }

        // IMPORTANT: Use HeadRotation for look direction, NOT TransformComponent.getRotation()
        HeadRotation headRotation = store.getComponent(playerRef, headRotationType);
        if (headRotation == null) {
            return false;
        }

        Vector3d pos = playerTransform.getPosition();

        // Get look direction from HeadRotation (where player is actually looking)
        Vector3d lookDir = headRotation.getDirection();

        // Eye position + look direction * distance
        out.x = pos.x + lookDir.x * dragDistance;
        out.y = pos.y + DEFAULT_EYE_HEIGHT + lookDir.y * dragDistance;
        out.z = pos.z + lookDir.z * dragDistance;

        return true;
    }

    /**
     * Get player transform using cached component type.
     *
     * @param store The entity store
     * @param playerRef The player entity reference
     * @return The transform component, or null if not found
     */
    public static TransformComponent getPlayerTransform(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        ComponentType<EntityStore, TransformComponent> transformType = getTransformType();
        if (transformType == null) {
            return null;
        }
        return store.getComponent(playerRef, transformType);
    }

    /**
     * Get player head rotation using cached component type.
     *
     * <p>Use this to get the player's look direction, NOT TransformComponent.getRotation()
     * which returns body rotation instead.
     *
     * @param store The entity store
     * @param playerRef The player entity reference
     * @return The head rotation component, or null if not found
     */
    public static HeadRotation getPlayerHeadRotation(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        ComponentType<EntityStore, HeadRotation> headRotationType = getHeadRotationType();
        if (headRotationType == null) {
            return null;
        }
        return store.getComponent(playerRef, headRotationType);
    }
}
