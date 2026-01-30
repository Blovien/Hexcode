package com.riprod.hexcode.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.ColorLight;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.asset.type.modelvfx.config.ModelVFX;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.DynamicLight;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.data.GlyphInstance;
import com.riprod.hexcode.glyph.GlyphVisual;
import com.riprod.hexcode.hex.HexNode;
import com.riprod.hexcode.math.GlyphRotation;

/**
 * Unified entity representing a HexNode (single glyph or composed hex) floating
 * around a player.
 *
 * <p>
 * This replaces both GlyphEntity and HexEntity with a unified treatment where:
 * <ul>
 * <li>A single glyph is a HexNode with no children (leaf node)</li>
 * <li>A composed hex is a HexNode with children</li>
 * <li>Both are positioned using angular coordinates (yaw/pitch) from
 * hex-positioning</li>
 * <li>Both use entity-mounting for visual hierarchy</li>
 * </ul>
 *
 * <p>
 * Entity hierarchy:
 *
 * <pre>
 * Player
 *   └── Root HexNode entity (mounted to player via angular offset)
 *         └── Child HexNode entity (mounted to parent via local angular offset)
 *               └── Grandchild HexNode entity...
 * </pre>
 *
 * <p>
 * Hit testing uses the HexNode's findDeepestAt() for precise nested selection.
 */
public class HexNodeEntity {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final float BOUNDING_BOX_SIZE = 0.5f;

    /** The root HexNode this entity represents */
    private final HexNode node;

    /** Reference to the player who owns this orbital element */
    private final Ref<EntityStore> ownerPlayer;

    /** Tracks if entity is pending spawn (entityRef set but not yet valid) */
    private boolean pendingSpawn = false;

    /** Hover state flag */
    private HexNode hoveredNode = null;
    private HexNode previousHoveredNode = null;

    /** Dragging state flag */
    private boolean isDragging = false;

    /** Availability state (greyed out when false) */
    private boolean isAvailable = true;

    /**
     * Create a new HexNodeEntity for the given node.
     *
     * @param node        The root HexNode this entity represents
     * @param ownerPlayer Reference to the player who owns this element
     */
    public HexNodeEntity(HexNode node, Ref<EntityStore> ownerPlayer) {
        this.node = node;
        this.ownerPlayer = ownerPlayer;
    }

    // ========== ACCESSORS ==========

    /**
     * Get the root HexNode this entity represents.
     *
     * @return The root HexNode
     */
    public HexNode getNode() {
        return node;
    }

    // ========== ORBITAL ELEMENT INTERFACE ==========

    public String getId() {
        return node.getId();
    }

    public boolean isSavedHex() {
        // A "saved hex" is one with children (composed)
        return node.hasChildren();
    }

    public GlyphVisual getVisual() {
        if (node.getValue() != null && node.getValue().getGlyph() != null) {
            return node.getValue().getGlyph().getVisual();
        }
        // Return a default visual if no glyph
        // Note: Model loading will prepend "Glyphs/" to this ID
        return GlyphVisual.select("Base_Glyph");
    }

    public Ref<EntityStore> getEntityRef() {
        return node.getEntityRef();
    }

    public Ref<EntityStore> getOwnerPlayer() {
        return ownerPlayer;
    }

    public GlyphRotation getRotation() {
        return new GlyphRotation(node.getAbsolutePitch(), node.getAbsoluteYaw());
    }

    public void setRotation(GlyphRotation rotation) {
        node.setAbsoluteYaw(rotation.getYaw());
        node.setAbsolutePitch(rotation.getPitch());
    }

    public float getSelectionTolerance() {
        return node.getAngularRadius();
    }

    // ========== STATE MACHINE ==========

    public boolean isHovered() {
        return hoveredNode != null;
    }

    public HexNode getHoveredNode() {
        return hoveredNode;
    }

    public void setHovered(@Nullable HexNode targetNode) {
        if (targetNode == null) {
            this.previousHoveredNode = this.hoveredNode;
        } else {
            this.previousHoveredNode = null;
        }
        this.hoveredNode = targetNode;
    }

    public boolean isDragging() {
        return isDragging;
    }

    public void setDragging(boolean dragging) {
        this.isDragging = dragging;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        this.isAvailable = available;
    }

    public boolean isPendingSpawn() {
        return pendingSpawn;
    }

    public void clearPendingSpawn() {
        this.pendingSpawn = false;
    }

    // ========== SPAWNING ==========

    public void spawn(CommandBuffer<EntityStore> commandBuffer, Vector3d spawnPosition, Ref<EntityStore> playerRef) {
        if (node.getEntityRef() != null) {
            LOGGER.atWarning().log("HexNodeEntity '%s' already has entity ref", node.getId());
            return;
        }

        // Recalculate layout to ensure all positions are up to date
        node.recalculateLayout();

        // Spawn the entire node tree recursively
        // Pass spawnPosition so entities spawn in a loaded chunk near the player
        spawnNodeTree(commandBuffer, node, playerRef, true, spawnPosition);

        this.pendingSpawn = true;

        int nodeCount = countNodes(node);
        LOGGER.atInfo().log("Spawned HexNodeEntity '%s' with %d nodes at (%.1f, %.1f, %.1f)",
                node.getId(), nodeCount, spawnPosition.x, spawnPosition.y, spawnPosition.z);
    }

    /**
     * Recursively spawn entities for each node in the tree.
     *
     * <p>
     * Root nodes use OrbitalPositionComponent to track player position via ticking.
     * Child nodes use MountedComponent to attach to their parent entity.
     *
     * @param commandBuffer   The command buffer for deferred operations
     * @param currentNode     The current node to spawn
     * @param parentEntityRef The parent entity (player for root, parent node for
     *                        children)
     * @param isRoot          Whether this is the root node
     * @param spawnPosition   Position near the player to spawn entities (must be in
     *                        loaded chunk)
     */
    private void spawnNodeTree(CommandBuffer<EntityStore> commandBuffer, HexNode currentNode,
            Ref<EntityStore> parentEntityRef, boolean isRoot, Vector3d spawnPosition) {
        // Create entity holder for this node
        Holder<EntityStore> holder = createNodeHolder(commandBuffer, currentNode, isRoot, spawnPosition);

        // Spawn the entity
        Ref<EntityStore> nodeEntityRef = commandBuffer.addEntity(holder, AddReason.SPAWN);
        currentNode.setEntityRef(nodeEntityRef);

        if (isRoot) {
            // ROOT NODE: Use OrbitalPositionComponent for ticked player tracking
            // This avoids mount/unmount issues during drag operations
            if (OrbitalPositionComponent.getComponentType() != null) {
                OrbitalPositionComponent orbital = new OrbitalPositionComponent(
                        parentEntityRef, // player ref
                        currentNode.getAbsoluteYaw(),
                        currentNode.getAbsolutePitch(),
                        GlyphRotation.DEFAULT_DISTANCE);
                commandBuffer.addComponent(nodeEntityRef, OrbitalPositionComponent.getComponentType(), orbital);
                LOGGER.atInfo().log("Added OrbitalPositionComponent to root node '%s' at yaw=%.1f pitch=%.1f",
                        currentNode.getId(), currentNode.getAbsoluteYaw(), currentNode.getAbsolutePitch());
            }
        } else {
            // CHILD NODE: Use MountedComponent to attach to parent entity
            // This preserves the internal hex hierarchy
            Vector3f offset = currentNode.getMountOffset(1f);
            MountedComponent mounted = new MountedComponent(
                    parentEntityRef, // parent node's entity ref
                    offset,
                    MountController.Minecart);
            commandBuffer.addComponent(nodeEntityRef, MountedComponent.getComponentType(), mounted);
            LOGGER.atInfo().log(
                    "Mounted child node '%s' to parent at offset (%.2f, %.2f, %.2f). Current coordinates are (%.2f, %.2f, %.2f)",
                    currentNode.getId(), offset.x, offset.y, offset.z);
        }

        // Add HexNodeComponent for system tracking
        if (HexNodeComponent.getComponentType() != null) {
            HexNodeComponent nodeComp = new HexNodeComponent(ownerPlayer, currentNode.getId());
            commandBuffer.addComponent(nodeEntityRef, HexNodeComponent.getComponentType(), nodeComp);
        }

        // Recursively spawn children, mounting each to this node's entity
        for (HexNode child : currentNode.getChildren()) {
            spawnNodeTree(commandBuffer, child, nodeEntityRef, false, spawnPosition);
        }
    }

    /**
     * Create an entity holder for a single HexNode.
     *
     * @param commandBuffer The command buffer for external data access
     * @param node          The node to create an entity for
     * @param isRoot        Whether this is the root node (gets BoundingBox)
     * @param spawnPosition Position near the player to spawn at (must be in loaded
     *                      chunk)
     * @return The configured entity holder
     */
    private Holder<EntityStore> createNodeHolder(CommandBuffer<EntityStore> commandBuffer,
            HexNode node, boolean isRoot, Vector3d spawnPosition) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        // Add UUID component
        holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(UUID.randomUUID()));

        // Add transform - spawn at player position so it's in a loaded chunk
        // The mount system or orbital tracking will reposition it immediately
        TransformComponent transform = new TransformComponent(spawnPosition, new Vector3f(0, 0, 0));
        holder.addComponent(TransformComponent.getComponentType(), transform);

        // Add model from glyph visual
        GlyphVisual visual = null;
        if (node.getValue() != null && node.getValue().getGlyph() != null) {
            visual = node.getValue().getGlyph().getVisual();
            String modelId = visual.getModelId();

            // Asset keys are just the filename without extension (not the full path)
            // e.g., Server/Models/Glyphs/Fire.json has key "Fire"
            ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(modelId);

            if (modelAsset != null) {
                LOGGER.atInfo().log("Loaded model '%s' for node '%s' at coordinates (%.2f, %.2f, %.2f)", modelId,
                        node.getId(), transform.getPosition().x, transform.getPosition().y, transform.getPosition().z);
                Model model = Model.createUnitScaleModel(modelAsset);
                holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
                holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
            } else {
                LOGGER.atWarning().log("Could not load model '%s' for node '%s', trying fallback",
                        modelId, node.getId());
                loadFallbackModel(holder);
            }
        } else {
            loadFallbackModel(holder);
        }

        // Add dynamic light based on glyph color
        if (visual != null) {
            ColorLight colorLight = createColorLightFromGlyph(visual);
            holder.addComponent(DynamicLight.getComponentType(), new DynamicLight(colorLight));
        }

        EffectControllerComponent effectController = new EffectControllerComponent();
        // Add glow effect
        holder.addComponent(EffectControllerComponent.getComponentType(), effectController);

        // Add network ID
        holder.addComponent(NetworkId.getComponentType(),
                new NetworkId(commandBuffer.getExternalData().takeNextNetworkId()));

        // Add scale component based on node's computed scale
        holder.addComponent(EntityScaleComponent.getComponentType(),
                new EntityScaleComponent(node.getScale()));

        // Only root node gets BoundingBox for hit detection
        // Child nodes are visual-only (no collision/interaction)
        if (isRoot) {
            float boxSize = BOUNDING_BOX_SIZE * node.getScale();
            Box box = new Box(-boxSize, -boxSize, -boxSize, boxSize, boxSize, boxSize);
            holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(box));
        }

        return holder;
    }

    /**
     * Load fallback model when the requested model is not found.
     * Asset keys are just the filename without extension.
     */
    private void loadFallbackModel(Holder<EntityStore> holder) {
        // Try fallback models in order of preference
        // Asset keys are just filenames without extension
        String[] fallbackModels = {
                "Base_Glyph", // Plugin: Server/Models/Glyphs/Base_Glyph.json
        };

        for (String fallbackId : fallbackModels) {
            ModelAsset fallbackAsset = ModelAsset.getAssetMap().getAsset(fallbackId);
            if (fallbackAsset != null) {
                Model model = Model.createUnitScaleModel(fallbackAsset);
                holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
                holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
                LOGGER.atInfo().log("Using fallback model '%s'", fallbackId);
                return;
            }
        }

        // If all fallbacks fail, log what we tried
        LOGGER.atSevere().log("CRITICAL: Could not load ANY fallback model! Tried: %s. Entity will be invisible.",
                String.join(", ", fallbackModels));
    }

    // ========== DESPAWNING ==========

    public void despawn(CommandBuffer<EntityStore> commandBuffer) {
        // Despawn entire tree recursively
        int nodeCount = despawnNodeTree(commandBuffer, node);

        LOGGER.atInfo().log("Despawned HexNodeEntity '%s' with %d nodes", node.getId(), nodeCount);
    }

    /**
     * Recursively despawn entities for each node in the tree.
     *
     * @param commandBuffer The command buffer for deferred operations
     * @param currentNode   The current node to despawn
     * @return Number of nodes despawned
     */
    private int despawnNodeTree(CommandBuffer<EntityStore> commandBuffer, HexNode currentNode) {
        int count = 0;

        // Despawn children first (bottom-up)
        for (HexNode child : currentNode.getChildren()) {
            count += despawnNodeTree(commandBuffer, child);
        }

        // Despawn this node's entity
        Ref<EntityStore> entityRef = currentNode.getEntityRef();
        if (entityRef != null && entityRef.isValid()) {
            commandBuffer.removeEntity(entityRef, RemoveReason.REMOVE);
            count++;
        }
        currentNode.setEntityRef(null);

        return count;
    }

    // ========== POSITION UPDATES ==========

    /** Player eye height offset */
    private static final float EYE_HEIGHT = 1.62f;

    public void updatePositionFromPlayer(Store<EntityStore> store, Vector3d playerPosition) {
        Ref<EntityStore> entityRef = node.getEntityRef();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        // Clear pending flag on first valid update
        if (pendingSpawn) {
            pendingSpawn = false;
            LOGGER.atInfo().log("HexNodeEntity '%s' entity became valid", node.getId());
        }

        // Get OrbitalPositionComponent for this entity
        if (OrbitalPositionComponent.getComponentType() == null) {
            return;
        }
        OrbitalPositionComponent orbital = store.getComponent(entityRef, OrbitalPositionComponent.getComponentType());
        if (orbital == null) {
            return;
        }

        // Skip if paused (entity is being dragged)
        if (orbital.isPaused()) {
            return;
        }

        // Calculate orbital position from yaw/pitch/distance
        float yaw = orbital.getYaw();
        float pitch = orbital.getPitch();
        float distance = orbital.getDistance();

        // Convert to radians
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        // Calculate offset from player
        // yaw 0 = +Z (forward), yaw 90 = +X (right)
        // pitch 0 = level, pitch positive = down, pitch negative = up
        double cosP = Math.cos(pitchRad);
        double sinP = Math.sin(pitchRad);
        double cosY = Math.cos(yawRad);
        double sinY = Math.sin(yawRad);

        double offsetX = sinY * cosP * distance;
        double offsetY = -sinP * distance;
        double offsetZ = cosY * cosP * distance;

        // Calculate world position (at eye height)
        double worldX = playerPosition.x + offsetX;
        double worldY = playerPosition.y + EYE_HEIGHT + offsetY;
        double worldZ = playerPosition.z + offsetZ;
        
        float faceYaw = yaw + 180.0f;
        if (faceYaw > 180.0f) {
            faceYaw -= 360.0f;
        }
        float facePitch = -pitch;
        // Update entity position
        TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform != null) {
            transform.getPosition().assign(worldX, worldY, worldZ);

            // Update entity rotation to face the player

            transform.getRotation().assign((float) Math.toRadians(facePitch), (float) Math.toRadians(faceYaw), 0);
        }

        List<HexNode> hexChildren = node.getAllChildren();

        for (HexNode child : hexChildren) {
            Ref<EntityStore> childRef = child.getEntityRef();
            if (childRef == null || !childRef.isValid()) {
                continue;
            }

            TransformComponent childTransform = store.getComponent(childRef, TransformComponent.getComponentType());
            if (childTransform != null) {
                childTransform.getRotation().assign((float) Math.toRadians(facePitch), (float) Math.toRadians(faceYaw), 0);
            }
        }
    }

    public void captureRotationFromLook(Store<EntityStore> store, GlyphRotation lookRotation) {
        node.setAbsoluteYaw(lookRotation.getYaw());
        node.setAbsolutePitch(lookRotation.getPitch());
        LOGGER.atInfo().log("HexNodeEntity '%s' captured rotation: %s", node.getId(), lookRotation);
    }

    public void updateWorldPositionDirect(Store<EntityStore> store, Vector3d position, Vector3f lookDirection) {
        Ref<EntityStore> entityRef = node.getEntityRef();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform != null) {
            transform.setPosition(position);
            transform.setRotation(lookDirection);
        }

        List<HexNode> hexChildren = node.getAllChildren();

        if (hexChildren.isEmpty()) {
            return;
        }

        for (HexNode child : hexChildren) {
            Ref<EntityStore> childRef = child.getEntityRef();
            if (childRef == null || !childRef.isValid()) {
                continue;
            }

            TransformComponent childTransform = store.getComponent(childRef, TransformComponent.getComponentType());
            if (childTransform != null) {
                Vector3f offset = child.getMountOffset(1f);
                Vector3d childPosition = new Vector3d(
                        position.x + offset.x,
                        position.y + offset.y,
                        position.z + offset.z);
                childTransform.setPosition(childPosition);
                childTransform.setRotation(lookDirection);
            }
        }
    }

    public void updateHoverVisual(Store<EntityStore> store) {
        boolean hasHovered = hoveredNode != null;
        boolean hasPrevHovered = previousHoveredNode != null;
        HexNode targetNode = hasHovered ? hoveredNode : hasPrevHovered ? previousHoveredNode : node;

        Ref<EntityStore> nodeRef = targetNode.getEntityRef();

        if (nodeRef == null || !nodeRef.isValid()) {
            return;
        }

        EntityScaleComponent scaleComp = store.getComponent(nodeRef, EntityScaleComponent.getComponentType());
        if (scaleComp != null) {
            float baseScale = targetNode.getScale();
            float targetScale = hasHovered ? baseScale * 1.1f : baseScale;
            if (Math.abs(scaleComp.getScale() - targetScale) > 0.001f) {
                scaleComp.setScale(targetScale);
            }
        }

        previousHoveredNode = null;
    }

    // ========== REFRESH AND RESPAWN ==========

    /**
     * Refresh and respawn all entities after structure changes.
     * Call this after modifying the HexNode tree (e.g., adding children).
     *
     * @param commandBuffer The command buffer for deferred operations
     * @param playerRef     The player reference for mounting
     * @param spawnPosition Position near the player to spawn at (must be in loaded
     *                      chunk)
     */
    public void refreshAndRespawn(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> playerRef,
            Vector3d spawnPosition) {
        // Despawn all existing entities
        despawnNodeTree(commandBuffer, node);

        // Recalculate layout with new structure
        node.recalculateLayout();

        // Respawn with updated structure at player position
        spawnNodeTree(commandBuffer, node, playerRef, true, spawnPosition);

        this.pendingSpawn = true;

        int nodeCount = countNodes(node);
        LOGGER.atInfo().log("Refreshed and respawned HexNodeEntity '%s' with %d nodes", node.getId(), nodeCount);
    }

    /**
     * Pause orbital tracking for this entity (for dragging).
     * The OrbitalTrackingSystem will skip this entity, allowing direct position
     * control.
     *
     * @param store The entity store to get the component from
     */
    public void pauseOrbitalTracking(Store<EntityStore> store) {
        Ref<EntityStore> entityRef = node.getEntityRef();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        if (OrbitalPositionComponent.getComponentType() != null) {
            OrbitalPositionComponent orbital = store.getComponent(entityRef,
                    OrbitalPositionComponent.getComponentType());
            if (orbital != null) {
                orbital.setPaused(true);
                LOGGER.atInfo().log("Paused orbital tracking for '%s'", node.getId());
            }
        }
    }

    /**
     * Resume orbital tracking for this entity after dragging.
     * Updates the orbital position to the new yaw/pitch and resumes ticking.
     *
     * @param store     The entity store to get the component from
     * @param playerRef The player reference (for updating target if needed)
     */
    public void resumeOrbitalTracking(Store<EntityStore> store, Ref<EntityStore> playerRef) {
        Ref<EntityStore> entityRef = node.getEntityRef();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        if (OrbitalPositionComponent.getComponentType() != null) {
            OrbitalPositionComponent orbital = store.getComponent(entityRef,
                    OrbitalPositionComponent.getComponentType());
            if (orbital != null) {
                // Update orbital position from node's current absolute rotation
                orbital.setYaw(node.getAbsoluteYaw());
                orbital.setPitch(node.getAbsolutePitch());
                orbital.setTargetRef(playerRef);
                orbital.setPaused(false);
                LOGGER.atInfo().log("Resumed orbital tracking for '%s' at yaw=%.1f pitch=%.1f",
                        node.getId(), node.getAbsoluteYaw(), node.getAbsolutePitch());
            }
        }
    }

    /**
     * @deprecated Use {@link #pauseOrbitalTracking(Store)} instead.
     *             Kept for backwards compatibility during transition.
     */
    @Deprecated
    public void unmountFromPlayer(CommandBuffer<EntityStore> commandBuffer) {
        // No-op: Use pauseOrbitalTracking(store) instead
        LOGGER.atWarning().log("unmountFromPlayer() is deprecated, use pauseOrbitalTracking()");
    }

    /**
     * @deprecated Use {@link #resumeOrbitalTracking(Store, Ref)} instead.
     *             Kept for backwards compatibility during transition.
     */
    @Deprecated
    public void remountToPlayer(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> playerRef) {
        // No-op: Use resumeOrbitalTracking(store, playerRef) instead
        LOGGER.atWarning().log("remountToPlayer() is deprecated, use resumeOrbitalTracking()");
    }

    // ========== UTILITY ==========

    /**
     * Count total nodes in a tree.
     */
    private int countNodes(HexNode root) {
        int count = 1;
        for (HexNode child : root.getChildren()) {
            count += countNodes(child);
        }
        return count;
    }

    /**
     * Create a ColorLight from glyph visual properties.
     */
    private ColorLight createColorLightFromGlyph(GlyphVisual visual) {
        int color = visual.getColor();
        float intensity = visual.getGlowIntensity();

        // Extract RGB from color int
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        // Scale RGB by intensity
        red = (int) (red * intensity);
        green = (int) (green * intensity);
        blue = (int) (blue * intensity);

        ColorLight colorLight = new ColorLight();
        colorLight.radius = (byte) Math.max(1, Math.min(255, (int) intensity));
        colorLight.red = (byte) red;
        colorLight.green = (byte) green;
        colorLight.blue = (byte) blue;

        return colorLight;
    }

    public String toString() {
        return String.format("HexNodeEntity[id=%s, hasChildren=%s]",
                node.getId(), node.hasChildren());
    }
}
