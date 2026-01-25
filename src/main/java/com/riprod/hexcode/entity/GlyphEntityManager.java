package com.riprod.hexcode.entity;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all spawned selectable glyph entities and tracks selection state.
 *
 * This manager provides:
 * - Tracking of all spawned glyph entities
 * - Single-selection state (one glyph selected at a time per player)
 * - Lookup of entities by UUID or entity reference
 */
public class GlyphEntityManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static GlyphEntityManager instance;

    // All spawned glyph entities, keyed by their UUID
    private final Map<UUID, SelectableGlyphEntity> glyphEntities;

    // Currently selected glyph per player (player UUID -> glyph entity UUID)
    private final Map<UUID, UUID> playerSelections;

    // Map from entity ref to glyph UUID for quick lookup
    private final Map<Ref<EntityStore>, UUID> refToUuid;

    private GlyphEntityManager() {
        this.glyphEntities = new ConcurrentHashMap<>();
        this.playerSelections = new ConcurrentHashMap<>();
        this.refToUuid = new ConcurrentHashMap<>();
    }

    /**
     * Get the singleton instance.
     *
     * @return The GlyphEntityManager instance
     */
    public static synchronized GlyphEntityManager getInstance() {
        if (instance == null) {
            instance = new GlyphEntityManager();
        }
        return instance;
    }

    /**
     * Spawn a new glyph entity and register it.
     *
     * @param store The entity store
     * @param glyphId The glyph type identifier
     * @param position World position to spawn at
     * @return The spawned entity, or null if spawn failed
     */
    public SelectableGlyphEntity spawnGlyph(Store<EntityStore> store, String glyphId, Vector3d position) {
        return spawnGlyph(store, glyphId, glyphId, position);
    }

    /**
     * Spawn a new glyph entity with a specific model and register it.
     *
     * @param store The entity store
     * @param glyphId The glyph type identifier
     * @param modelAssetId The model asset ID
     * @param position World position to spawn at
     * @return The spawned entity, or null if spawn failed
     */
    public SelectableGlyphEntity spawnGlyph(Store<EntityStore> store, String glyphId,
                                             String modelAssetId, Vector3d position) {
        SelectableGlyphEntity entity = new SelectableGlyphEntity(glyphId, modelAssetId);

        if (entity.spawn(store, position)) {
            UUID entityUUID = entity.getEntityUUID();
            glyphEntities.put(entityUUID, entity);
            refToUuid.put(entity.getEntityRef(), entityUUID);
            LOGGER.atInfo().log("Registered glyph entity '%s' with UUID %s", glyphId, entityUUID);
            return entity;
        }

        return null;
    }

    /**
     * Despawn and unregister a glyph entity.
     *
     * @param store The entity store
     * @param entityUUID The UUID of the entity to despawn
     */
    public void despawnGlyph(Store<EntityStore> store, UUID entityUUID) {
        SelectableGlyphEntity entity = glyphEntities.remove(entityUUID);
        if (entity != null) {
            if (entity.getEntityRef() != null) {
                refToUuid.remove(entity.getEntityRef());
            }
            entity.despawn(store);

            // Clear any player selections of this entity
            playerSelections.entrySet().removeIf(entry -> entityUUID.equals(entry.getValue()));
        }
    }

    /**
     * Despawn all glyph entities.
     *
     * @param store The entity store
     */
    public void despawnAll(Store<EntityStore> store) {
        for (SelectableGlyphEntity entity : glyphEntities.values()) {
            entity.despawn(store);
        }
        glyphEntities.clear();
        refToUuid.clear();
        playerSelections.clear();
        LOGGER.atInfo().log("Despawned all glyph entities");
    }

    /**
     * Get a glyph entity by its UUID.
     *
     * @param entityUUID The entity UUID
     * @return The entity, or null if not found
     */
    public SelectableGlyphEntity getGlyph(UUID entityUUID) {
        return glyphEntities.get(entityUUID);
    }

    /**
     * Get a glyph entity by its entity reference.
     *
     * @param entityRef The entity reference
     * @return The entity, or null if not found
     */
    public SelectableGlyphEntity getGlyphByRef(Ref<EntityStore> entityRef) {
        UUID uuid = refToUuid.get(entityRef);
        if (uuid != null) {
            return glyphEntities.get(uuid);
        }
        return null;
    }

    /**
     * Get the UUID for an entity reference.
     *
     * @param entityRef The entity reference
     * @return The UUID, or null if not found
     */
    public UUID getUuidByRef(Ref<EntityStore> entityRef) {
        return refToUuid.get(entityRef);
    }

    /**
     * Set the selected glyph for a player.
     *
     * Deselects any previously selected glyph for this player.
     *
     * @param store The entity store
     * @param playerId The player's UUID
     * @param glyphEntityUUID The UUID of the glyph entity to select
     */
    public void setSelectedGlyph(Store<EntityStore> store, UUID playerId, UUID glyphEntityUUID) {
        // Clear previous selection
        clearSelection(store, playerId);

        if (glyphEntityUUID != null) {
            SelectableGlyphEntity entity = glyphEntities.get(glyphEntityUUID);
            if (entity != null && entity.isSpawned()) {
                entity.setSelected(store, true);
                playerSelections.put(playerId, glyphEntityUUID);
                LOGGER.atInfo().log("Player %s selected glyph entity %s", playerId, glyphEntityUUID);
            }
        }
    }

    /**
     * Clear the player's current glyph selection.
     *
     * @param store The entity store
     * @param playerId The player's UUID
     */
    public void clearSelection(Store<EntityStore> store, UUID playerId) {
        UUID previousSelection = playerSelections.remove(playerId);
        if (previousSelection != null) {
            SelectableGlyphEntity entity = glyphEntities.get(previousSelection);
            if (entity != null && entity.isSpawned()) {
                entity.setSelected(store, false);
            }
            LOGGER.atInfo().log("Player %s cleared selection (was %s)", playerId, previousSelection);
        }
    }

    /**
     * Get the currently selected glyph entity for a player.
     *
     * @param playerId The player's UUID
     * @return The selected entity, or null if none selected
     */
    public SelectableGlyphEntity getSelectedGlyph(UUID playerId) {
        UUID selectedUUID = playerSelections.get(playerId);
        if (selectedUUID != null) {
            return glyphEntities.get(selectedUUID);
        }
        return null;
    }

    /**
     * Get the UUID of the currently selected glyph for a player.
     *
     * @param playerId The player's UUID
     * @return The selected entity UUID, or null if none selected
     */
    public UUID getSelectedGlyphUUID(UUID playerId) {
        return playerSelections.get(playerId);
    }

    /**
     * Check if a player has a glyph selected.
     *
     * @param playerId The player's UUID
     * @return true if the player has a glyph selected
     */
    public boolean hasSelection(UUID playerId) {
        return playerSelections.containsKey(playerId);
    }

    /**
     * Toggle selection of a glyph entity for a player.
     *
     * @param store The entity store
     * @param playerId The player's UUID
     * @param glyphEntityUUID The UUID of the glyph entity
     * @return true if the entity is now selected, false if deselected
     */
    public boolean toggleSelection(Store<EntityStore> store, UUID playerId, UUID glyphEntityUUID) {
        UUID currentSelection = playerSelections.get(playerId);

        if (glyphEntityUUID.equals(currentSelection)) {
            // Already selected - deselect
            clearSelection(store, playerId);
            return false;
        } else {
            // Not selected - select it
            setSelectedGlyph(store, playerId, glyphEntityUUID);
            return true;
        }
    }

    /**
     * Get the number of spawned glyph entities.
     *
     * @return The count
     */
    public int getGlyphCount() {
        return glyphEntities.size();
    }

    /**
     * Get all spawned glyph entities.
     *
     * @return Map of UUID to entity
     */
    public Map<UUID, SelectableGlyphEntity> getAllGlyphs() {
        return new HashMap<>(glyphEntities);
    }
}
