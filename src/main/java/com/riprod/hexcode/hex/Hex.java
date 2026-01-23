package com.riprod.hexcode.hex;

import java.util.UUID;

import com.google.gson.JsonObject;

/**
 * A complete Hex spell structure.
 *
 * A Hex is a tree-structured spell construct where:
 * - EFFECT glyphs are the innermost leaves (actions like FIRE, HEAL)
 * - MODIFIER glyphs wrap around others as inner shells (amplify/alter behavior)
 * - SELECT glyphs wrap around others as outer shells (determine
 * targeting/delivery)
 *
 * If no SELECT wraps the Hex, an implicit SELF[] is assumed.
 */
public class Hex {
    private HexNode root;
    // unique id
    private final String id;
    private int uses = 0;

    
    public Hex() {
        this.root = null;
        this.id = UUID.randomUUID().toString();
    }

    public Hex(String id) {
        this.root = null;
        this.id = id != null ? id : UUID.randomUUID().toString();
    }

    public Hex(String id, HexNode root) {
        this.root = root;
        this.id = id != null ? id : UUID.randomUUID().toString();
    }
    public Hex(HexNode root) {
        this.root = root;
        this.id = UUID.randomUUID().toString();
    }

    /**
     * @return The root node of this Hex tree
     */
    public HexNode getRoot() {
        return root;
    }

    public String getId() {
        return id;
    }

    public int getUses() {
        return uses;
    }

    public void incrementUses() {
        this.uses++;
    }

    public Hex withIncrementedUses() {
        Hex newHex = new Hex(this.id, this.root);
        newHex.uses = this.uses + 1;
        return newHex;
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
     * @return Maximum depth of the Hex tree
     */
    public int getMaxDepth() {
        if (root == null) {
            return 0;
        }
        return root.getDepth();
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
        for (HexNode child : node.children) {
            count += countNodes(child);
        }
        return count;
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
        return "Hex[" + root.toString() + "]";
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        if (root != null) {
            obj.add("root", root.toJson());
        }
        return obj;
    }

    public static Hex fromJson(JsonObject obj) {
        if (obj == null || !obj.has("root")) {
            return new Hex();
        }
        HexNode rootNode = HexNode.fromJson(obj.getAsJsonObject("root"));
        return new Hex(rootNode);
    }

    /**
     * Get a tree visualization of this Hex.
     */
    public String toTreeString() {
        if (root == null) {
            return "Empty Hex";
        }
        return toTreeString(root, 0);
    }

    private String toTreeString(HexNode node, int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append("  ".repeat(depth)).append(node.getValue()).append("\n");
        for (HexNode child : node.getChildren()) {
            sb.append(toTreeString(child, depth + 1));
        }
        return sb.toString();
    }
}
