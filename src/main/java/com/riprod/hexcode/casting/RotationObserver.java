package com.riprod.hexcode.casting;

import java.util.List;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.entity.HexNodeEntity;
import com.riprod.hexcode.hex.HexNode;
import com.riprod.hexcode.math.GlyphRotation;

/**
 * Detects drop targets for glyph/hex drag-and-drop operations using angular
 * distance.
 *
 * <p>
 * Uses angular distance comparison to determine what orbital element the player
 * is looking at. For HexNodeEntity targets, provides nested node detection
 * using
 * the HexNode's findDeepestAt() method for precise selection within composed
 * hexes.
 *
 * <p>
 * Uses HeadRotation component for accurate look direction
 * (not TransformComponent which only stores body rotation).
 */
public class RotationObserver {
    private HytaleLogger LOGGER = HytaleLogger.getLogger();

    /**
     * Result for HexNodeEntity targets with specific node information.
     * Uses the unified HexNode system for nested selection.
     */
    public static class NodeDropTarget {
        /** The HexNodeEntity that was matched */
        public final HexNodeEntity entity;

        /** The specific HexNode within the tree that was targeted (deepest match) */
        public final HexNode targetNode;

        /** The rotation where the player was looking */
        public final GlyphRotation lookRotation;

        /** Angular distance from player look direction to target (in degrees) */
        public final float angularDistance;

        public NodeDropTarget(HexNodeEntity entity, HexNode targetNode,
                GlyphRotation lookRotation, float angularDistance) {
            this.lookRotation = lookRotation;
            this.angularDistance = angularDistance;
            this.entity = entity;
            this.targetNode = targetNode;
        }
    }

    /**
     * Find the orbital element the player is looking at.
     *
     * <p>
     * Excludes elements that are currently being dragged.
     * Returns the closest angular match within tolerance.
     *
     * <p>
     * For HexNodeEntity targets, returns a NodeDropTarget with the deepest
     * matching node within the hex tree.
     *
     * @param store     The entity store
     * @param playerRef The player entity reference
     * @param elements  List of orbital elements to check (accepts List of any
     *                  HexNodeEntity subtype)
     * @return DropTarget (or NodeDropTarget for HexNodeEntity), or null if nothing
     *         matched
     */
    public NodeDropTarget findDropTarget(Store<EntityStore> store, Ref<EntityStore> playerRef,
            List<HexNodeEntity> elements) {
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
     * @param elements     List of orbital elements to check (accepts List of any
     *                     OrbitalElement subtype)
     * @param lookRotation The rotation to test against
     * @return DropTarget (or NodeDropTarget for HexNodeEntity), or null if nothing
     *         matched
     */
    public NodeDropTarget findDropTargetAtRotation(List<HexNodeEntity> elements, GlyphRotation lookRotation) {
        if (elements == null || elements.isEmpty() || lookRotation == null) {
            return null;
        }

        float userYaw = lookRotation.getYaw();
        float userPitch = lookRotation.getPitch();

        HexNodeEntity closestElement = null;
        HexNode deepestNode = null;
        float closestAngularDistance = Float.MAX_VALUE;

        for (int i = 0, size = elements.size(); i < size; i++) {
            HexNodeEntity element = elements.get(i);

            // Skip elements being dragged
            if (element.isDragging()) {
                continue;
            }

            // For HexNodeEntity, use the nested hit testing
            HexNode rootNode = element.getNode();

            // early check to see if the user is even looking at the root node
            if (!rootNode.isWithinBounds(userYaw, userPitch)) {
                continue;
            }

            // Use HexNode's findDeepestAt for precise nested selection
            HexNode hitNode = rootNode.findDeepestAt(userYaw, userPitch);
            if (hitNode != null) {
                // Calculate angular distance to the hit node
                float angularDistance = HexNode.angularDistance(
                        hitNode.getAbsoluteYaw(), hitNode.getAbsolutePitch(),
                        userYaw, userPitch);

                if (angularDistance < closestAngularDistance) {
                    closestElement = element;
                    deepestNode = hitNode;
                    closestAngularDistance = angularDistance;
                }
            }
        }

        if (closestElement == null) {
            return null;
        }

        return new NodeDropTarget(
                closestElement,
                deepestNode,
                lookRotation,
                closestAngularDistance);
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
}
