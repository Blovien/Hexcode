package com.riprod.hexcode.mode;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

/**
 * Component attached to orbital glyph entities.
 *
 * Stores orbital parameters for glyph entities in the orbital ring.
 */
public class GlyphModeComponent implements Component<EntityStore> {
    private static ComponentType<EntityStore, GlyphModeComponent> componentType;

    /**
     * CODEC for serialization - required for ECS component registration.
     */
    public static final BuilderCodec<GlyphModeComponent> CODEC =
        BuilderCodec.builder(GlyphModeComponent.class, GlyphModeComponent::new)
            .append(
                new KeyedCodec<>("GlyphId", Codec.STRING),
                (c, v) -> c.glyphId = v,
                c -> c.glyphId
            )
            .add()
            .append(
                new KeyedCodec<>("OwnerPlayerId", Codec.UUID_STRING),
                (c, v) -> c.ownerPlayerId = v,
                c -> c.ownerPlayerId
            )
            .add()
            .append(
                new KeyedCodec<>("OrbitAngle", Codec.FLOAT),
                (c, v) -> c.orbitAngle = v,
                c -> c.orbitAngle
            )
            .add()
            .append(
                new KeyedCodec<>("OrbitSpeed", Codec.FLOAT),
                (c, v) -> c.orbitSpeed = v,
                c -> c.orbitSpeed
            )
            .add()
            .append(
                new KeyedCodec<>("OrbitalRadius", Codec.FLOAT),
                (c, v) -> c.orbitalRadius = v,
                c -> c.orbitalRadius
            )
            .add()
            .append(
                new KeyedCodec<>("Height", Codec.FLOAT),
                (c, v) -> c.height = v,
                c -> c.height
            )
            .add()
            .append(
                new KeyedCodec<>("IsHovered", Codec.BOOLEAN),
                (c, v) -> c.isHovered = v,
                c -> c.isHovered
            )
            .add()
            .append(
                new KeyedCodec<>("IsDragging", Codec.BOOLEAN),
                (c, v) -> c.isDragging = v,
                c -> c.isDragging
            )
            .add()
            .build();

    private String glyphId;
    private UUID ownerPlayerId;
    private float orbitAngle;
    private float orbitSpeed;
    private float orbitalRadius;
    private float height;
    private boolean isHovered;
    private boolean isDragging;

    /**
     * Default constructor required for ECS component registration.
     */
    public GlyphModeComponent() {
        this.glyphId = "";
        this.ownerPlayerId = null;
        this.orbitAngle = 0;
        this.orbitSpeed = 0;
        this.orbitalRadius = 0;
        this.height = 0;
        this.isHovered = false;
        this.isDragging = false;
    }

    public GlyphModeComponent(String glyphId, UUID ownerPlayerId, float initialAngle,
                                  float orbitSpeed, float orbitalRadius, float height) {
        this.glyphId = glyphId;
        this.ownerPlayerId = ownerPlayerId;
        this.orbitAngle = initialAngle;
        this.orbitSpeed = orbitSpeed;
        this.orbitalRadius = orbitalRadius;
        this.height = height;
        this.isHovered = false;
        this.isDragging = false;
    }

    /**
     * Initialize an instance with all parameters (for use after default construction).
     */
    public void initialize(String glyphId, UUID ownerPlayerId, float initialAngle,
                           float orbitSpeed, float orbitalRadius, float height) {
        this.glyphId = glyphId;
        this.ownerPlayerId = ownerPlayerId;
        this.orbitAngle = initialAngle;
        this.orbitSpeed = orbitSpeed;
        this.orbitalRadius = orbitalRadius;
        this.height = height;
    }

    public String getGlyphId() {
        return glyphId;
    }

    public UUID getOwnerPlayerId() {
        return ownerPlayerId;
    }

    public float getOrbitAngle() {
        return orbitAngle;
    }

    public void setOrbitAngle(float angle) {
        this.orbitAngle = angle;
    }

    public float getOrbitSpeed() {
        return orbitSpeed;
    }

    public void setOrbitSpeed(float speed) {
        this.orbitSpeed = speed;
    }

    public float getOrbitalRadius() {
        return orbitalRadius;
    }

    public void setOrbitalRadius(float radius) {
        this.orbitalRadius = radius;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public boolean isHovered() {
        return isHovered;
    }

    public void setHovered(boolean hovered) {
        this.isHovered = hovered;
    }

    public boolean isDragging() {
        return isDragging;
    }

    public void setDragging(boolean dragging) {
        this.isDragging = dragging;
    }

    /**
     * Update the orbital angle based on delta time.
     *
     * @param dt Delta time in seconds
     */
    public void updateOrbit(float dt) {
        if (!isDragging) {
            orbitAngle += orbitSpeed * dt;
            if (orbitAngle > Math.PI * 2) {
                orbitAngle -= Math.PI * 2;
            }
        }
    }

    /**
     * Get the component type for registration.
     */
    public static ComponentType<EntityStore, GlyphModeComponent> getComponentType() {
        return componentType;
    }

    /**
     * Set the component type (called during plugin initialization).
     */
    public static void setComponentType(ComponentType<EntityStore, GlyphModeComponent> type) {
        componentType = type;
    }

    /**
     * Clone this component (required by Component interface).
     */
    @Override
    public Component<EntityStore> clone() {
        GlyphModeComponent cloned = new GlyphModeComponent(
            glyphId, ownerPlayerId, orbitAngle, orbitSpeed, orbitalRadius, height
        );
        cloned.isHovered = this.isHovered;
        cloned.isDragging = this.isDragging;
        return cloned;
    }
}
