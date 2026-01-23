package com.riprod.hexcode.mode;

import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.hex.HexNode;
import com.riprod.hexcode.util.HexMathUtil;
import com.hypixel.hytale.math.vector.Vector3d;

/**
 * Represents the 3D crafting space where hexes are composed.
 *
 * Located ~2 blocks in front of player at eye level.
 * Shows the composed Hex structure with shell visuals.
 */
public class CraftingSpace {
    private final Vector3d playerPosition;
    private final Vector3d playerLookDirection;
    private final float distanceFromPlayer;

    // Visual configuration
    private static final float NODE_SPACING = 0.5f;
    private static final float SHELL_PADDING = 0.2f;

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
     */
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

    public enum DropAction {
        PLACE_AS_ROOT,
        WRAP_NODE,
        ADD_AS_SIBLING,
        INVALID
    }
}
