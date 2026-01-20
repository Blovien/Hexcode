package com.riprod.hexcode.hex;

import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphRole;

import java.util.List;

/**
 * A complete Hex spell structure.
 *
 * A Hex is a tree-structured spell construct where:
 * - EFFECT glyphs are the innermost leaves (actions like FIRE, HEAL)
 * - MODIFIER glyphs wrap around others as inner shells (amplify/alter behavior)
 * - SELECT glyphs wrap around others as outer shells (determine targeting/delivery)
 *
 * If no SELECT wraps the Hex, an implicit SELF[] is assumed.
 */
public class Hex {
    private HexNode root;

    public Hex() {
        this.root = null;
    }

    public Hex(HexNode root) {
        this.root = root;
    }

    /**
     * @return The root node of this Hex tree
     */
    public HexNode getRoot() {
        return root;
    }

    /**
     * Set the root node of this Hex.
     *
     * @param root The new root node
     */
    public void setRoot(HexNode root) {
        this.root = root;
    }

    /**
     * @return true if this Hex has a root node
     */
    public boolean hasRoot() {
        return root != null;
    }

    /**
     * @return true if this Hex is empty (no root)
     */
    public boolean isEmpty() {
        return root == null;
    }

    /**
     * @return true if the Hex is valid and ready to cast
     */
    public boolean isValid() {
        if (root == null) {
            return false;
        }
        return validateNode(root);
    }

    private boolean validateNode(HexNode node) {
        Glyph glyph = node.getGlyph();
        GlyphRole role = glyph.getRole();

        switch (role) {
            case EFFECT:
                // Effects must be leaves
                return node.isLeaf();

            case MODIFIER:
                // Modifiers must have exactly one child
                if (node.getChildCount() != 1) {
                    return false;
                }
                return validateNode(node.getChildren().get(0));

            case SELECT:
                // Selects must have at least one child
                if (node.getChildCount() == 0) {
                    return false;
                }
                for (HexNode child : node.getChildren()) {
                    if (!validateNode(child)) {
                        return false;
                    }
                }
                return true;

            default:
                return false;
        }
    }

    /**
     * @return true if the root is a SELECT glyph
     */
    public boolean hasExplicitSelect() {
        return root != null && root.getGlyph().getRole() == GlyphRole.SELECT;
    }

    /**
     * @return Maximum depth of the Hex tree
     */
    public int getMaxDepth() {
        if (root == null) {
            return 0;
        }
        return root.getMaxSubtreeDepth();
    }

    /**
     * @return Number of nodes in the Hex tree
     */
    public int getNodeCount() {
        if (root == null) {
            return 0;
        }
        return countNodes(root);
    }

    private int countNodes(HexNode node) {
        int count = 1;
        for (HexNode child : node.getChildren()) {
            count += countNodes(child);
        }
        return count;
    }

    /**
     * @return List of all leaf nodes (EFFECT glyphs)
     */
    public List<HexNode> getEffectNodes() {
        if (root == null) {
            return List.of();
        }
        return root.getLeaves();
    }

    /**
     * Calculate the base mana cost of this Hex.
     * Formula: Sum of (effect_base_cost) for all effects.
     * This is before target and modifier multipliers.
     *
     * @return Base mana cost
     */
    public int getBaseCost() {
        if (root == null) {
            return 0;
        }
        return calculateBaseCost(root);
    }

    private int calculateBaseCost(HexNode node) {
        Glyph glyph = node.getGlyph();
        if (glyph.getRole() == GlyphRole.EFFECT) {
            return glyph.getBaseCost();
        }
        int cost = 0;
        for (HexNode child : node.getChildren()) {
            cost += calculateBaseCost(child);
        }
        return cost;
    }

    /**
     * Get string representation of this Hex.
     * Example: "BEAM[POWER[FIRE[]], ICE[]]"
     */
    @Override
    public String toString() {
        if (root == null) {
            return "Hex[]";
        }
        return "Hex{" + root.toString() + "}";
    }

    /**
     * Get a tree visualization of this Hex.
     */
    public String toTreeString() {
        if (root == null) {
            return "Empty Hex";
        }
        return root.toTreeString();
    }
}
