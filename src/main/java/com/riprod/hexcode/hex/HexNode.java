package com.riprod.hexcode.hex;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.data.GlyphInstance;
import com.riprod.hexcode.math.GlyphRotation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A node in the Hex spell tree structure with angular positioning support.
 *
 * <p>
 * HexNodes form a tree rendered in spherical coordinates (yaw/pitch) around
 * a central point. Parent nodes scale based on subtree depth to contain
 * children.
 * Hit testing finds the deepest node under the user's view angle using
 * recursive descent.
 *
 * <p>
 * A Hex is a tree-structured spell construct where:
 * <ul>
 * <li>EFFECT glyphs are the innermost leaves (actions like FIRE, HEAL)</li>
 * <li>MODIFIER glyphs wrap around others as inner shells (amplify/alter
 * behavior)</li>
 * <li>SELECT glyphs wrap around others as outer shells (determine
 * targeting/delivery)</li>
 * </ul>
 *
 * <p>
 * Key concepts:
 * <ul>
 * <li><b>Yaw</b>: Horizontal angle (-180 to 180), like longitude</li>
 * <li><b>Pitch</b>: Vertical angle (-90 to 90), like latitude</li>
 * <li><b>Subtree Depth</b>: Max depth of children (0 for leaves, +1 per
 * level)</li>
 * <li><b>Scale</b>: 1.0 + 0.2 * subtreeDepth - larger for deeper subtrees</li>
 * <li><b>Angular Radius</b>: baseMargin * scale - visual bounds in degrees</li>
 * </ul>
 *
 * <p>
 * If no SELECT wraps the Hex, an implicit SELF[] is assumed.
 */
public class HexNode {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // ========== TREE STRUCTURE ==========

    private GlyphInstance value;
    private HexNode parent;
    private List<HexNode> children = new ArrayList<>();

    // ========== CONFIGURATION ==========

    /** Unique identifier for this node */
    private String id;

    /** Base angular radius in degrees (before scaling) */
    private float baseMargin = 5.0f;

    // ========== COMPUTED LAYOUT (recalculated on tree changes) ==========

    /** Subtree depth: 0 for leaf, max(children.subtreeDepth) + 1 for parents */
    private int subtreeDepth;

    /** Scale factor: 1.0 + 0.2 * subtreeDepth */
    private float scale = 1f;

    /** Angular radius in degrees: baseMargin * scale */
    private float angularRadius;

    // ========== LOCAL POSITIONING (relative to parent) ==========

    /** Direction from parent center (0-360 degrees) */
    private float localOffsetAngle;

    /** Angular distance from parent center (degrees) */
    private float localOffsetDistance;

    // ========== ABSOLUTE POSITIONING (computed) ==========

    /** Absolute yaw position (-180 to 180) */
    private float absoluteYaw;

    /** Absolute pitch position (-90 to 90) */
    private float absolutePitch;

    // ========== ENTITY REFERENCE ==========

    /** Reference to the spawned visual entity for this node */
    private transient Ref<EntityStore> entityRef;

    // ========== USAGE TRACKING ==========

    /** Number of times this hex has been cast (for saved hexes) */
    private int uses = 0;

    // ========== CONSTRUCTORS ==========

    /**
     * Default constructor for codec deserialization.
     */
    public HexNode() {
        this.value = null;
        this.parent = null;
        this.id = UUID.randomUUID().toString();
        this.angularRadius = baseMargin;
    }

    /**
     * Create a new HexNode with the given glyph instance.
     *
     * @param value The glyph instance for this node
     */
    public HexNode(GlyphInstance value) {
        this.value = value;
        this.parent = null;
        this.id = UUID.randomUUID().toString();
        this.angularRadius = baseMargin;
    }

    /**
     * Create a new HexNode with the given glyph instance and custom base margin.
     *
     * @param value      The glyph instance for this node
     * @param baseMargin The base angular radius in degrees
     */
    public HexNode(GlyphInstance value, float baseMargin) {
        this.value = value;
        this.parent = null;
        this.id = UUID.randomUUID().toString();
        this.baseMargin = baseMargin;
        this.angularRadius = baseMargin;
    }

    // ========== LAYOUT ALGORITHM (3-pass) ==========

    /**
     * Recalculate the entire layout for this node and all descendants.
     * Call this after any tree structure changes.
     *
     * <p>
     * This performs a 3-pass layout:
     * <ol>
     * <li>Calculate subtree depths (bottom-up)</li>
     * <li>Position children (weighted angular distribution)</li>
     * <li>Calculate absolute positions (top-down)</li>
     * </ol>
     */
    public void recalculateLayout() {
        calculateSubtreeDepth(this);
        positionChildren(this);
        calculateAbsolutePositions(this, absoluteYaw, absolutePitch);
    }

    /**
     * Pass 1: Calculate subtree depths bottom-up.
     * Sets subtreeDepth, scale, and angularRadius for each node.
     *
     * @param node The node to process
     * @return The subtree depth of this node
     */
    private int calculateSubtreeDepth(HexNode node) {
        if (node.children.isEmpty()) {
            node.subtreeDepth = 0;
        } else {
            int maxChildDepth = 0;
            for (HexNode child : node.children) {
                int childDepth = calculateSubtreeDepth(child);
                maxChildDepth = Math.max(maxChildDepth, childDepth);
            }
            node.subtreeDepth = maxChildDepth + 1;
        }

        // Calculate scale and angular radius
        node.scale = 1.0f + 0.3f * node.subtreeDepth;
        node.angularRadius = node.baseMargin * node.scale * 2;

        return node.subtreeDepth;
    }

    /**
     * Pass 2: Position children using weighted angular distribution.
     * Single child shares parent center. Multiple children are distributed
     * proportionally by their angular radius around 360 degrees.
     *
     * @param parent The parent node whose children to position
     */
    private void positionChildren(HexNode parent) {
        List<HexNode> childList = parent.children;
        if (childList.isEmpty()) {
            return;
        }

        // Single child: same center as parent (perfect stack)
        if (childList.size() == 1) {
            HexNode child = childList.get(0);
            child.localOffsetAngle = 0;
            child.localOffsetDistance = 0;
            child.scale = parent.scale * 0.75f;
            positionChildren(child);
            return;
        }

        // Multiple children: weighted distribution
        float totalMass = 0;
        for (HexNode child : childList) {
            totalMass += child.scale;
        }

        float paddingPerChild = 1.0f; // degrees between children
        float totalPadding = paddingPerChild * childList.size();
        float availableAngle = 360.0f - totalPadding;
        float currentAngle = 0;

        for (HexNode child : childList) {
            float childSlice = (child.scale / totalMass) * availableAngle;

            child.localOffsetAngle = currentAngle + (childSlice / 2.0f);
            child.localOffsetDistance = (parent.angularRadius - child.angularRadius) * 2f;

            child.scale = parent.scale * 0.33f; // Further scale down children when there are multiple

            currentAngle += childSlice + paddingPerChild;
        }

        // Recursively position grandchildren
        for (HexNode child : childList) {
            positionChildren(child);
        }
    }

    /**
     * Pass 3: Calculate absolute positions top-down.
     * Converts local offsets to world yaw/pitch coordinates.
     *
     * @param node        The node to process
     * @param parentYaw   The parent's absolute yaw
     * @param parentPitch The parent's absolute pitch
     */
    private void calculateAbsolutePositions(HexNode node, float parentYaw, float parentPitch) {
        if (node.parent == null) {
            // Root node uses its current absolute position
            node.absoluteYaw = parentYaw;
            node.absolutePitch = parentPitch;
        } else {
            float offsetAngleRad = (float) Math.toRadians(node.localOffsetAngle);
            float distance = node.localOffsetDistance;

            // Local 0 = toward +pitch, 90 = toward +yaw
            float deltaPitch = distance * (float) Math.cos(offsetAngleRad);

            // Correct for longitude convergence at poles
            float parentPitchRad = (float) Math.toRadians(parentPitch);
            float deltaYaw;
            if (Math.abs(parentPitch) > 85.0f) {
                // Near poles, skip correction to avoid division issues
                deltaYaw = distance * (float) Math.sin(offsetAngleRad);
            } else {
                deltaYaw = distance * (float) Math.sin(offsetAngleRad) / (float) Math.cos(parentPitchRad);
            }

            node.absoluteYaw = parentYaw + deltaYaw;
            node.absolutePitch = parentPitch + deltaPitch;

            // Normalize yaw to [-180, 180]
            while (node.absoluteYaw > 180)
                node.absoluteYaw -= 360;
            while (node.absoluteYaw < -180)
                node.absoluteYaw += 360;

            // Clamp pitch to [-90, 90]
            node.absolutePitch = Math.max(-90, Math.min(90, node.absolutePitch));
        }

        // Recursively process children
        for (HexNode child : node.children) {
            calculateAbsolutePositions(child, node.absoluteYaw, node.absolutePitch);
        }
    }

    // ========== HIT TESTING ==========

    /**
     * Calculate angular distance between two points on a sphere (Haversine
     * formula).
     *
     * @param yaw1   First point yaw
     * @param pitch1 First point pitch
     * @param yaw2   Second point yaw
     * @param pitch2 Second point pitch
     * @return Angular distance in degrees
     */
    public static float angularDistance(float yaw1, float pitch1, float yaw2, float pitch2) {
        float lat1 = (float) Math.toRadians(pitch1);
        float lat2 = (float) Math.toRadians(pitch2);
        float deltaLat = lat2 - lat1;
        float deltaLon = (float) Math.toRadians(yaw2 - yaw1);

        float a = (float) (Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2));
        float c = 2 * (float) Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (float) Math.toDegrees(c);
    }

    /**
     * Check if a point (yaw/pitch) is within this node's angular bounds.
     *
     * @param userYaw   The user's look yaw
     * @param userPitch The user's look pitch
     * @return true if the point is within this node's angular radius
     */
    public boolean isWithinBounds(float userYaw, float userPitch) {
        float distance = angularDistance(absoluteYaw, absolutePitch, userYaw, userPitch);
        return distance <= angularRadius;
    }

    /**
     * Find the deepest node at the given look direction using recursive descent.
     *
     * @param userYaw   The user's look yaw
     * @param userPitch The user's look pitch
     * @return The deepest node containing the point, or null if none
     */
    public HexNode findDeepestAt(float userYaw, float userPitch) {
        return findDeepestRecursive(this, userYaw, userPitch);
    }

    /**
     * Recursive helper for findDeepestAt.
     */
    private HexNode findDeepestRecursive(HexNode node, float userYaw, float userPitch) {
        if (!node.isWithinBounds(userYaw, userPitch)) {
            return null;
        }

        // Check children for deeper matches
        for (HexNode child : node.children) {
            HexNode result = findDeepestRecursive(child, userYaw, userPitch);
            if (result != null) {
                return result;
            }
        }

        // No child matched, return this node
        return node;
    }

    // ========== MOUNT OFFSET CONVERSION ==========

    /**
     * Convert this node's position to a mount offset Vector3f.
     * For child nodes, this returns the offset relative to the parent node.
     * For root nodes, this returns the absolute offset (though root nodes
     * typically use OrbitalPositionComponent instead of MountedComponent).
     *
     * @param distance Distance from the center point
     * @return Vector3f offset for mounting
     */
    public Vector3f getMountOffset(float distance) {
        if (parent == null) {
            // Root node: use absolute position
            return GlyphRotation.angularToMountOffset(absoluteYaw, absolutePitch, distance);
        } else {
            // Child node: calculate offset relative to parent
            // localOffsetAngle: 0 = toward +pitch (down), 90 = toward +yaw (right)
            // localOffsetDistance: angular distance in degrees
            float offsetAngleRad = (float) Math.toRadians(localOffsetAngle);

            // Convert polar (angle, distance) to local Cartesian offset
            // In local space: +Z = parent's forward, +X = parent's right
            float localYawOffset = localOffsetDistance * (float) Math.sin(offsetAngleRad);
            float localPitchOffset = localOffsetDistance * (float) Math.cos(offsetAngleRad);

            // Convert angular offset to linear offset at the given distance
            // At small angles: arc length ≈ angle_radians * radius
            float x = (float) Math.toRadians(localYawOffset) * distance;
            float y = (float) -Math.toRadians(localPitchOffset) * distance; // -pitch = up
            float z = 0; // Children are on the same spherical shell (same distance from player)

            return new Vector3f(x, y, z);
        }
    }

    // ========== CODEC SETTERS ==========

    /**
     * Set the glyph instance value (used by codec).
     *
     * @param value The glyph instance for this node
     */
    public void setValue(GlyphInstance value) {
        this.value = value;
    }

    /**
     * Set the children list (used by codec).
     * Automatically sets parent references.
     *
     * @param children The child nodes
     */
    public void setChildren(List<HexNode> children) {
        this.children.clear();
        if (children != null) {
            for (HexNode child : children) {
                if (child != null) {
                    child.parent = this;
                    this.children.add(child);
                }
            }
        }
    }

    /**
     * Set children from an array (used by codec).
     * Automatically sets parent references.
     *
     * @param children The child nodes array
     */
    public void setChildrenFromArray(HexNode[] children) {
        this.children.clear();
        if (children != null) {
            for (HexNode child : children) {
                if (child != null) {
                    child.parent = this;
                    this.children.add(child);
                }
            }
        }
    }

    // ========== TREE MANIPULATION ==========

    /**
     * Add a child node to this node.
     * Sets this node as the child's parent.
     *
     * @param child The child node to add
     * @return true if added successfully, false if child is null
     */
    public boolean addChild(HexNode child) {
        if (child == null) {
            return false;
        }
        child.parent = this;
        children.add(child);
        return true;
    }

    /**
     * Remove a child node from this node.
     * Clears the child's parent reference.
     *
     * @param child The child node to remove
     * @return true if removed successfully, false if child was not found
     */
    public boolean removeChild(HexNode child) {
        if (child == null) {
            return false;
        }
        if (children.remove(child)) {
            child.parent = null;
            return true;
        }
        return false;
    }

    /**
     * Replace a child node with another node.
     * Useful for wrapping operations where a node is inserted between parent and
     * child.
     *
     * @param oldChild The child to replace
     * @param newChild The replacement node
     * @return true if replaced successfully
     */
    public boolean replaceChild(HexNode oldChild, HexNode newChild) {
        if (oldChild == null || newChild == null) {
            return false;
        }
        int index = children.indexOf(oldChild);
        if (index == -1) {
            return false;
        }
        oldChild.parent = null;
        newChild.parent = this;
        children.set(index, newChild);
        return true;
    }

    // ========== GETTERS & SETTERS ==========

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public GlyphInstance getValue() {
        return value;
    }

    public HexNode getParent() {
        return parent;
    }

    void setParent(HexNode parent) {
        this.parent = parent;
    }

    public List<HexNode> getChildren() {
        return children;
    }

    public List<HexNode> getAllChildren() {
        // iterate over all sub-children in the tree and get all of the children to return
        List<HexNode> allChildren = new ArrayList<>();
        for (HexNode child : children) {
            allChildren.add(child);
            allChildren.addAll(child.getAllChildren());
        }

        return allChildren;
    }

    /**
     * Check if this node has children.
     *
     * @return true if this node has at least one child
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public float getBaseMargin() {
        return baseMargin;
    }

    public void setBaseMargin(float baseMargin) {
        this.baseMargin = baseMargin;
        this.angularRadius = baseMargin * scale;
    }

    public int getSubtreeDepth() {
        return subtreeDepth;
    }

    public float getScale() {
        return scale;
    }

    public float getAngularRadius() {
        return angularRadius;
    }

    public float getLocalOffsetAngle() {
        return localOffsetAngle;
    }

    public float getLocalOffsetDistance() {
        return localOffsetDistance;
    }

    public float getAbsoluteYaw() {
        return absoluteYaw;
    }

    public void setAbsoluteYaw(float absoluteYaw) {
        this.absoluteYaw = absoluteYaw;
    }

    public float getAbsolutePitch() {
        return absolutePitch;
    }

    public void setAbsolutePitch(float absolutePitch) {
        this.absolutePitch = absolutePitch;
    }

    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    public void setEntityRef(Ref<EntityStore> entityRef) {
        this.entityRef = entityRef;
    }

    // ========== USAGE TRACKING ==========

    /**
     * Get the number of times this hex has been cast.
     *
     * @return The usage count
     */
    public int getUses() {
        return uses;
    }

    /**
     * Set the usage count (used by codec deserialization).
     *
     * @param uses The number of times this hex has been used
     */
    public void setUses(int uses) {
        this.uses = Math.max(0, uses);
    }

    /**
     * Increment the usage count by 1.
     */
    public void incrementUses() {
        this.uses++;
    }

    // ========== TREE QUERIES ==========

    /**
     * Get the depth of the subtree rooted at this node.
     * Depth is defined as the length of the longest path from this node to a leaf.
     *
     * @return The depth (1 for a leaf node)
     */
    public int getDepth() {
        if (children.isEmpty()) {
            return 1;
        }
        int maxChildDepth = 0;
        for (HexNode child : children) {
            int childDepth = child.getDepth();
            if (childDepth > maxChildDepth) {
                maxChildDepth = childDepth;
            }
        }
        return 1 + maxChildDepth;
    }

    /**
     * Get the level of this node in the tree (distance from root).
     * Root is level 0.
     *
     * @return The level of this node
     */
    public int getLevel() {
        int level = 0;
        HexNode current = this.parent;
        while (current != null) {
            level++;
            current = current.parent;
        }
        return level;
    }

    /**
     * Get the index of this node among its siblings.
     *
     * @return The sibling index, or 0 if this is root or has no parent
     */
    public int getSiblingIndex() {
        if (parent == null) {
            return 0;
        }
        return parent.children.indexOf(this);
    }

    /**
     * Get the number of siblings (including this node).
     *
     * @return The sibling count, or 1 if this is root
     */
    public int getSiblingCount() {
        if (parent == null) {
            return 1;
        }
        return parent.children.size();
    }

    /**
     * Check if this node is a leaf (has no children).
     *
     * @return true if this node has no children
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    /**
     * Check if this node is the root (has no parent).
     *
     * @return true if this node has no parent
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Get the root node of the tree containing this node.
     *
     * @return The root node
     */
    public HexNode getRoot() {
        HexNode current = this;
        while (current.parent != null) {
            current = current.parent;
        }
        return current;
    }

    /**
     * Count the total number of nodes in the tree rooted at this node.
     *
     * @return The total node count (including this node)
     */
    public int getNodeCount() {
        int count = 1;
        for (HexNode child : children) {
            count += child.getNodeCount();
        }
        return count;
    }

    /**
     * Create a deep copy of this node and all its children.
     * The copy has new UUIDs and no parent reference.
     *
     * @return A deep copy of this node tree
     */
    public HexNode deepCopy() {
        HexNode copy = new HexNode(this.value, this.baseMargin);
        copy.id = UUID.randomUUID().toString(); // New ID for copy
        copy.uses = this.uses;
        copy.absoluteYaw = this.absoluteYaw;
        copy.absolutePitch = this.absolutePitch;

        for (HexNode child : this.children) {
            HexNode childCopy = child.deepCopy();
            copy.addChild(childCopy);
        }

        return copy;
    }

    /**
     * Get a tree visualization of this node and its descendants.
     *
     * @return Formatted tree string
     */
    public String toTreeString() {
        return toTreeStringHelper(0);
    }

    private String toTreeStringHelper(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append("  ".repeat(depth));
        if (value != null) {
            sb.append(value.toString());
        } else {
            sb.append("(empty)");
        }
        sb.append("\n");
        for (HexNode child : children) {
            sb.append(child.toTreeStringHelper(depth + 1));
        }
        return sb.toString();
    }

    // ========== SERIALIZATION ==========

    /**
     * Serialize this HexNode to JSON.
     *
     * @return JSON representation of this node and its children
     */
    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("baseMargin", baseMargin);
        obj.add("value", value.toJson());

        JsonArray childrenArray = new JsonArray();
        for (HexNode child : children) {
            childrenArray.add(child.toJson());
        }
        obj.add("children", childrenArray);

        return obj;
    }

    /**
     * Deserialize a HexNode from JSON.
     * Parent relationships are automatically restored via addChild().
     * Layout is recalculated after deserialization.
     *
     * @param obj The JSON object
     * @return The deserialized HexNode with proper parent-child relationships
     */
    public static HexNode fromJson(JsonObject obj) {
        GlyphInstance instance = GlyphInstance.fromJson(obj.getAsJsonObject("value"));
        HexNode node = new HexNode(instance);

        // Restore id if present
        if (obj.has("id")) {
            node.id = obj.get("id").getAsString();
        }

        // Restore baseMargin if present
        if (obj.has("baseMargin")) {
            node.baseMargin = obj.get("baseMargin").getAsFloat();
            node.angularRadius = node.baseMargin;
        }

        // Restore children
        JsonArray childrenArray = obj.getAsJsonArray("children");
        for (int i = 0; i < childrenArray.size(); i++) {
            JsonObject childObj = childrenArray.get(i).getAsJsonObject();
            node.addChild(fromJson(childObj));
        }

        return node;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (value != null && value.getGlyph() != null) {
            sb.append(value.getGlyph().getId());
        } else {
            sb.append("null");
        }
        sb.append("[");
        for (int i = 0; i < children.size(); i++) {
            sb.append(children.get(i).toString());
            if (i < children.size() - 1) {
                sb.append(":");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
