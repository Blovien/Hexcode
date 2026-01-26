package com.riprod.hexcode.casting;

import java.util.List;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.casting.styles.OrbitalElement;
import com.riprod.hexcode.entity.HexEntity;
import com.riprod.hexcode.hex.HexNode;
import com.riprod.hexcode.util.RaycastUtil;

/**
 * Detects drop targets for glyph/hex drag-and-drop operations.
 *
 * Uses ray-sphere intersection to determine what orbital element the player
 * is aiming at. For HexEntity targets, provides tier-based detection where
 * outer shells (larger radii) are hit first, allowing insertion at specific
 * tree levels.
 *
 * <p>Uses HeadRotation component for accurate look direction
 * (not TransformComponent which only stores body rotation).
 */
public class IntersectionObserver {

    // Default hit radius for single glyphs
    private static final float DEFAULT_GLYPH_HIT_RADIUS = 0.4f;

    // Default eye height for players
    private static final double DEFAULT_EYE_HEIGHT = 1.62;

    // Cached component types for performance
    private static volatile ComponentType<EntityStore, TransformComponent> cachedTransformType;
    private static volatile ComponentType<EntityStore, HeadRotation> cachedHeadRotationType;

    /**
     * Result of finding a drop target.
     */
    public static class DropTarget {
        /** The orbital element that was hit */
        public final OrbitalElement target;

        /** The point where the ray hit the target */
        public final Vector3d hitPoint;

        /** Distance from player eye to hit point */
        public final double distance;

        public DropTarget(OrbitalElement target, Vector3d hitPoint, double distance) {
            this.target = target;
            this.hitPoint = hitPoint;
            this.distance = distance;
        }
    }

    /**
     * Extended result for hex targets with tier information.
     */
    public static class HexDropTarget extends DropTarget {
        /** The tier level that was hit (0 = outermost/root) */
        public final int tierLevel;

        /** The HexNode corresponding to the hit tier */
        public final HexNode targetNode;

        public HexDropTarget(OrbitalElement target, Vector3d hitPoint, double distance,
                            int tierLevel, HexNode targetNode) {
            super(target, hitPoint, distance);
            this.tierLevel = tierLevel;
            this.targetNode = targetNode;
        }
    }

    private static ComponentType<EntityStore, TransformComponent> getTransformType() {
        if (cachedTransformType == null) {
            cachedTransformType = TransformComponent.getComponentType();
        }
        return cachedTransformType;
    }

    private static ComponentType<EntityStore, HeadRotation> getHeadRotationType() {
        if (cachedHeadRotationType == null) {
            cachedHeadRotationType = HeadRotation.getComponentType();
        }
        return cachedHeadRotationType;
    }

    /**
     * Find the orbital element the player is aiming at.
     *
     * <p>Excludes elements that are currently being dragged.
     * Returns the closest hit element within maxDistance.
     *
     * @param store The entity store
     * @param playerRef The player entity reference
     * @param elements List of orbital elements to check
     * @param maxDistance Maximum raycast distance
     * @return DropTarget with hit information, or null if nothing hit
     */
    public DropTarget findDropTarget(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                     List<OrbitalElement> elements, float maxDistance) {
        if (elements == null || elements.isEmpty()) {
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
        Vector3d lookDirection = headRotation.getDirection();

        OrbitalElement closestElement = null;
        double closestDistance = maxDistance;
        Vector3d closestHitPoint = null;

        for (int i = 0, size = elements.size(); i < size; i++) {
            OrbitalElement element = elements.get(i);

            // Skip elements being dragged
            if (element.isDragging()) {
                continue;
            }

            Ref<EntityStore> entityRef = element.getEntityRef();
            if (entityRef == null) {
                continue;
            }

            TransformComponent elementTransform = store.getComponent(entityRef, transformType);
            if (elementTransform == null) {
                continue;
            }

            Vector3d elementPosition = elementTransform.getPosition();

            // For HexEntity, use the outermost tier radius for initial hit detection
            float hitRadius = DEFAULT_GLYPH_HIT_RADIUS;
            if (element instanceof HexEntity) {
                HexEntity hexEntity = (HexEntity) element;
                float[] tierRadii = hexEntity.getTierRadii();
                if (tierRadii != null && tierRadii.length > 0) {
                    hitRadius = tierRadii[0]; // Outermost tier
                }
            }

            double hitDistance = RaycastUtil.rayIntersectsSphere(
                    eyePosition, lookDirection, elementPosition, hitRadius);

            if (hitDistance >= 0 && hitDistance < closestDistance) {
                closestElement = element;
                closestDistance = hitDistance;
                closestHitPoint = calculateHitPoint(eyePosition, lookDirection, hitDistance);
            }
        }

        if (closestElement == null) {
            return null;
        }

        // If it's a HexEntity, return a HexDropTarget with tier info
        if (closestElement instanceof HexEntity) {
            return findHexDropTarget(store, playerRef, (HexEntity) closestElement);
        }

        return new DropTarget(closestElement, closestHitPoint, closestDistance);
    }

    /**
     * Find which tier of a hex the player is aiming at.
     *
     * <p>Tests intersection with each tier's sphere, starting from outermost.
     * Returns the first (outermost) tier that is hit, since outer shells
     * visually occlude inner ones.
     *
     * <p>Tier 0 = outermost (largest radius, root node)
     * Tier N = innermost (smallest radius, deepest leaf)
     *
     * @param store The entity store
     * @param playerRef The player entity reference
     * @param hexEntity The hex entity to test
     * @return HexDropTarget with tier and node info, or null if no hit
     */
    public HexDropTarget findHexDropTarget(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                           HexEntity hexEntity) {
        ComponentType<EntityStore, TransformComponent> transformType = getTransformType();
        ComponentType<EntityStore, HeadRotation> headRotationType = getHeadRotationType();
        if (transformType == null || headRotationType == null) {
            return null;
        }

        // Get player position and look direction
        TransformComponent playerTransform = store.getComponent(playerRef, transformType);
        if (playerTransform == null) {
            return null;
        }

        HeadRotation headRotation = store.getComponent(playerRef, headRotationType);
        if (headRotation == null) {
            return null;
        }

        Vector3d eyePosition = getPlayerEyePosition(playerTransform);
        Vector3d lookDirection = headRotation.getDirection();

        // Get hex entity position
        Ref<EntityStore> hexRef = hexEntity.getEntityRef();
        if (hexRef == null) {
            return null;
        }

        TransformComponent hexTransform = store.getComponent(hexRef, transformType);
        if (hexTransform == null) {
            return null;
        }

        Vector3d hexPosition = hexTransform.getPosition();

        // Get tier radii
        float[] tierRadii = hexEntity.getTierRadii();
        if (tierRadii == null || tierRadii.length == 0) {
            return null;
        }

        // Test each tier from outermost (index 0) to innermost
        // The first hit is the outermost visible tier
        for (int tier = 0; tier < tierRadii.length; tier++) {
            float tierRadius = tierRadii[tier];

            double hitDistance = RaycastUtil.rayIntersectsSphere(
                    eyePosition, lookDirection, hexPosition, tierRadius);

            if (hitDistance >= 0) {
                // Hit this tier - find the corresponding HexNode
                HexNode targetNode = hexEntity.getNodeAtTier(tier);
                Vector3d hitPoint = calculateHitPoint(eyePosition, lookDirection, hitDistance);

                return new HexDropTarget(hexEntity, hitPoint, hitDistance, tier, targetNode);
            }
        }

        // No tier was hit
        return null;
    }

    /**
     * Check if the player is aiming at a specific orbital element.
     *
     * @param store The entity store
     * @param playerRef The player entity reference
     * @param element The element to check
     * @param maxDistance Maximum raycast distance
     * @return true if the player is aiming at the element
     */
    public boolean isAimingAt(Store<EntityStore> store, Ref<EntityStore> playerRef,
                              OrbitalElement element, float maxDistance) {
        ComponentType<EntityStore, TransformComponent> transformType = getTransformType();
        ComponentType<EntityStore, HeadRotation> headRotationType = getHeadRotationType();
        if (transformType == null || headRotationType == null) {
            return false;
        }

        TransformComponent playerTransform = store.getComponent(playerRef, transformType);
        if (playerTransform == null) {
            return false;
        }

        HeadRotation headRotation = store.getComponent(playerRef, headRotationType);
        if (headRotation == null) {
            return false;
        }

        Ref<EntityStore> entityRef = element.getEntityRef();
        if (entityRef == null) {
            return false;
        }

        TransformComponent elementTransform = store.getComponent(entityRef, transformType);
        if (elementTransform == null) {
            return false;
        }

        Vector3d eyePosition = getPlayerEyePosition(playerTransform);
        Vector3d lookDirection = headRotation.getDirection();
        Vector3d elementPosition = elementTransform.getPosition();

        float hitRadius = DEFAULT_GLYPH_HIT_RADIUS;
        if (element instanceof HexEntity) {
            HexEntity hexEntity = (HexEntity) element;
            float[] tierRadii = hexEntity.getTierRadii();
            if (tierRadii != null && tierRadii.length > 0) {
                hitRadius = tierRadii[0];
            }
        }

        double hitDistance = RaycastUtil.rayIntersectsSphere(
                eyePosition, lookDirection, elementPosition, hitRadius);

        return hitDistance >= 0 && hitDistance <= maxDistance;
    }

    /**
     * Get the player's eye position.
     */
    private Vector3d getPlayerEyePosition(TransformComponent transform) {
        Vector3d pos = transform.getPosition();
        return new Vector3d(pos.x, pos.y + DEFAULT_EYE_HEIGHT, pos.z);
    }

    /**
     * Calculate the hit point along a ray.
     */
    private Vector3d calculateHitPoint(Vector3d origin, Vector3d direction, double distance) {
        return new Vector3d(
                origin.x + direction.x * distance,
                origin.y + direction.y * distance,
                origin.z + direction.z * distance
        );
    }
}
