package com.riprod.hexcode.hex;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.riprod.hexcode.data.GlyphInstance;
import com.riprod.hexcode.math.GlyphRotation;

import java.util.ArrayList;
import java.util.List;

/**
 * A node in the Hex spell tree structure.
 *
 * <p>A Hex is a tree-structured spell construct where:
 * <ul>
 *   <li>EFFECT glyphs are the innermost leaves (actions like FIRE, HEAL)</li>
 *   <li>MODIFIER glyphs wrap around others as inner shells (amplify/alter behavior)</li>
 *   <li>SELECT glyphs wrap around others as outer shells (determine targeting/delivery)</li>
 * </ul>
 *
 * <p>Each node tracks:
 * <ul>
 *   <li>Parent reference for tree traversal</li>
 *   <li>Angular margin for selection tolerance (grows when children are added)</li>
 * </ul>
 *
 * <p>If no SELECT wraps the Hex, an implicit SELF[] is assumed.
 */
public class HexNode {
    private GlyphInstance value;
    private HexNode parent;
    private List<HexNode> children = new ArrayList<HexNode>();

    /**
     * Angular margin for selection tolerance in degrees.
     * Single glyphs start at BASE_TOLERANCE (5 degrees).
     * Composed nodes (with children) grow to COMPOSED_TOLERANCE (7 degrees).
     */
    private float angularMargin = GlyphRotation.BASE_TOLERANCE;

    /**
     * Default constructor for codec deserialization.
     */
    public HexNode() {
        this.value = null;
        this.parent = null;
    }

    public HexNode(GlyphInstance value) {
        this.value = value;
        this.parent = null;
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

    /**
     * Get children as an array (used by codec).
     *
     * @return Array of child nodes
     */
    public HexNode[] getChildrenAsArray() {
        return children.toArray(new HexNode[0]);
    }

    /**
     * Add a child node to this node.
     * Sets this node as the child's parent and grows the angular margin.
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

        // Grow angular margin when becoming a parent
        if (children.size() == 1) {
            // First child added - grow from BASE to COMPOSED
            angularMargin = GlyphRotation.COMPOSED_TOLERANCE;
        }

        return true;
    }

    /**
     * Remove a child node from this node.
     * Clears the child's parent reference and shrinks angular margin if no children remain.
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

            // Shrink angular margin if no children remain
            if (children.isEmpty()) {
                angularMargin = GlyphRotation.BASE_TOLERANCE;
            }

            return true;
        }
        return false;
    }

    /**
     * Replace a child node with another node.
     * Useful for wrapping operations where a node is inserted between parent and child.
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

    /**
     * Get the parent node.
     *
     * @return The parent node, or null if this is the root
     */
    public HexNode getParent() {
        return parent;
    }

    /**
     * Set the parent node (used internally during tree manipulation).
     *
     * @param parent The new parent node
     */
    void setParent(HexNode parent) {
        this.parent = parent;
    }

    public List<HexNode> getChildren() {
        return children;
    }

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

    public GlyphInstance getValue() {
        return value;
    }

    // ========== ANGULAR MARGIN ==========

    /**
     * Get the angular margin for this node (selection tolerance in degrees).
     *
     * <p>Nodes without children have BASE_TOLERANCE (5 degrees).
     * Nodes with children have COMPOSED_TOLERANCE (7 degrees).
     *
     * @return Angular margin in degrees
     */
    public float getAngularMargin() {
        return angularMargin;
    }

    /**
     * Set the angular margin for this node.
     * Typically managed automatically by addChild/removeChild.
     *
     * @param margin Angular margin in degrees
     */
    public void setAngularMargin(float margin) {
        this.angularMargin = margin;
    }

    /**
     * Check if this node has a composed (larger) angular margin.
     *
     * @return true if this node has children and uses COMPOSED_TOLERANCE
     */
    public boolean hasComposedMargin() {
        return !children.isEmpty();
    }

    /**
     * Recalculate angular margin based on children.
     * Call after direct modification of children list.
     */
    public void recalculateAngularMargin() {
        if (children.isEmpty()) {
            angularMargin = GlyphRotation.BASE_TOLERANCE;
        } else {
            angularMargin = GlyphRotation.COMPOSED_TOLERANCE;
        }
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

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
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
     *
     * @param obj The JSON object
     * @return The deserialized HexNode with proper parent-child relationships
     */
    public static HexNode fromJson(JsonObject obj) {
        GlyphInstance instance = GlyphInstance.fromJson(obj.getAsJsonObject("value"));
        HexNode node = new HexNode(instance);
        JsonArray childrenArray = obj.getAsJsonArray("children");
        for (int i = 0; i < childrenArray.size(); i++) {
            JsonObject childObj = childrenArray.get(i).getAsJsonObject();
            // addChild() automatically sets the parent reference
            node.addChild(fromJson(childObj));
        }
        return node;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(value.getGlyph().getId());
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