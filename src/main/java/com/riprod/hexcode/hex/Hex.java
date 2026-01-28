package com.riprod.hexcode.hex;

import java.util.UUID;

import com.google.gson.JsonObject;
import com.riprod.hexcode.glyph.GlyphVisual;
import java.util.ArrayList;
import java.util.List;

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
    // unique id - mutable for codec deserialization
    private String id;
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

    /**
     * Set the hex ID (used by codec deserialization).
     *
     * @param id The unique identifier for this hex
     */
    public void setId(String id) {
        this.id = id != null ? id : UUID.randomUUID().toString();
    }

    /**
     * Get a display name for this hex.
     * For saved hexes, this returns a user-friendly name.
     *
     * @return The display name (same as id for now)
     */
    public String getName() {
        return id;
    }

    /**
     * Set the name/id of this hex (alias for setId for codec compatibility).
     *
     * @param name The name to set
     */
    public void setName(String name) {
        setId(name);
    }

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
     * Iterates over all glyphs using depth-first traversal and returns the styles of each glyph.
     * Glyph size decreases with depth (top-level is largest).
     * Neighboring glyphs are offset relative to their parent, arranged in a circle around the parent.
     */
    public List<GlyphVisual> getGlyphStyles() {
        List<GlyphVisual> styles = new ArrayList<>();
        if (root != null) {
            int maxDepth = getMaxDepth();
            traverse(root, 0, 0.0f, 0.0f, maxDepth, styles);
        }
        return styles;
    }

    private void traverse(HexNode node, int depth, float parentX, float parentY, int maxDepth, List<GlyphVisual> styles) {
        // Size: root is largest, shrinks to 1.0 at max depth
        float size = 1.0f + (maxDepth - depth) * 0.1f;
        // Position is offset from parent; for simplicity, place at parent's position initially
        float x = parentX;
        float y = parentY;
        GlyphVisual glyphVisual = node.getValue().getGlyph().getVisual();

        glyphVisual.setOffsetX(x);
        glyphVisual.setOffsetY(y);
        glyphVisual.setOffsetZ(depth * 0.1f); // Slight Z offset per depth for layering
        glyphVisual.setScale(size);
        styles.add(glyphVisual);

        List<HexNode> children = node.getChildren();
        if (children.size() == 1) {
            // One child: center it on the parent
            traverse(children.get(0), depth + 1, x, y, maxDepth, styles);
        } else if (!children.isEmpty()) {
            // Multiple children: arrange in a circle around the parent with small radius
            float radius = 0.2f;
            for (int i = 0; i < children.size(); i++) {
                float angle = 2 * (float) Math.PI * i / children.size() + (float) Math.PI / 2;
                float childX = x + radius * (float) Math.cos(angle);
                float childY = y + radius * (float) Math.sin(angle);
                traverse(children.get(i), depth + 1, childX, childY, maxDepth, styles);
            }
        }
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
        for (HexNode child : node.getChildren()) {
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
