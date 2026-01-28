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
import com.riprod.hexcode.math.GlyphRotation;
import com.riprod.hexcode.util.RotationMath;

/**
 * Detects drop targets for glyph/hex drag-and-drop operations using angular distance.
 *
 * <p>Uses angular distance comparison to determine what orbital element the player
 * is looking at. Selection uses a 5-degree tolerance for single glyphs, 7-degree
 * for composed hexes.
 *
 * <p>For HexEntity targets, provides tier-based detection where outer tiers (larger
 * angular margins) are matched first, allowing insertion at specific tree levels.
 *
 * <p>Uses HeadRotation component for accurate look direction
 * (not TransformComponent which only stores body rotation).
 */
public class RotationObserver {

    // Cached component types for performance
    private static volatile ComponentType<EntityStore, TransformComponent> cachedTransformType;
    private static volatile ComponentType<EntityStore, HeadRotation> cachedHeadRotationType;

    /**
     * Result of finding a drop target.
     */
    public static class DropTarget {
        /** The orbital element that was matched */
        public final OrbitalElement target;

        /** The rotation where the player was looking */
        public final GlyphRotation lookRotation;

        /** Angular distance from player look direction to target (in degrees) */
        public final float angularDistance;

        public DropTarget(OrbitalElement target, GlyphRotation lookRotation, float angularDistance) {
            this.target = target;
            this.lookRotation = lookRotation;
            this.angularDistance = angularDistance;
        }
    }

    /**
     * Extended result for hex targets with tier information.
     */
    public static class HexDropTarget extends DropTarget {
        /** The tier level that was matched (0 = outermost/root) */
        public final int tierLevel;

        /** The HexNode corresponding to the matched tier */
        public final HexNode targetNode;

        public HexDropTarget(OrbitalElement target, GlyphRotation lookRotation, float angularDistance,
                            int tierLevel, HexNode targetNode) {
            super(target, lookRotation, angularDistance);
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
     * Find the orbital element the player is looking at.
     *
     * <p>Excludes elements that are currently being dragged.
     * Returns the closest angular match within tolerance.
     *
     * @param store The entity store
     * @param playerRef The player entity reference
     * @param elements List of orbital elements to check
     * @return DropTarget with match information, or null if nothing matched
     */
    public DropTarget findDropTarget(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                     List<OrbitalElement> elements) {
        if (elements == null || elements.isEmpty()) {
            return null;
        }

        ComponentType<EntityStore, HeadRotation> headRotationType = getHeadRotationType();
        if (headRotationType == null) {
            return null;
        }

        // Get player head rotation (where they're actually looking)
        HeadRotation headRotation = store.getComponent(playerRef, headRotationType);
        if (headRotation == null) {
            return null;
        }

        // Convert look direction to GlyphRotation
        Vector3d lookDirection = headRotation.getDirection();
        GlyphRotation lookRotation = GlyphRotation.fromDirection(lookDirection);

        OrbitalElement closestElement = null;
        float closestAngularDistance = Float.MAX_VALUE;

        for (int i = 0, size = elements.size(); i < size; i++) {
            OrbitalElement element = elements.get(i);

            // Skip elements being dragged
            if (element.isDragging()) {
                continue;
            }

            GlyphRotation elementRotation = element.getRotation();
            if (elementRotation == null) {
                continue;
            }

            // Calculate angular distance
            float angularDistance = lookRotation.angularDistanceTo(elementRotation);

            // Check if within element's selection tolerance
            float tolerance = element.getSelectionTolerance();
            if (angularDistance <= tolerance && angularDistance < closestAngularDistance) {
                closestElement = element;
                closestAngularDistance = angularDistance;
            }
        }

        if (closestElement == null) {
            return null;
        }

        // If it's a HexEntity, return a HexDropTarget with tier info
        if (closestElement instanceof HexEntity) {
            return findHexDropTarget(store, playerRef, (HexEntity) closestElement, lookRotation);
        }

        return new DropTarget(closestElement, lookRotation, closestAngularDistance);
    }

    /**
     * Find which tier of a hex the player is looking at.
     *
     * <p>Tests angular distance against each tier's tolerance, starting from outermost.
     * Returns the innermost tier that matches (closest angular distance), since
     * more precise aiming should select inner tiers.
     *
     * <p>Tier 0 = outermost (largest tolerance, root node)
     * Tier N = innermost (smallest tolerance, deepest leaf)
     *
     * @param store The entity store
     * @param playerRef The player entity reference
     * @param hexEntity The hex entity to test
     * @param lookRotation The player's look rotation (if null, will be computed)
     * @return HexDropTarget with tier and node info, or null if no match
     */
    public HexDropTarget findHexDropTarget(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                           HexEntity hexEntity, GlyphRotation lookRotation) {
        // Get look rotation if not provided
        if (lookRotation == null) {
            ComponentType<EntityStore, HeadRotation> headRotationType = getHeadRotationType();
            if (headRotationType == null) {
                return null;
            }

            HeadRotation headRotation = store.getComponent(playerRef, headRotationType);
            if (headRotation == null) {
                return null;
            }

            lookRotation = GlyphRotation.fromDirection(headRotation.getDirection());
        }

        // Get hex rotation
        GlyphRotation hexRotation = hexEntity.getRotation();
        if (hexRotation == null) {
            return null;
        }

        // Calculate angular distance to hex center
        float angularDistance = lookRotation.angularDistanceTo(hexRotation);

        // Get tier tolerances
        float[] tierTolerances = hexEntity.getTierTolerances();
        if (tierTolerances == null || tierTolerances.length == 0) {
            return null;
        }

        // Find the innermost (smallest tolerance) tier that matches
        // Check from innermost to outermost so we get the most specific match
        int matchedTier = -1;
        for (int tier = tierTolerances.length - 1; tier >= 0; tier--) {
            if (angularDistance <= tierTolerances[tier]) {
                matchedTier = tier;
                break; // Found innermost match
            }
        }

        if (matchedTier < 0) {
            return null; // No tier matched
        }

        // Find the corresponding HexNode
        HexNode targetNode = hexEntity.getNodeAtTier(matchedTier);

        return new HexDropTarget(hexEntity, lookRotation, angularDistance, matchedTier, targetNode);
    }

    /**
     * Check if the player is looking at a specific orbital element.
     *
     * @param store The entity store
     * @param playerRef The player entity reference
     * @param element The element to check
     * @return true if the player is looking at the element (within its tolerance)
     */
    public boolean isLookingAt(Store<EntityStore> store, Ref<EntityStore> playerRef,
                               OrbitalElement element) {
        ComponentType<EntityStore, HeadRotation> headRotationType = getHeadRotationType();
        if (headRotationType == null) {
            return false;
        }

        HeadRotation headRotation = store.getComponent(playerRef, headRotationType);
        if (headRotation == null) {
            return false;
        }

        GlyphRotation lookRotation = GlyphRotation.fromDirection(headRotation.getDirection());
        GlyphRotation elementRotation = element.getRotation();
        if (elementRotation == null) {
            return false;
        }

        float angularDistance = lookRotation.angularDistanceTo(elementRotation);
        return angularDistance <= element.getSelectionTolerance();
    }

    /**
     * Get the player's current look rotation.
     *
     * @param store The entity store
     * @param playerRef The player entity reference
     * @return The player's look rotation, or null if unavailable
     */
    public GlyphRotation getPlayerLookRotation(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        ComponentType<EntityStore, HeadRotation> headRotationType = getHeadRotationType();
        if (headRotationType == null) {
            return null;
        }

        HeadRotation headRotation = store.getComponent(playerRef, headRotationType);
        if (headRotation == null) {
            return null;
        }

        return GlyphRotation.fromDirection(headRotation.getDirection());
    }

    /**
     * Find the element with the smallest angular distance to the player's look direction.
     *
     * <p>Unlike {@link #findDropTarget}, this does NOT check tolerance - it returns
     * the closest element regardless of distance. Useful for drop detection where
     * you want to find the "best" target even if slightly outside tolerance.
     *
     * @param store The entity store
     * @param playerRef The player entity reference
     * @param elements List of orbital elements to check
     * @param maxAngularDistance Maximum angular distance to consider (in degrees)
     * @return DropTarget with the closest element, or null if none within max distance
     */
    public DropTarget findClosestElement(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                         List<OrbitalElement> elements, float maxAngularDistance) {
        if (elements == null || elements.isEmpty()) {
            return null;
        }

        ComponentType<EntityStore, HeadRotation> headRotationType = getHeadRotationType();
        if (headRotationType == null) {
            return null;
        }

        HeadRotation headRotation = store.getComponent(playerRef, headRotationType);
        if (headRotation == null) {
            return null;
        }

        GlyphRotation lookRotation = GlyphRotation.fromDirection(headRotation.getDirection());

        OrbitalElement closestElement = null;
        float closestAngularDistance = maxAngularDistance;

        for (OrbitalElement element : elements) {
            if (element.isDragging()) {
                continue;
            }

            GlyphRotation elementRotation = element.getRotation();
            if (elementRotation == null) {
                continue;
            }

            float angularDistance = lookRotation.angularDistanceTo(elementRotation);
            if (angularDistance < closestAngularDistance) {
                closestElement = element;
                closestAngularDistance = angularDistance;
            }
        }

        if (closestElement == null) {
            return null;
        }

        if (closestElement instanceof HexEntity) {
            return findHexDropTarget(store, playerRef, (HexEntity) closestElement, lookRotation);
        }

        return new DropTarget(closestElement, lookRotation, closestAngularDistance);
    }
}
