package com.riprod.hexcode.hex;

import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphRegistry;

/**
 * Serializes and deserializes Hex structures.
 *
 * Format: Nested bracket notation like "BEAM[POWER[FIRE[]], ICE[]]"
 */
public class HexSerializer {

    private final GlyphRegistry registry;

    public HexSerializer() {
        this.registry = GlyphRegistry.getInstance();
    }

    /**
     * Serialize a Hex to string format.
     *
     * @param hex The hex to serialize
     * @return String representation
     */
    public String serialize(Hex hex) {
        if (hex == null || hex.getRoot() == null) {
            return "";
        }
        return serializeNode(hex.getRoot());
    }

    private String serializeNode(HexNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.getGlyph().getId());
        sb.append("[");

        boolean first = true;
        for (HexNode child : node.getChildren()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(serializeNode(child));
            first = false;
        }

        sb.append("]");
        return sb.toString();
    }

    /**
     * Deserialize a Hex from string format.
     *
     * @param data The serialized string
     * @return The deserialized Hex, or empty Hex if parsing fails
     */
    public Hex deserialize(String data) {
        if (data == null || data.isEmpty()) {
            return new Hex();
        }

        try {
            ParseResult result = parseNode(data, 0);
            if (result.node != null) {
                return new Hex(result.node);
            }
        } catch (Exception e) {
            // Parsing failed
        }

        return new Hex();
    }

    private ParseResult parseNode(String data, int start) {
        // Find glyph ID (until '[')
        int bracketStart = data.indexOf('[', start);
        if (bracketStart == -1) {
            return new ParseResult(null, start);
        }

        String glyphId = data.substring(start, bracketStart).trim();
        if (glyphId.endsWith(",")) {
            glyphId = glyphId.substring(0, glyphId.length() - 1).trim();
        }

        Glyph glyph = registry.getGlyph(glyphId);
        if (glyph == null) {
            // Try with hexcode prefix
            glyph = registry.getGlyph("hexcode:" + glyphId);
        }
        if (glyph == null) {
            return new ParseResult(null, start);
        }

        HexNode node = new HexNode(glyph);

        // Parse children
        int pos = bracketStart + 1;
        while (pos < data.length()) {
            // Skip whitespace
            while (pos < data.length() && Character.isWhitespace(data.charAt(pos))) {
                pos++;
            }

            if (pos >= data.length()) {
                break;
            }

            char c = data.charAt(pos);
            if (c == ']') {
                // End of children
                pos++;
                break;
            } else if (c == ',') {
                // Separator
                pos++;
                continue;
            } else {
                // Parse child
                ParseResult childResult = parseNode(data, pos);
                if (childResult.node != null) {
                    node.addChild(childResult.node);
                    pos = childResult.endPos;
                } else {
                    pos++;
                }
            }
        }

        return new ParseResult(node, pos);
    }

    private static class ParseResult {
        final HexNode node;
        final int endPos;

        ParseResult(HexNode node, int endPos) {
            this.node = node;
            this.endPos = endPos;
        }
    }

    /**
     * Validate a serialized hex string.
     *
     * @param data The serialized string
     * @return true if the string can be parsed
     */
    public boolean isValid(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }

        // Check bracket balance
        int depth = 0;
        for (char c : data.toCharArray()) {
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth < 0) {
                    return false;
                }
            }
        }

        if (depth != 0) {
            return false;
        }

        // Try to parse
        Hex hex = deserialize(data);
        return hex.hasRoot();
    }
}
