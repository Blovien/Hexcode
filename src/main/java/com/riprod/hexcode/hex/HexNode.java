package com.riprod.hexcode.hex;

import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A node in the Hex tree structure.
 *
 * Each node contains a glyph and its children:
 * - EFFECT nodes have no children (leaves)
 * - MODIFIER nodes have exactly one child
 * - SELECT nodes can have one or multiple children (linked siblings)
 */
public class HexNode {
    private final Glyph glyph;
    private final List<HexNode> children;
    private HexNode parent;

    public HexNode(Glyph glyph) {
        this.glyph = glyph;
        this.children = new ArrayList<>();
        this.parent = null;
    }

    /**
     * @return The glyph at this node
     */
    public Glyph getGlyph() {
        return glyph;
    }

    /**
     * @return Unmodifiable list of child nodes
     */
    public List<HexNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * @return The parent node, or null if this is the root
     */
    public HexNode getParent() {
        return parent;
    }

    /**
     * @return true if this node has no children (is a leaf)
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    /**
     * @return true if this node is a sibling in a linked chain
     */
    public boolean isLinkedSibling() {
        return parent != null && parent.children.size() > 1;
    }

    /**
     * @return true if this is the root node (no parent)
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * @return Number of children
     */
    public int getChildCount() {
        return children.size();
    }

    /**
     * Add a child node.
     *
     * @param child The child node to add
     * @return true if added successfully
     */
    public boolean addChild(HexNode child) {
        if (!canAddChild()) {
            return false;
        }
        child.parent = this;
        children.add(child);
        return true;
    }

    /**
     * Remove a child node.
     *
     * @param child The child node to remove
     * @return true if removed successfully
     */
    public boolean removeChild(HexNode child) {
        if (children.remove(child)) {
            child.parent = null;
            return true;
        }
        return false;
    }

    /**
     * Check if a child can be added to this node.
     */
    public boolean canAddChild() {
        GlyphRole role = glyph.getRole();
        switch (role) {
            case EFFECT:
                // Effects are always leaves
                return false;
            case MODIFIER:
                // Modifiers can only have one child
                return children.isEmpty();
            case SELECT:
                // Selects can have multiple children (linked siblings)
                return true;
            default:
                return false;
        }
    }

    /**
     * Get the depth of this node in the tree.
     *
     * @return 0 for root, 1 for root's children, etc.
     */
    public int getDepth() {
        int depth = 0;
        HexNode current = this.parent;
        while (current != null) {
            depth++;
            current = current.parent;
        }
        return depth;
    }

    /**
     * Get the maximum depth of the subtree rooted at this node.
     *
     * @return Maximum depth from this node to its deepest leaf
     */
    public int getMaxSubtreeDepth() {
        if (isLeaf()) {
            return 0;
        }
        int maxChildDepth = 0;
        for (HexNode child : children) {
            maxChildDepth = Math.max(maxChildDepth, child.getMaxSubtreeDepth());
        }
        return 1 + maxChildDepth;
    }

    /**
     * Get all leaf nodes in this subtree.
     *
     * @return List of leaf nodes (EFFECT glyphs)
     */
    public List<HexNode> getLeaves() {
        List<HexNode> leaves = new ArrayList<>();
        collectLeaves(leaves);
        return leaves;
    }

    private void collectLeaves(List<HexNode> leaves) {
        if (isLeaf()) {
            leaves.add(this);
        } else {
            for (HexNode child : children) {
                child.collectLeaves(leaves);
            }
        }
    }

    /**
     * Find the root of the tree this node belongs to.
     *
     * @return The root node
     */
    public HexNode findRoot() {
        HexNode current = this;
        while (current.parent != null) {
            current = current.parent;
        }
        return current;
    }

    /**
     * Create a string representation of this node's subtree.
     */
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        buildTreeString(sb, "", true);
        return sb.toString();
    }

    private void buildTreeString(StringBuilder sb, String prefix, boolean isLast) {
        sb.append(prefix);
        sb.append(isLast ? "└── " : "├── ");
        sb.append(glyph.getDisplayName());
        sb.append(" (").append(glyph.getRole()).append(")");
        sb.append("\n");

        String childPrefix = prefix + (isLast ? "    " : "│   ");
        for (int i = 0; i < children.size(); i++) {
            children.get(i).buildTreeString(sb, childPrefix, i == children.size() - 1);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(glyph.getId());
        if (!children.isEmpty()) {
            sb.append("[");
            for (int i = 0; i < children.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(children.get(i).toString());
            }
            sb.append("]");
        } else {
            sb.append("[]");
        }
        return sb.toString();
    }
}
