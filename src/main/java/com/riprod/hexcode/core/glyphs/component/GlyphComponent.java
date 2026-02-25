package com.riprod.hexcode.core.glyphs.component;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GlyphComponent implements Component<EntityStore> {

    private enum GlyphFlags {
        Hovering,
        Dragging,
        Hiding,
        Selected
    }

    public static final BuilderCodec<GlyphComponent> CODEC = BuilderCodec
            .builder(GlyphComponent.class, GlyphComponent::new)
            .append(new KeyedCodec<>("Glyph", Glyph.CODEC),
                    (c, v) -> c.glyph = v,
                    c -> c.glyph)
            .add()
            .build();

    private static ComponentType<EntityStore, GlyphComponent> componentType;

    // persistent - from asset
    @Nonnull
    private Glyph glyph;

    // transient
    private Ref<EntityStore> parentRef;
    private Ref<EntityStore> selfRef;
    private Ref<EntityStore> hexRef;
    private Set<GlyphFlags> flags = EnumSet.noneOf(GlyphFlags.class);
    private float scale = 1f;

    // for the codec - do not use
    public GlyphComponent() {
    }

    public GlyphComponent(@Nonnull Glyph glyph) {
        this.glyph = glyph;
    }

    public static void setComponentType(ComponentType<EntityStore, GlyphComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, GlyphComponent> getComponentType() {
        return componentType;
    }

    @Nonnull
    public String getId() {
        return this.glyph.getId();
    }

    /** Getters and Setters */

    @Nonnull
    public String getGlyphId() {
        return this.glyph.getGlyphId();
    }

    @Nullable
    public Ref<EntityStore> getParentRef() {
        return parentRef;
    }

    public void setParentRef(@Nullable Ref<EntityStore> parentRef) {
        this.parentRef = parentRef;
    }

    @Nullable
    public Ref<EntityStore> getSelfRef() {
        return selfRef;
    }

    public void setSelfRef(@Nullable Ref<EntityStore> selfRef) {
        this.selfRef = selfRef;
    }

    @Nullable
    public Ref<EntityStore> getHexRef() {
        return hexRef;
    }

    public void setHexRef(@Nullable Ref<EntityStore> hexRef) {
        this.hexRef = hexRef;
    }

    /** Positioning */

    public float getYaw() {
        return this.glyph.getRotation().getYaw();
    }

    public void setYaw(float yaw) {
        this.glyph.getRotation().setYaw(yaw);
    }

    public float getPitch() {
        return this.glyph.getRotation().getPitch();
    }

    public void setPitch(float pitch) {
        this.glyph.getRotation().setPitch(pitch);
    }

    public float getDistance() {
        return this.glyph.getRotation().getRoll();
    }

    public void setDistance(float distance) {
        this.glyph.getRotation().setRoll(distance);
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public Vector3f getOffset() {
        return this.glyph.getPosition();
    }

    public void setOffset(Vector3f offset) {
        this.glyph.setPosition(offset);
    }

    public void setOffset(float x, float y, float z) {
        this.glyph.setPosition(new Vector3f(x, y, z));
    }

    /** Flags */

    public void setDragState(Boolean isBeingDragged) {
        if (isBeingDragged) {
            this.flags.add(GlyphFlags.Dragging);
        } else {
            this.flags.remove(GlyphFlags.Dragging);
        }
    }

    public boolean isBeingDragged() {
        return this.flags.contains(GlyphFlags.Dragging);
    }

    @Nonnull
    @Override
    public GlyphComponent clone() {
        GlyphComponent copy = new GlyphComponent();
        copy.glyph = this.glyph.clone();
        copy.scale = this.scale;
        copy.parentRef = this.parentRef;
        copy.selfRef = this.selfRef;
        copy.hexRef = this.hexRef;
        copy.flags = this.flags;
        return copy;
    }
}
