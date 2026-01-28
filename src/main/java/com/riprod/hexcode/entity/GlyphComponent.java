package com.riprod.hexcode.entity;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.math.GlyphRotation;

import java.util.UUID;

/**
 * Component attached to glyph/hex entities.
 *
 * <p>Stores rotation (pitch/yaw) for positioning elements relative to the player's eyes.
 */
public class GlyphComponent implements Component<EntityStore> {
    private static ComponentType<EntityStore, GlyphComponent> componentType;

    /**
     * CODEC for serialization - required for ECS component registration.
     */
    public static final BuilderCodec<GlyphComponent> CODEC =
        BuilderCodec.builder(GlyphComponent.class, GlyphComponent::new)
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
                new KeyedCodec<>("Pitch", Codec.FLOAT),
                (c, v) -> c.pitch = v,
                c -> c.pitch
            )
            .add()
            .append(
                new KeyedCodec<>("Yaw", Codec.FLOAT),
                (c, v) -> c.yaw = v,
                c -> c.yaw
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
    private float pitch;
    private float yaw;
    private boolean isHovered;
    private boolean isDragging;

    /**
     * Default constructor required for ECS component registration.
     */
    public GlyphComponent() {
        this.glyphId = "";
        this.ownerPlayerId = null;
        this.pitch = 0;
        this.yaw = 0;
        this.isHovered = false;
        this.isDragging = false;
    }

    /**
     * Create a new GlyphComponent with rotation.
     *
     * @param glyphId       The glyph/hex ID
     * @param ownerPlayerId The owner player's UUID
     * @param pitch         Vertical angle in degrees (negative = up)
     * @param yaw           Horizontal angle in degrees (0 = forward)
     */
    public GlyphComponent(String glyphId, UUID ownerPlayerId, float pitch, float yaw) {
        this.glyphId = glyphId;
        this.ownerPlayerId = ownerPlayerId;
        this.pitch = pitch;
        this.yaw = yaw;
        this.isHovered = false;
        this.isDragging = false;
    }

    /**
     * Create a GlyphComponent from a GlyphRotation.
     *
     * @param glyphId       The glyph/hex ID
     * @param ownerPlayerId The owner player's UUID
     * @param rotation      The rotation to use
     */
    public GlyphComponent(String glyphId, UUID ownerPlayerId, GlyphRotation rotation) {
        this.glyphId = glyphId;
        this.ownerPlayerId = ownerPlayerId;
        this.pitch = rotation.getPitch();
        this.yaw = rotation.getYaw();
        this.isHovered = false;
        this.isDragging = false;
    }

    /**
     * Initialize an instance with rotation (for use after default construction).
     */
    public void initialize(String glyphId, UUID ownerPlayerId, float pitch, float yaw) {
        this.glyphId = glyphId;
        this.ownerPlayerId = ownerPlayerId;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public String getGlyphId() {
        return glyphId;
    }

    public UUID getOwnerPlayerId() {
        return ownerPlayerId;
    }

    /**
     * @return Vertical angle in degrees. Negative = up, positive = down.
     */
    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    /**
     * @return Horizontal angle in degrees. 0 = forward (+Z), 90 = right (+X).
     */
    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    /**
     * Get the rotation as a GlyphRotation object.
     *
     * @return A GlyphRotation with this component's pitch and yaw
     */
    public GlyphRotation getRotation() {
        return new GlyphRotation(pitch, yaw);
    }

    /**
     * Set the rotation from a GlyphRotation object.
     *
     * @param rotation The rotation to set
     */
    public void setRotation(GlyphRotation rotation) {
        this.pitch = rotation.getPitch();
        this.yaw = rotation.getYaw();
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
     * Get the component type for registration.
     */
    public static ComponentType<EntityStore, GlyphComponent> getComponentType() {
        return componentType;
    }

    /**
     * Set the component type (called during plugin initialization).
     */
    public static void setComponentType(ComponentType<EntityStore, GlyphComponent> type) {
        componentType = type;
    }

    /**
     * Clone this component (required by Component interface).
     */
    @Override
    public Component<EntityStore> clone() {
        GlyphComponent cloned = new GlyphComponent(glyphId, ownerPlayerId, pitch, yaw);
        cloned.isHovered = this.isHovered;
        cloned.isDragging = this.isDragging;
        return cloned;
    }
}
