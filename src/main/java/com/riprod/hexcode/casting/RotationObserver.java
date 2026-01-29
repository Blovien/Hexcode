package com.riprod.hexcode.casting;

import java.util.List;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.entity.HexNodeEntity;
import com.riprod.hexcode.entity.OrbitalElement;
import com.riprod.hexcode.hex.HexNode;
import com.riprod.hexcode.math.GlyphRotation;

/**
 * Detects drop targets for glyph/hex drag-and-drop operations using angular distance.
 *
 * <p>Uses angular distance comparison to determine what orbital element the player
 * is looking at. For HexNodeEntity targets, provides nested node detection using
 * the HexNode's findDeepestAt() method for precise selection within composed hexes.
 *
 * <p>Uses HeadRotation component for accurate look direction
 * (not TransformComponent which only stores body rotation).
 */
public class RotationObserver {

    // Cached component types for performance
    private static volatile ComponentType<EntityStore, TransformComponent> cachedTransformType;

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
     * Result for HexNodeEntity targets with specific node information.
     * Uses the unified HexNode system for nested selection.
     */
    public static class NodeDropTarget extends DropTarget {
        /** The HexNodeEntity that was matched */
        public final HexNodeEntity entity;

        /** The specific HexNode within the tree that was targeted (deepest match) */
        public final HexNode targetNode;

        public NodeDropTarget(HexNodeEntity entity, HexNode targetNode,
                              GlyphRotation lookRotation, float angularDistance) {
            super(entity, lookRotation, angularDistance);
            this.entity = entity;
            this.targetNode = targetNode;
        }
    }

    private static ComponentType<EntityStore, TransformComponent> getTransformType() {
        if (cachedTransformType == null) {
            cachedTransformType = TransformComponent.getComponentType();
        }
        return cachedTransformType;
    }

    /**
     * Find the orbital element the player is looking at.
     *
     * <p>Excludes elements that are currently being dragged.
     * Returns the closest angular match within tolerance.
     *
     * <p>For HexNodeEntity targets, returns a NodeDropTarget with the deepest
     * matching node within the hex tree.
     *
     * @param store     The entity store
     * @param playerRef The player entity reference
     * @param elements  List of orbital elements to check (accepts List of any OrbitalElement subtype)
     * @return DropTarget (or NodeDropTarget for HexNodeEntity), or null if nothing matched
     */
    public DropTarget findDropTarget(Store<EntityStore> store, Ref<EntityStore> playerRef,
            List<? extends OrbitalElement> elements) {
        if (elements == null || elements.isEmpty()) {
            return null;
        }

        GlyphRotation lookRotation = getPlayerLookRotation(store, playerRef);
        if (lookRotation == null) {
            return null;
        }

        return findDropTargetAtRotation(elements, lookRotation);
    }

    /**
     * Find the orbital element at a specific rotation.
     *
     * @param elements     List of orbital elements to check (accepts List of any OrbitalElement subtype)
     * @param lookRotation The rotation to test against
     * @return DropTarget (or NodeDropTarget for HexNodeEntity), or null if nothing matched
     */
    public DropTarget findDropTargetAtRotation(List<? extends OrbitalElement> elements, GlyphRotation lookRotation) {
        if (elements == null || elements.isEmpty() || lookRotation == null) {
            return null;
        }

        float userYaw = lookRotation.getYaw();
        float userPitch = lookRotation.getPitch();

        OrbitalElement closestElement = null;
        HexNode deepestNode = null;
        float closestAngularDistance = Float.MAX_VALUE;

        for (int i = 0, size = elements.size(); i < size; i++) {
            OrbitalElement element = elements.get(i);

            // Skip elements being dragged
            if (element.isDragging()) {
                continue;
            }

            // For HexNodeEntity, use the nested hit testing
            if (element instanceof HexNodeEntity) {
                HexNodeEntity hexNodeEntity = (HexNodeEntity) element;
                HexNode rootNode = hexNodeEntity.getNode();

                // Use HexNode's findDeepestAt for precise nested selection
                HexNode hitNode = rootNode.findDeepestAt(userYaw, userPitch);
                if (hitNode != null) {
                    // Calculate angular distance to the hit node
                    float angularDistance = HexNode.angularDistance(
                            hitNode.getAbsoluteYaw(), hitNode.getAbsolutePitch(),
                            userYaw, userPitch
                    );

                    if (angularDistance < closestAngularDistance) {
                        closestElement = element;
                        deepestNode = hitNode;
                        closestAngularDistance = angularDistance;
                    }
                }
            } else {
                // For other OrbitalElement types, use simple rotation check
                GlyphRotation elementRotation = element.getRotation();
                if (elementRotation == null) {
                    continue;
                }

                float angularDistance = lookRotation.angularDistanceTo(elementRotation);
                float tolerance = element.getSelectionTolerance();

                if (angularDistance <= tolerance && angularDistance < closestAngularDistance) {
                    closestElement = element;
                    deepestNode = null; // Not a HexNodeEntity
                    closestAngularDistance = angularDistance;
                }
            }
        }

        if (closestElement == null) {
            return null;
        }

        // Return NodeDropTarget for HexNodeEntity, regular DropTarget otherwise
        if (closestElement instanceof HexNodeEntity && deepestNode != null) {
            return new NodeDropTarget(
                    (HexNodeEntity) closestElement,
                    deepestNode,
                    lookRotation,
                    closestAngularDistance
            );
        }

        return new DropTarget(closestElement, lookRotation, closestAngularDistance);
    }

    /**
     * Find the deepest node in a HexNodeEntity at the player's look direction.
     *
     * <p>This is a convenience method that extracts just the node targeting
     * without needing to check all orbital elements.
     *
     * @param store         The entity store
     * @param playerRef     The player entity reference
     * @param hexNodeEntity The HexNodeEntity to search within
     * @return NodeDropTarget with the deepest matching node, or null if not looking at it
     */
    public NodeDropTarget findNodeDropTarget(Store<EntityStore> store, Ref<EntityStore> playerRef,
                                              HexNodeEntity hexNodeEntity) {
        GlyphRotation lookRotation = getPlayerLookRotation(store, playerRef);
        if (lookRotation == null) {
            return null;
        }

        return findNodeDropTargetAtRotation(hexNodeEntity, lookRotation);
    }

    /**
     * Find the deepest node in a HexNodeEntity at a specific rotation.
     *
     * @param hexNodeEntity The HexNodeEntity to search within
     * @param lookRotation  The rotation to test against
     * @return NodeDropTarget with the deepest matching node, or null if not within bounds
     */
    public NodeDropTarget findNodeDropTargetAtRotation(HexNodeEntity hexNodeEntity, GlyphRotation lookRotation) {
        if (hexNodeEntity == null || lookRotation == null) {
            return null;
        }

        float userYaw = lookRotation.getYaw();
        float userPitch = lookRotation.getPitch();

        HexNode rootNode = hexNodeEntity.getNode();
        HexNode hitNode = rootNode.findDeepestAt(userYaw, userPitch);

        if (hitNode == null) {
            return null;
        }

        float angularDistance = HexNode.angularDistance(
                hitNode.getAbsoluteYaw(), hitNode.getAbsolutePitch(),
                userYaw, userPitch
        );

        return new NodeDropTarget(hexNodeEntity, hitNode, lookRotation, angularDistance);
    }

    /**
     * Check if the player is looking at a specific orbital element.
     *
     * @param store     The entity store
     * @param playerRef The player entity reference
     * @param element   The element to check
     * @return true if the player is looking at the element (within its tolerance)
     */
    public boolean isLookingAt(Store<EntityStore> store, Ref<EntityStore> playerRef,
            OrbitalElement element) {
        GlyphRotation lookRotation = getPlayerLookRotation(store, playerRef);
        if (lookRotation == null) {
            return false;
        }

        // For HexNodeEntity, use the nested hit testing
        if (element instanceof HexNodeEntity) {
            HexNodeEntity hexNodeEntity = (HexNodeEntity) element;
            HexNode hitNode = hexNodeEntity.getNode().findDeepestAt(
                    lookRotation.getYaw(), lookRotation.getPitch()
            );
            return hitNode != null;
        }

        // For other elements, use simple rotation check
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
     * @param store     The entity store
     * @param playerRef The player entity reference
     * @return The player's look rotation, or null if unavailable
     */
    public GlyphRotation getPlayerLookRotation(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        ComponentType<EntityStore, HeadRotation> headRotationType = HeadRotation.getComponentType();
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
     * @param store              The entity store
     * @param playerRef          The player entity reference
     * @param elements           List of orbital elements to check
     * @param maxAngularDistance Maximum angular distance to consider (in degrees)
     * @return DropTarget with the closest element, or null if none within max distance
     */
    public DropTarget findClosestElement(Store<EntityStore> store, Ref<EntityStore> playerRef,
            List<? extends OrbitalElement> elements, float maxAngularDistance) {
        if (elements == null || elements.isEmpty()) {
            return null;
        }

        GlyphRotation lookRotation = getPlayerLookRotation(store, playerRef);
        if (lookRotation == null) {
            return null;
        }

        float userYaw = lookRotation.getYaw();
        float userPitch = lookRotation.getPitch();

        OrbitalElement closestElement = null;
        HexNode deepestNode = null;
        float closestAngularDistance = maxAngularDistance;

        for (OrbitalElement element : elements) {
            if (element.isDragging()) {
                continue;
            }

            // For HexNodeEntity, use nested hit testing
            if (element instanceof HexNodeEntity) {
                HexNodeEntity hexNodeEntity = (HexNodeEntity) element;
                HexNode rootNode = hexNodeEntity.getNode();

                HexNode hitNode = rootNode.findDeepestAt(userYaw, userPitch);
                if (hitNode != null) {
                    float angularDistance = HexNode.angularDistance(
                            hitNode.getAbsoluteYaw(), hitNode.getAbsolutePitch(),
                            userYaw, userPitch
                    );

                    if (angularDistance < closestAngularDistance) {
                        closestElement = element;
                        deepestNode = hitNode;
                        closestAngularDistance = angularDistance;
                    }
                }
            } else {
                GlyphRotation elementRotation = element.getRotation();
                if (elementRotation == null) {
                    continue;
                }

                float angularDistance = lookRotation.angularDistanceTo(elementRotation);
                if (angularDistance < closestAngularDistance) {
                    closestElement = element;
                    deepestNode = null;
                    closestAngularDistance = angularDistance;
                }
            }
        }

        if (closestElement == null) {
            return null;
        }

        if (closestElement instanceof HexNodeEntity && deepestNode != null) {
            return new NodeDropTarget(
                    (HexNodeEntity) closestElement,
                    deepestNode,
                    lookRotation,
                    closestAngularDistance
            );
        }

        return new DropTarget(closestElement, lookRotation, closestAngularDistance);
    }
}
