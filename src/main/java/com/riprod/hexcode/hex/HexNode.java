package com.riprod.hexcode.hex;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.riprod.hexcode.data.GlyphInstance;

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
public class HexNode {
    GlyphInstance value;
    List<HexNode> children = new ArrayList<HexNode>();

    public HexNode(GlyphInstance value) {
        this.value = value;
    }

    public void addChild(HexNode child) {
        children.add(child);
    }

    public List<HexNode> getChildren() {
        return children;
    }

    /**
     * Get the depth of this node in the tree.
     * The depth is defined as the length of the longest path
     * @return
     */
    public int getDepth () {
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

    public GlyphInstance getValue() {
        return value;
    }

    public Boolean isLeaf() {
        return children.isEmpty();
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

    public static HexNode fromJson(JsonObject obj) {
        GlyphInstance instance = GlyphInstance.fromJson(obj.getAsJsonObject("value"));
        HexNode node = new HexNode((GlyphInstance) instance);
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