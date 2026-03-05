package com.riprod.hexcode.core.common.glyphs.component;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EffectComponent implements Component<EntityStore> {

    private enum GlyphFlags {
        Hovering,
        Dragging,
        Hiding,
        Selected
    }

    public static final BuilderCodec<EffectComponent> CODEC = BuilderCodec
            .builder(EffectComponent.class, EffectComponent::new)
            .append(new KeyedCodec<>("Glyph", Glyph.CODEC),
                    (c, v) -> c.glyph = v,
                    c -> c.glyph)
            .add()
            .build();

    private static ComponentType<EntityStore, EffectComponent> componentType;

    // persistent - from asset
    @Nonnull
    private Glyph glyph = new Glyph(); // default value to avoid null issues during codec construction

    // transient
    private Ref<EntityStore> parentRef;
    private Ref<EntityStore> selfRef;
    private Ref<EntityStore> hexRef;
    private Set<GlyphFlags> flags = EnumSet.noneOf(GlyphFlags.class);
    private float scale = 1f;
    private Ref<EntityStore> nodeRef;

    // for the codec - do not use
    public EffectComponent() {
    }

    public EffectComponent(@Nonnull Glyph glyph) {
        this.glyph = glyph;
    }

    public static void setComponentType(ComponentType<EntityStore, EffectComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, EffectComponent> getComponentType() {
        return componentType;
    }

    @Nonnull
    public Glyph getGlyph() {
        return this.glyph;
    }

    @Nonnull
    public String getId() {
        return this.glyph.getId();
    }

    /** Abstraction from Glyph */

    public List<String> getNext() {
        return this.glyph.getNext();
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

    @Nullable
    public Ref<EntityStore> getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(@Nullable Ref<EntityStore> nodeRef) {
        this.nodeRef = nodeRef;
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
        return this.glyph.getRotation().getZ();
    }

    public void setDistance(float distance) {
        this.glyph.getRotation().setZ(distance);
    }

    public Vector3f getRotation() {
        return this.glyph.getRotation();
    }

    public void setRotation(Vector3f rotation) {
        this.glyph.setRotation(rotation);
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

    public void addNext(String nextId) {
        this.glyph.addNext(nextId);
    }

    /** Flags */

    public void setHoverState(boolean isHovered) {
        if (isHovered) {
            this.flags.add(GlyphFlags.Hovering);
        } else {
            this.flags.remove(GlyphFlags.Hovering);
        }
    }

    public boolean isHovered() {
        return this.flags.contains(GlyphFlags.Hovering);
    }

    public void setDragState(boolean isBeingDragged) {
        if (isBeingDragged) {
            this.flags.add(GlyphFlags.Dragging);
        } else {
            this.flags.remove(GlyphFlags.Dragging);
        }
    }

    public boolean isBeingDragged() {
        return this.flags.contains(GlyphFlags.Dragging);
    }

    public float getVolatility() {
        return this.glyph.getVolatility();
    }

    public float getEfficiency() {
        return this.glyph.getEfficiency();
    }

    @Nonnull
    @Override
    public EffectComponent clone() {
        EffectComponent copy = new EffectComponent();
        copy.glyph = this.glyph.clone();
        copy.scale = this.scale;
        copy.parentRef = this.parentRef;
        copy.selfRef = this.selfRef;
        copy.hexRef = this.hexRef;
        copy.nodeRef = this.nodeRef;
        copy.flags = this.flags.isEmpty() ? EnumSet.noneOf(GlyphFlags.class) : EnumSet.copyOf(this.flags);
        return copy;
    }
}
