package com.riprod.hexcode.entity;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

/**
 * Component for entities that can be selected and manipulated with a Glyph Wand.
 *
 * Tracks selection state and provides the glyph identifier for the entity.
 * Used by the glyph editor system to enable right-click selection and click-drag movement.
 */
public class SelectableGlyphComponent implements Component<EntityStore> {
    private static ComponentType<EntityStore, SelectableGlyphComponent> componentType;

    /**
     * CODEC for serialization - required for ECS component registration.
     */
    public static final BuilderCodec<SelectableGlyphComponent> CODEC =
        BuilderCodec.builder(SelectableGlyphComponent.class, SelectableGlyphComponent::new)
            .append(
                new KeyedCodec<>("GlyphId", Codec.STRING),
                (c, v) -> c.glyphId = v,
                c -> c.glyphId
            )
            .add()
            .append(
                new KeyedCodec<>("IsSelected", Codec.BOOLEAN),
                (c, v) -> c.isSelected = v,
                c -> c.isSelected
            )
            .add()
            .append(
                new KeyedCodec<>("EntityUUID", Codec.UUID_STRING),
                (c, v) -> c.entityUUID = v,
                c -> c.entityUUID
            )
            .add()
            .append(
                new KeyedCodec<>("ModelAssetId", Codec.STRING),
                (c, v) -> c.modelAssetId = v,
                c -> c.modelAssetId
            )
            .add()
            .build();

    private String glyphId;
    private boolean isSelected;
    private UUID entityUUID;
    private String modelAssetId;

    /**
     * Default constructor required for ECS component registration.
     */
    public SelectableGlyphComponent() {
        this.glyphId = "";
        this.isSelected = false;
        this.entityUUID = null;
        this.modelAssetId = "";
    }

    /**
     * Create a new selectable glyph component.
     *
     * @param glyphId The unique identifier for this glyph type
     */
    public SelectableGlyphComponent(String glyphId) {
        this.glyphId = glyphId;
        this.isSelected = false;
        this.entityUUID = UUID.randomUUID();
        this.modelAssetId = glyphId;
    }

    /**
     * Create a new selectable glyph component with a specific model.
     *
     * @param glyphId The unique identifier for this glyph type
     * @param modelAssetId The model asset ID to use for rendering
     */
    public SelectableGlyphComponent(String glyphId, String modelAssetId) {
        this.glyphId = glyphId;
        this.isSelected = false;
        this.entityUUID = UUID.randomUUID();
        this.modelAssetId = modelAssetId;
    }

    /**
     * @return The glyph type identifier
     */
    public String getGlyphId() {
        return glyphId;
    }

    /**
     * @return The unique UUID for this entity instance
     */
    public UUID getEntityUUID() {
        return entityUUID;
    }

    /**
     * @return The model asset ID used for rendering
     */
    public String getModelAssetId() {
        return modelAssetId;
    }

    /**
     * Set the model asset ID.
     *
     * @param modelAssetId The model asset ID
     */
    public void setModelAssetId(String modelAssetId) {
        this.modelAssetId = modelAssetId;
    }

    /**
     * @return true if this entity is currently selected
     */
    public boolean isSelected() {
        return isSelected;
    }

    /**
     * Set the selection state.
     *
     * @param selected true to select, false to deselect
     */
    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    /**
     * Toggle the selection state.
     *
     * @return The new selection state
     */
    public boolean toggleSelected() {
        this.isSelected = !this.isSelected;
        return this.isSelected;
    }

    /**
     * Get the component type for registration.
     */
    public static ComponentType<EntityStore, SelectableGlyphComponent> getComponentType() {
        return componentType;
    }

    /**
     * Set the component type (called during plugin initialization).
     */
    public static void setComponentType(ComponentType<EntityStore, SelectableGlyphComponent> type) {
        componentType = type;
    }

    /**
     * Clone this component (required by Component interface).
     */
    @Override
    public Component<EntityStore> clone() {
        SelectableGlyphComponent cloned = new SelectableGlyphComponent(glyphId, modelAssetId);
        cloned.isSelected = this.isSelected;
        cloned.entityUUID = this.entityUUID;
        return cloned;
    }
}
