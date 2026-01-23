package com.riprod.hexcode.asset;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.riprod.hexcode.glyph.Glyph;
import com.riprod.hexcode.glyph.GlyphRegistry;
import com.riprod.hexcode.glyph.GlyphRole;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a glyph's asset-defined properties loaded from a JSON file.
 *
 * <p>
 * All glyph configuration is driven by asset files rather than hard-coded
 * values.
 * This enables external plugins to define glyphs using the same system and
 * allows
 * for hot-reloading during development.
 *
 * <p>
 * Asset File Location: {@code Assets/Server/Hexcode/Glyphs/{glyphId}.json}
 *
 * <p>
 * Example asset file:
 * 
 * <pre>{@code
 * {
 *   "id": "hexcode:fire",
 *   "displayName": "Fire",
 *   "role": "EFFECT",
 *   "modelPath": "Hexcode/Models/Glyphs/fire.blockymodel",
 *   "drawingTemplatePath": "Hexcode/Drawings/fire.png",
 *   "basePower": 0.25,
 *   "baseManaCost": 15,
 *   "baseVariability": 0.6,
 *   "properties": {
 *     "damageType": "fire",
 *     "baseDamage": 10.0,
 *     "burnDuration": 3.0
 *   }
 * }
 * }</pre>
 */
public class GlyphAssetDefinition {

    private final String id;
    private final String displayName;
    private final String modelPath;
    private final String drawingTemplatePath;
    private final float basePower;
    private final float baseManaCost;
    private final float baseVariability;
    private final Map<String, Object> properties;
    private final String glyphId;

    private GlyphAssetDefinition(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id is required");
        this.displayName = Objects.requireNonNull(builder.displayName, "displayName is required");
        this.glyphId = Objects.requireNonNull(builder.glyphId, "glyphId is required");
        this.modelPath = builder.modelPath;
        this.drawingTemplatePath = builder.drawingTemplatePath;
        this.basePower = builder.basePower;
        this.baseManaCost = builder.baseManaCost;
        this.baseVariability = builder.baseVariability;
        this.properties = new HashMap<>(builder.properties);
    }

    /**
     * @return Unique identifier for this glyph (e.g., "hexcode:fire")
     */
    public String getId() {
        return id;
    }

    /**
     * @return Human-readable display name (e.g., "Fire")
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return The role of this glyph: EFFECT, MODIFIER, or SELECT
     */
    public String getGlyphId() {
        return glyphId;
    }

    /**
     * @return Path to the Blockbench .blockymodel file for 3D visualization
     */
    public String getModelPath() {
        return modelPath;
    }

    /**
     * @return Path to the PNG used for drawing comparison
     */
    public String getDrawingTemplatePath() {
        return drawingTemplatePath;
    }

    /**
     * @return Base power multiplier for effect strength (default 1.0)
     */
    public float getBasePower() {
        return basePower;
    }

    /**
     * @return Base mana cost before modifiers
     */
    public float getBaseManaCost() {
        return baseManaCost;
    }

    /**
     * @return Drawing tolerance from 0.0-1.0 (default 0.5)
     */
    public float getBaseVariability() {
        return baseVariability;
    }

    /**
     * @return Role-specific custom properties
     */
    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }

    /**
     * Get a property value as a float.
     *
     * @param key          Property key
     * @param defaultValue Default value if property doesn't exist or isn't a number
     * @return The property value or default
     */
    public float getPropertyFloat(String key, float defaultValue) {
        Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return defaultValue;
    }

    /**
     * Get a property value as a String.
     *
     * @param key          Property key
     * @param defaultValue Default value if property doesn't exist
     * @return The property value or default
     */
    public String getPropertyString(String key, String defaultValue) {
        Object value = properties.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }

    /**
     * Get a property value as an int.
     *
     * @param key          Property key
     * @param defaultValue Default value if property doesn't exist or isn't a number
     * @return The property value or default
     */
    public int getPropertyInt(String key, int defaultValue) {
        Object value = properties.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Get a property value as a boolean.
     *
     * @param key          Property key
     * @param defaultValue Default value if property doesn't exist
     * @return The property value or default
     */
    public boolean getPropertyBoolean(String key, boolean defaultValue) {
        Object value = properties.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    /**
     * Validates this asset definition and returns the expected mana cost.
     *
     * @return The expected mana cost for this glyph
     * @throws IllegalStateException if required fields are missing based on role
     */
    public float validate() throws IllegalStateException {
        // Validate ID format (namespace:name)
        if (!id.contains(":")) {
            throw new IllegalStateException("Glyph ID must follow namespace format (e.g., 'hexcode:fire'): " + id);
        }

        return baseManaCost;
    }

    /**
     * Parse a GlyphAssetDefinition from a JSON object.
     *
     * @param json The JSON object to parse
     * @return The parsed GlyphAssetDefinition
     * @throws IllegalArgumentException if required fields are missing or invalid
     */
    public static GlyphAssetDefinition fromJson(JsonObject json) {
        Builder builder = builder();

        // Required fields
        if (!json.has("id")) {
            throw new IllegalArgumentException("Missing required field: id");
        }
        builder.id(json.get("id").getAsString());

        if (!json.has("displayName")) {
            throw new IllegalArgumentException("Missing required field: displayName");
        }
        builder.displayName(json.get("displayName").getAsString());

        if (!json.has("glyphId")) {
            throw new IllegalArgumentException("Missing required field: glyphId");
        }

        String glyphId = json.get("glyphId").getAsString().toUpperCase();
        builder.glyphId(glyphId);

        // Optional fields with defaults
        if (json.has("modelPath")) {
            builder.modelPath(json.get("modelPath").getAsString());
        }

        if (json.has("drawingTemplatePath")) {
            builder.drawingTemplatePath(json.get("drawingTemplatePath").getAsString());
        }

        if (json.has("basePower")) {
            builder.basePower(json.get("basePower").getAsFloat());
        }

        if (json.has("baseManaCost")) {
            builder.baseManaCost(json.get("baseManaCost").getAsFloat());
        }

        if (json.has("baseVariability")) {
            builder.baseVariability(json.get("baseVariability").getAsFloat());
        }

        // Parse properties map
        if (json.has("properties") && json.get("properties").isJsonObject()) {
            JsonObject props = json.getAsJsonObject("properties");
            for (Map.Entry<String, JsonElement> entry : props.entrySet()) {
                JsonElement element = entry.getValue();
                if (element.isJsonPrimitive()) {
                    if (element.getAsJsonPrimitive().isBoolean()) {
                        builder.property(entry.getKey(), element.getAsBoolean());
                    } else if (element.getAsJsonPrimitive().isNumber()) {
                        builder.property(entry.getKey(), element.getAsNumber());
                    } else if (element.getAsJsonPrimitive().isString()) {
                        builder.property(entry.getKey(), element.getAsString());
                    }
                }
            }
        }

        return builder.build();
    }

    /**
     * Serialize this GlyphAssetDefinition to a JSON object.
     *
     * @return The JSON representation
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();

        // Required fields
        json.addProperty("id", id);
        json.addProperty("displayName", displayName);
        json.addProperty("glyphId", glyphId); // or however you get the glyph's string ID

        // Optional fields (only include if non-default/non-null)
        if (modelPath != null) {
            json.addProperty("modelPath", modelPath);
        }

        if (drawingTemplatePath != null) {
            json.addProperty("drawingTemplatePath", drawingTemplatePath);
        }

        if (basePower != 1.0f) {
            json.addProperty("basePower", basePower);
        }

        if (baseManaCost != 1.0f) {
            json.addProperty("baseManaCost", baseManaCost);
        }

        if (baseVariability != 0.5f) {
            json.addProperty("baseVariability", baseVariability);
        }

        // Properties map
        if (!properties.isEmpty()) {
            JsonObject props = new JsonObject();
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Boolean) {
                    props.addProperty(entry.getKey(), (Boolean) value);
                } else if (value instanceof Number) {
                    props.addProperty(entry.getKey(), (Number) value);
                } else if (value instanceof String) {
                    props.addProperty(entry.getKey(), (String) value);
                }
            }
            json.add("properties", props);
        }

        return json;
    }

    /**
     * Create a new builder for GlyphAssetDefinition.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing GlyphAssetDefinition instances.
     */
    public static class Builder {
        private String id;
        private String displayName;
        private String glyphId;
        private String modelPath;
        private String drawingTemplatePath;
        private float basePower = 1.0f;
        private float baseManaCost = 1.0f;
        private float baseVariability = 0.5f;
        private final Map<String, Object> properties = new HashMap<>();

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder glyphId(String glyphId) {
            this.glyphId = glyphId;
            return this;
        }

        public Builder modelPath(String modelPath) {
            this.modelPath = modelPath;
            return this;
        }

        public Builder drawingTemplatePath(String drawingTemplatePath) {
            this.drawingTemplatePath = drawingTemplatePath;
            return this;
        }

        public Builder basePower(float basePower) {
            this.basePower = basePower;
            return this;
        }

        public Builder baseManaCost(float baseManaCost) {
            this.baseManaCost = baseManaCost;
            return this;
        }

        public Builder baseVariability(float baseVariability) {
            this.baseVariability = baseVariability;
            return this;
        }

        public Builder property(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            this.properties.putAll(properties);
            return this;
        }

        public GlyphAssetDefinition build() {
            return new GlyphAssetDefinition(this);
        }
    }

    @Override
    public String toString() {
        return "GlyphAssetDefinition{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", glyphId='" + glyphId + '\'' +
                ", basePower=" + basePower +
                ", baseManaCost=" + baseManaCost +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GlyphAssetDefinition that = (GlyphAssetDefinition) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
