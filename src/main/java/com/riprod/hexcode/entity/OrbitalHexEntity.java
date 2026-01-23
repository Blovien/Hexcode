package com.riprod.hexcode.entity;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.ColorLight;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.DynamicLight;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.config.HexcodeConfig;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.hex.Hex;
import com.riprod.hexcode.util.HexMathUtil;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Entity representing a saved hex in the orbital ring around a player.
 *
 * Similar to OrbitalGlyphEntity but for saved hexes.
 * Uses a distinct visual style (purple glow) to differentiate from individual glyphs.
 *
 * @see OrbitalGlyphEntity
 * @see SavedHexElement
 */
public class OrbitalHexEntity {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Purple color for saved hex visuals */
    private static final int SAVED_HEX_COLOR = 0x9966FF;

    private final Hex hex;
    private final Ref<EntityStore> ownerPlayer;
    private Ref<EntityStore> entityRef;

    private float orbitAngle;
    private float orbitSpeed;
    private float orbitalRadius;
    private float height;

    private boolean isHovered;
    private boolean isDragging;
    private boolean isAvailable;

    // Visual configuration
    private float pulsePhase;

    public OrbitalHexEntity(@Nonnull Hex hex,
                                  @Nonnull Ref<EntityStore> ownerPlayer,
                                  float initialAngle) {
        this.hex = hex;
        this.ownerPlayer = ownerPlayer;
        this.orbitAngle = initialAngle;

        HexcodeConfig config = HexcodeConfig.getInstance();
        this.orbitSpeed = config.getOrbitSpeed();
        this.orbitalRadius = config.getOrbitalRadius();
        this.height = 1.0f; // Chest height offset

        this.isHovered = false;
        this.isDragging = false;
        this.isAvailable = true;

        this.pulsePhase = 0.0f;
    }

    // ==================== GETTERS ====================

    /**
     * @return The saved hex element this entity represents
     */
    @Nonnull
    public Hex getHex() {
        return hex;
    }

    /**
     * @return The player who owns this orbital entity
     */
    @Nonnull
    public Ref<EntityStore> getOwnerPlayer() {
        return ownerPlayer;
    }

    /**
     * @return The entity reference (if spawned)
     */
    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }

    /**
     * @return Current orbit angle in radians
     */
    public float getOrbitAngle() {
        return orbitAngle;
    }

    /**
     * @return Current orbit speed
     */
    public float getOrbitSpeed() {
        return orbitSpeed;
    }

    /**
     * @return Orbital radius
     */
    public float getOrbitalRadius() {
        return orbitalRadius;
    }

    // ==================== STATE ====================

    /**
     * @return true if this entity is being hovered
     */
    public boolean isHovered() {
        return isHovered;
    }

    /**
     * Set hover state.
     */
    public void setHovered(boolean hovered) {
        this.isHovered = hovered;
    }

    /**
     * @return true if this entity is being dragged
     */
    public boolean isDragging() {
        return isDragging;
    }

    /**
     * Set dragging state.
     */
    public void setDragging(boolean dragging) {
        this.isDragging = dragging;
    }

    /**
     * @return true if this saved hex is available for use
     */
    public boolean isAvailable() {
        return isAvailable;
    }

    /**
     * Set availability.
     */
    public void setAvailable(boolean available) {
        this.isAvailable = available;
    }

    // ==================== UPDATE ====================

    /**
     * Update the orbital position based on delta time.
     *
     * @param dt Delta time in seconds
     */
    public void update(float dt) {
        if (!isDragging) {
            orbitAngle += orbitSpeed * dt;
            if (orbitAngle > Math.PI * 2) {
                orbitAngle -= Math.PI * 2;
            }
        }

        // Update pulse phase for visual effect
        pulsePhase += dt * 2.0f;
        if (pulsePhase > Math.PI * 2) {
            pulsePhase -= Math.PI * 2;
        }
    }

    /**
     * Calculate the world position of this entity.
     *
     * @param playerPosition The player's current position
     * @return World position of the entity
     */
    public Vector3d calculatePosition(Vector3d playerPosition) {
        return HexMathUtil.calculateOrbitalPosition(playerPosition, orbitalRadius, orbitAngle, height);
    }

    // ==================== SPAWNING ====================

    /**
     * Spawn this orbital saved hex entity in the world.
     *
     * @param commandBuffer  The command buffer for deferred entity operations
     * @param playerPosition The player's position
     */
    public void spawn(CommandBuffer<EntityStore> commandBuffer, Vector3d playerPosition) {
        spawn(commandBuffer, playerPosition, null);
    }

    /**
     * Spawn this orbital saved hex entity in the world.
     *
     * @param commandBuffer The command buffer for deferred entity operations
     * @param playerPosition The player's position
     * @param ownerPlayerId The owner player's UUID
     */
    public void spawn(CommandBuffer<EntityStore> commandBuffer, Vector3d playerPosition, UUID ownerPlayerId) {
        if (entityRef != null) {
            LOGGER.atWarning().log("Orbital saved hex '%s' already spawned", hex.getId());
            return;
        }

        Vector3d position = calculatePosition(playerPosition);

        // Create entity holder
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        // Add UUID component
        holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(UUID.randomUUID()));

        // Add transform component with position
        TransformComponent transform = new TransformComponent(position, new Vector3f(0, 0, 0));
        holder.addComponent(TransformComponent.getComponentType(), transform);

        // Add dynamic light with purple glow
        ColorLight colorLight = createColorLight();
        DynamicLight dynamicLight = new DynamicLight(colorLight);
        holder.addComponent(DynamicLight.getComponentType(), dynamicLight);

        // Note: No model component - saved hexes use particle-based rendering
        // Could add OrbitalSavedHexComponent here if needed for server-side tracking

        // Add entity to command buffer (deferred execution after tick completes)
        entityRef = commandBuffer.addEntity(holder, AddReason.SPAWN);

        LOGGER.atInfo().log("Spawned orbital saved hex '%s' at (%.1f, %.1f, %.1f)",
                hex.getId(), position.x, position.y, position.z);
    }

    /**
     * Despawn this orbital saved hex entity.
     *
     * @param commandBuffer The command buffer for deferred entity operations
     */
    public void despawn(CommandBuffer<EntityStore> commandBuffer) {
        if (entityRef != null) {
            commandBuffer.removeEntity(entityRef, RemoveReason.REMOVE);
            LOGGER.atInfo().log("Despawned orbital saved hex '%s'", hex.getId());
            entityRef = null;
        }
    }

    // ==================== VISUAL UPDATES ====================

    /**
     * Update the entity's position in the world.
     *
     * @param store          The entity store
     * @param playerPosition The player's current position
     */
    public void updateWorldPosition(Store<EntityStore> store, Vector3d playerPosition) {
        if (entityRef == null) {
            return;
        }

        TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform != null) {
            Vector3d newPosition = calculatePosition(playerPosition);
            transform.setPosition(newPosition);
        }
    }

    /**
     * Update the hover highlight visual.
     *
     * @param store The entity store
     */
    public void updateHoverVisual(Store<EntityStore> store) {
        if (entityRef == null) {
            return;
        }

        DynamicLight light = store.getComponent(entityRef, DynamicLight.getComponentType());
        if (light != null) {
            ColorLight colorLight = createColorLight();

            // Increase intensity when hovered
            if (isHovered) {
                colorLight.radius = (byte) Math.min(255, colorLight.radius + 4);
            }

            light.setColorLight(colorLight);
        }
    }

    /**
     * Create a ColorLight for saved hex visual.
     */
    private ColorLight createColorLight() {
        int color = SAVED_HEX_COLOR;
        float baseIntensity = 10.0f;

        // Add pulsing effect
        float pulseIntensity = baseIntensity + (float) Math.sin(pulsePhase) * 2.0f;

        // Extract RGB from color int
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        // Create ColorLight
        ColorLight colorLight = new ColorLight();
        colorLight.radius = (byte) Math.min(255, (int) pulseIntensity);
        colorLight.red = (byte) red;
        colorLight.green = (byte) green;
        colorLight.blue = (byte) blue;

        return colorLight;
    }
}
