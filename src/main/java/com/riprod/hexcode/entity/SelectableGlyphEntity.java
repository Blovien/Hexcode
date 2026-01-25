package com.riprod.hexcode.entity;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

/**
 * Entity wrapper for spawning and manipulating selectable glyph entities.
 *
 * This class provides methods to:
 * - Spawn a glyph entity at a given position
 * - Update the entity's position (for drag movement)
 * - Despawn the entity
 *
 * The spawned entity has no NPC/AI behavior and can be selected via right-click
 * when the player is holding a Glyph Wand.
 */
public class SelectableGlyphEntity {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final float DEFAULT_SCALE = 0.5f;
    private static final float BOUNDING_BOX_SIZE = 0.25f;

    private final String glyphId;
    private final String modelAssetId;
    private final float scale;

    private Ref<EntityStore> entityRef;
    private UUID entityUUID;

    /**
     * Create a new selectable glyph entity wrapper.
     *
     * @param glyphId The glyph type identifier
     */
    public SelectableGlyphEntity(String glyphId) {
        this(glyphId, glyphId, DEFAULT_SCALE);
    }

    /**
     * Create a new selectable glyph entity wrapper with a specific model.
     *
     * @param glyphId      The glyph type identifier
     * @param modelAssetId The model asset ID to use for rendering
     */
    public SelectableGlyphEntity(String glyphId, String modelAssetId) {
        this(glyphId, modelAssetId, DEFAULT_SCALE);
    }

    /**
     * Create a new selectable glyph entity wrapper with a specific model and scale.
     *
     * @param glyphId      The glyph type identifier
     * @param modelAssetId The model asset ID to use for rendering
     * @param scale        The visual scale of the entity
     */
    public SelectableGlyphEntity(String glyphId, String modelAssetId, float scale) {
        this.glyphId = glyphId;
        this.modelAssetId = modelAssetId;
        this.scale = scale;
        this.entityRef = null;
        this.entityUUID = null;
    }

    /**
     * Spawn this glyph entity in the world.
     *
     * Creates an entity with:
     * - UUIDComponent for unique identification
     * - TransformComponent for position/rotation
     * - ModelComponent for visual representation
     * - Interactable component to enable right-click targeting
     * - BoundingBox for hit detection
     * - SelectableGlyphComponent for selection state tracking
     *
     * @param store    The entity store
     * @param position World position to spawn at
     * @return true if spawn was successful
     */
    public boolean spawn(Store<EntityStore> store, Vector3d position) {
        if (entityRef != null) {
            LOGGER.atWarning().log("Glyph entity '%s' already spawned", glyphId);
            return false;
        }

        // Create entity holder
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        // Add UUID component - required for unique identification
        entityUUID = UUID.randomUUID();
        holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(entityUUID));

        // Add transform component - position and rotation
        TransformComponent transform = new TransformComponent(position, new Vector3f(0, 0, 0));
        holder.addComponent(TransformComponent.getComponentType(), transform);

        // Add model component - visual representation
        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(modelAssetId);
        if (modelAsset != null) {
            Model model = Model.createScaledModel(modelAsset, scale);
            holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
            holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));

        } else {
            LOGGER.atWarning().log("ModelAsset '%s' not found for glyph '%s' - trying fallback",
                    modelAssetId, glyphId);

            // Try Base_glyph as fallback
            ModelAsset fallbackAsset = ModelAsset.getAssetMap().getAsset("Base_glyph");
            if (fallbackAsset != null) {
                Model model = Model.createScaledModel(fallbackAsset, scale);
                holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
                holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));

            } else {
                LOGGER.atSevere().log("No fallback model available - entity may be invisible");
            }
        }

        // Add Interactable component - THIS IS KEY for right-click targeting
        holder.addComponent(Interactable.getComponentType(), Interactable.INSTANCE);

        // Add BoundingBox for hit detection
        Box box = new Box(
                -BOUNDING_BOX_SIZE, -BOUNDING_BOX_SIZE, -BOUNDING_BOX_SIZE,
                BOUNDING_BOX_SIZE, BOUNDING_BOX_SIZE, BOUNDING_BOX_SIZE);
        holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(box));

        holder.addComponent(NetworkId.getComponentType(),
                new NetworkId(store.getExternalData().takeNextNetworkId()));

        // Add SelectableGlyphComponent for selection state tracking
        SelectableGlyphComponent selectableComp = new SelectableGlyphComponent(glyphId, modelAssetId);
        if (SelectableGlyphComponent.getComponentType() != null) {
            holder.addComponent(SelectableGlyphComponent.getComponentType(), selectableComp);
        } else {
            LOGGER.atWarning().log("SelectableGlyphComponent type not registered - selection won't work");
        }

        // Add entity to store
        entityRef = store.addEntity(holder, AddReason.SPAWN);

        LOGGER.atInfo().log("Spawned selectable glyph entity '%s' at (%.1f, %.1f, %.1f) with UUID %s",
                glyphId, position.x, position.y, position.z, entityUUID);

        return true;
    }

    /**
     * Update the entity's position.
     *
     * @param store       The entity store
     * @param newPosition The new world position
     */
    public void updatePosition(Store<EntityStore> store, Vector3d newPosition) {
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform != null) {
            transform.setPosition(newPosition);
        }
    }

    /**
     * Despawn this glyph entity.
     *
     * @param store The entity store
     */
    public void despawn(Store<EntityStore> store) {
        if (entityRef != null && entityRef.isValid()) {
            store.removeEntity(entityRef, RemoveReason.REMOVE);
            LOGGER.atInfo().log("Despawned selectable glyph entity '%s'", glyphId);
        }
        entityRef = null;
        entityUUID = null;
    }

    /**
     * Check if the entity is currently spawned.
     *
     * @return true if the entity is spawned and valid
     */
    public boolean isSpawned() {
        return entityRef != null && entityRef.isValid();
    }

    /**
     * Get the entity reference.
     *
     * @return The entity reference, or null if not spawned
     */
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    /**
     * Get the entity's UUID.
     *
     * @return The entity UUID, or null if not spawned
     */
    public UUID getEntityUUID() {
        return entityUUID;
    }

    /**
     * Get the glyph type identifier.
     *
     * @return The glyph ID
     */
    public String getGlyphId() {
        return glyphId;
    }

    /**
     * Get the model asset ID.
     *
     * @return The model asset ID
     */
    public String getModelAssetId() {
        return modelAssetId;
    }

    /**
     * Get the visual scale.
     *
     * @return The scale
     */
    public float getScale() {
        return scale;
    }

    /**
     * Get the current position of the entity.
     *
     * @param store The entity store
     * @return The current position, or null if not spawned
     */
    public Vector3d getPosition(Store<EntityStore> store) {
        if (entityRef == null || !entityRef.isValid()) {
            return null;
        }

        TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform != null) {
            return new Vector3d(transform.getPosition());
        }
        return null;
    }

    /**
     * Check if this entity is currently selected.
     *
     * @param store The entity store
     * @return true if selected
     */
    public boolean isSelected(Store<EntityStore> store) {
        if (entityRef == null || !entityRef.isValid()) {
            return false;
        }

        if (SelectableGlyphComponent.getComponentType() == null) {
            return false;
        }

        SelectableGlyphComponent selectableComp = store.getComponent(
                entityRef, SelectableGlyphComponent.getComponentType());
        return selectableComp != null && selectableComp.isSelected();
    }

    /**
     * Set the selection state of this entity.
     *
     * @param store    The entity store
     * @param selected true to select, false to deselect
     */
    public void setSelected(Store<EntityStore> store, boolean selected) {
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        if (SelectableGlyphComponent.getComponentType() == null) {
            return;
        }

        SelectableGlyphComponent selectableComp = store.getComponent(
                entityRef, SelectableGlyphComponent.getComponentType());
        if (selectableComp != null) {
            selectableComp.setSelected(selected);
        }
    }
}
