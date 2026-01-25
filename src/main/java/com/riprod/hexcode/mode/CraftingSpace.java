package com.riprod.hexcode.mode;

import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.hex.HexNode;
import com.riprod.hexcode.util.HexMathUtil;
import com.riprod.hexcode.util.RaycastUtil;
import com.hypixel.hytale.math.vector.Vector3d;

import javax.annotation.Nullable;

/**
 * Represents the 3D crafting space where hexes are composed.
 *
 * Located ~2 blocks in front of player at eye level.
 * Shows the composed Hex structure with shell visuals.
 *
 * Supports gaze-based targeting for drag and drop operations:
 * - Players look at glyphs to select them as drop targets
 * - Dropping on center of a glyph wraps it (Hexed)
 * - Dropping on edge adds as sibling (Chained)
 */
public class CraftingSpace {
    private final Vector3d playerPosition;
    private final Vector3d playerLookDirection;
    private final float distanceFromPlayer;

    // Visual configuration
    private static final float NODE_SPACING = 0.5f;
    private static final float SHELL_PADDING = 0.2f;

    // Hit detection configuration
    private static final float BASE_HIT_RADIUS = 0.25f;
    private static final float INNER_ZONE_RATIO = 0.6f;  // Inner 60% = wrap, outer 40% = sibling

    public CraftingSpace(Vector3d playerPosition, Vector3d playerLookDirection, float distanceFromPlayer) {
        this.playerPosition = playerPosition;
        this.playerLookDirection = playerLookDirection;
        this.distanceFromPlayer = distanceFromPlayer;
    }

    /**
     * @return The center position of the crafting space
     */
    public Vector3d getCenter() {
        Vector3d direction = new Vector3d(playerLookDirection).normalize();
        return new Vector3d(playerPosition).add(HexMathUtil.mul(direction, distanceFromPlayer));
    }

    /**
     * Calculate the 3D position for a hex node in the crafting space.
     *
     * @param node The hex node
     * @param siblingIndex Index among siblings (0 for first/only child)
     * @param siblingCount Total number of siblings
     * @return Position in world space
     */
    public Vector3d calculateNodePosition(HexNode node, int siblingIndex, int siblingCount) {
        Vector3d center = getCenter();
        int depth = node.getChildren().size();

        // Calculate offset based on depth (shells are larger at lower depth)
        float depthOffset = depth * NODE_SPACING;

        // Calculate sibling offset (spread horizontally)
        float siblingOffset = 0;
        if (siblingCount > 1) {
            float totalWidth = (siblingCount - 1) * NODE_SPACING;
            siblingOffset = (siblingIndex * NODE_SPACING) - (totalWidth / 2);
        }

        // Position: center + depth offset (towards player) + sibling offset (horizontal)
        Vector3d right = new Vector3d(playerLookDirection).cross(new Vector3d(0, 1, 0)).normalize();
        Vector3d up = new Vector3d(0, 1, 0);

        return new Vector3d(center)
            .add(HexMathUtil.mul(playerLookDirection, -depthOffset)) // Move towards player for deeper nodes
            .add(HexMathUtil.mul(right, siblingOffset)); // Spread siblings horizontally
    }

    /**
     * Calculate the bounding radius for a shell visual at a given depth.
     *
     * @param depth The depth level
     * @param childrenCount Number of children wrapped
     * @return Shell radius
     */
    public float calculateShellRadius(int depth, int childrenCount) {
        float baseRadius = 0.3f;
        float depthScaling = (depth + 1) * 0.1f;
        float childScaling = Math.max(1, childrenCount) * 0.05f;
        return baseRadius + depthScaling + childScaling + SHELL_PADDING;
    }

    /**
     * Check if a world position is within the crafting space bounds.
     *
     * @param position The position to check
     * @return true if within crafting space
     */
    public boolean isInBounds(Vector3d position) {
        Vector3d center = getCenter();
        double distance = HexMathUtil.distance(center, position);
        return distance <= 2.0; // 2 block radius crafting space
    }

    /**
     * Find the node at a given position in the crafting space.
     *
     * @param position The position to check
     * @param rootNode The root node of the hex being composed
     * @return The node at the position, or null if none
     */
    public HexNode findNodeAtPosition(Vector3d position, HexNode rootNode) {
        if (rootNode == null) {
            return null;
        }
        return findNodeRecursive(position, rootNode, 0, 1);
    }

    private HexNode findNodeRecursive(Vector3d position, HexNode node, int siblingIndex, int siblingCount) {
        Vector3d nodePos = calculateNodePosition(node, siblingIndex, siblingCount);
        double distance = HexMathUtil.distance(nodePos, position);

        // Check if close enough to this node
        if (distance < NODE_SPACING / 2) {
            return node;
        }

        // Check children
        int childCount = node.getChildren().size();
        for (int i = 0; i < childCount; i++) {
            HexNode found = findNodeRecursive(position, node.getChildren().get(i), i, childCount);
            if (found != null) {
                return found;
            }
        }

        return null;
    }

    /**
     * Determine what action should happen when dropping a glyph at a position.
     *
     * @param position The drop position
     * @param glyph The glyph being dropped
     * @param rootNode The current hex root (may be null if empty)
     * @return The drop action to perform
     * @deprecated Use determineDropActionWithGaze instead for proper targeting
     */
    @Deprecated
    public DropAction determineDropAction(Vector3d position, Glyph glyph, HexNode rootNode) {
        if (!isInBounds(position)) {
            return DropAction.INVALID;
        }

        if (rootNode == null) {
            // Empty crafting space - place as root
            return DropAction.PLACE_AS_ROOT;
        }

        HexNode targetNode = findNodeAtPosition(position, rootNode);
        if (targetNode != null) {
            // Dropped on existing node - attempt wrap
            return DropAction.WRAP_NODE;
        }

        // Dropped in empty space - add as sibling (if valid parent exists nearby)
        return DropAction.ADD_AS_SIBLING;
    }

    // ==================== GAZE-BASED TARGETING ====================

    /**
     * Find the node the player is looking at using raycasting.
     * Returns the DEEPEST (lowest in tree / highest level) node that intersects the gaze ray.
     * This ensures players can target inner glyphs even when outer shells are larger.
     *
     * @param eyePos The player's eye position
     * @param lookDir The player's look direction (normalized)
     * @param rootNode The root of the hex tree
     * @return The gazed node, or null if not looking at any node
     */
    @Nullable
    public HexNode findGazedNode(Vector3d eyePos, Vector3d lookDir, HexNode rootNode) {
        if (rootNode == null) {
            return null;
        }

        // Find the deepest node that intersects the gaze ray
        return findGazedNodeRecursive(eyePos, lookDir, rootNode);
    }

    /**
     * Recursively find the deepest gazed node.
     * Children are checked first (higher priority since they're deeper in tree).
     */
    private HexNode findGazedNodeRecursive(Vector3d eyePos, Vector3d lookDir, HexNode node) {
        // Check children first (they're deeper, so higher priority for targeting)
        for (int i = 0; i < node.getChildren().size(); i++) {
            HexNode child = node.getChildren().get(i);
            HexNode childHit = findGazedNodeRecursive(eyePos, lookDir, child);
            if (childHit != null) {
                return childHit;  // Return deepest child hit
            }
        }

        // If no child hit, check if gaze intersects this node
        Vector3d nodePos = calculateNodePosition(node, node.getSiblingIndex(), node.getSiblingCount());
        float hitRadius = calculateHitRadius(node);

        double hitDist = RaycastUtil.rayIntersectsSphere(eyePos, lookDir, nodePos, hitRadius);
        if (hitDist >= 0) {
            return node;
        }

        return null;
    }

    /**
     * Calculate the hit radius for a node based on its depth and children.
     * Nodes with more children appear larger.
     *
     * @param node The node
     * @return The hit detection radius
     */
    public float calculateHitRadius(HexNode node) {
        int depth = node.getDepth();
        int childCount = node.getChildren().size();

        // Base radius + scaling based on subtree depth and child count
        float depthBonus = (depth - 1) * 0.08f;
        float childBonus = childCount * 0.04f;

        return BASE_HIT_RADIUS + depthBonus + childBonus;
    }

    /**
     * Calculate the visual scale for a node based on its subtree.
     * Nodes with deeper subtrees appear larger.
     *
     * @param node The node
     * @return The visual scale multiplier (1.0 = base size)
     */
    public float calculateVisualScale(HexNode node) {
        int depth = node.getDepth();
        int childCount = node.getChildren().size();

        // Base scale + depth bonus + child bonus
        float baseScale = 1.0f;
        float depthBonus = (depth - 1) * 0.3f;
        float childBonus = childCount * 0.1f;

        return baseScale + depthBonus + childBonus;
    }

    /**
     * Determine drop action based on gaze direction.
     * Uses raycasting to find what the player is looking at.
     *
     * @param eyePos The player's eye position
     * @param lookDir The player's look direction (normalized)
     * @param glyph The glyph being dropped
     * @param rootNode The current hex root (may be null if empty)
     * @return A DropResult containing the action and target node
     */
    public DropResult determineDropActionWithGaze(Vector3d eyePos, Vector3d lookDir,
                                                   Glyph glyph, HexNode rootNode) {
        // Check if gaze is pointing towards crafting space
        Vector3d center = getCenter();
        Vector3d toCenter = HexMathUtil.sub(center, eyePos);
        double dotProduct = toCenter.x * lookDir.x + toCenter.y * lookDir.y + toCenter.z * lookDir.z;

        if (dotProduct < 0) {
            // Looking away from crafting space
            return new DropResult(DropAction.INVALID, null);
        }

        if (rootNode == null) {
            // Empty crafting space - check if looking at the center area
            double distToCenter = RaycastUtil.rayIntersectsSphere(eyePos, lookDir, center, 1.0f);
            if (distToCenter >= 0) {
                return new DropResult(DropAction.PLACE_AS_ROOT, null);
            }
            return new DropResult(DropAction.INVALID, null);
        }

        // Find the node being looked at
        HexNode targetNode = findGazedNode(eyePos, lookDir, rootNode);

        if (targetNode == null) {
            // Not looking at any node
            return new DropResult(DropAction.INVALID, null);
        }

        // Determine if looking at center (wrap) or edge (sibling) of the target
        Vector3d targetPos = calculateNodePosition(targetNode, targetNode.getSiblingIndex(), targetNode.getSiblingCount());
        float hitRadius = calculateHitRadius(targetNode);

        // Find the exact hit point on the ray
        double hitDist = RaycastUtil.rayIntersectsSphere(eyePos, lookDir, targetPos, hitRadius);
        if (hitDist < 0) {
            return new DropResult(DropAction.INVALID, null);
        }

        // Calculate hit point
        Vector3d hitPoint = new Vector3d(
            eyePos.x + lookDir.x * hitDist,
            eyePos.y + lookDir.y * hitDist,
            eyePos.z + lookDir.z * hitDist
        );

        // Distance from hit point to node center
        double distToNodeCenter = HexMathUtil.distance(hitPoint, targetPos);
        float innerZoneRadius = hitRadius * INNER_ZONE_RATIO;

        if (distToNodeCenter <= innerZoneRadius) {
            // Hit inner zone - wrap the target node
            return new DropResult(DropAction.WRAP_NODE, targetNode);
        } else {
            // Hit outer zone - add as sibling
            return new DropResult(DropAction.ADD_AS_SIBLING, targetNode);
        }
    }

    /**
     * Get the position where a dragged glyph should appear based on gaze.
     *
     * @param eyePos The player's eye position
     * @param lookDir The player's look direction
     * @param distance Distance along the ray to place the glyph
     * @return The position for the dragged glyph
     */
    public Vector3d getDragPreviewPosition(Vector3d eyePos, Vector3d lookDir, float distance) {
        return new Vector3d(
            eyePos.x + lookDir.x * distance,
            eyePos.y + lookDir.y * distance,
            eyePos.z + lookDir.z * distance
        );
    }

    public enum DropAction {
        PLACE_AS_ROOT,
        WRAP_NODE,
        ADD_AS_SIBLING,
        INVALID
    }

    /**
     * Result of a drop action determination.
     * Contains both the action type and the target node (if applicable).
     */
    public static class DropResult {
        private final DropAction action;
        private final HexNode targetNode;

        public DropResult(DropAction action, @Nullable HexNode targetNode) {
            this.action = action;
            this.targetNode = targetNode;
        }

        public DropAction getAction() {
            return action;
        }

        @Nullable
        public HexNode getTargetNode() {
            return targetNode;
        }

        public boolean isValid() {
            return action != DropAction.INVALID;
        }

        @Override
        public String toString() {
            return "DropResult{action=" + action + ", target=" +
                   (targetNode != null ? targetNode.getValue().getGlyph().getDisplayName() : "null") + "}";
        }
    }
}
