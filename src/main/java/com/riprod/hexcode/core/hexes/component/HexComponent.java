package com.riprod.hexcode.core.hexes.component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class HexComponent implements Component<EntityStore> {

    private enum HexFlags {
        Hovering,
        Dragging,
        Hiding,
        Selected
    }

    public static final BuilderCodec<HexComponent> CODEC = BuilderCodec
            .builder(HexComponent.class, HexComponent::new)
            .append(new KeyedCodec<>("Hex", Hex.CODEC),
                    (c, v) -> c.hex = v,
                    c -> c.hex)
            .add()
            .build();

    private static ComponentType<EntityStore, HexComponent> componentType;

    // persistent - from asset
    @Nonnull
    private Hex hex;

    // transient
    private Vector3f offset = new Vector3f(); // offset from parent
    private Vector3f rotation = new Vector3f(); // x = yaw, y = pitch, z = distance
    private float scale = 1f;
    private List<Ref<EntityStore>> childHexRefs = new ArrayList<>();
    private Ref<EntityStore> parentRef;
    private Ref<EntityStore> selfRef;
    private Ref<EntityStore> rootRef;
    private Set<HexFlags> flags = EnumSet.noneOf(HexFlags.class);

    // for the codec - do not use
    public HexComponent() {
    }

    public HexComponent(@Nonnull Hex hex) {
        this.hex = hex;
    }

    public static void setComponentType(ComponentType<EntityStore, HexComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, HexComponent> getComponentType() {
        return componentType;
    }

    @Nullable
    public Ref<EntityStore> getParentRef() {
        return parentRef;
    }

    public void setParentRef(@Nullable Ref<EntityStore> parentRef) {
        this.parentRef = parentRef;
    }

    @Nullable
    public Ref<EntityStore> getRootRef() {
        return rootRef;
    }

    public void setRootRef(@Nullable Ref<EntityStore> rootRef) {
        this.rootRef = rootRef;
    }

    @Nullable
    public Ref<EntityStore> getSelfRef() {
        return selfRef;
    }

    public void setSelfRef(@Nullable Ref<EntityStore> selfRef) {
        this.selfRef = selfRef;
    }

    public Hex getHex() {
        return hex;
    }

    public String getId() {
        return this.hex.get();
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public List<Ref<EntityStore>> getChildHexRefs() {
        return childHexRefs;
    }

    public void addChildHexRef(Ref<EntityStore> childRef) {
        this.childHexRefs.add(childRef);
    }

    public void setChildHexRefs(List<Ref<EntityStore>> childHexRefs) {
        this.childHexRefs = childHexRefs;
    }

    public void removeChildHexRef(Ref<EntityStore> childRef) {
        this.childHexRefs.remove(childRef);
    }

    /** Positioning */

    public float getYaw() {
        return this.rotation.getYaw();
    }

    public void setYaw(float yaw) {
        this.rotation.setYaw(yaw);
    }

    public float getPitch() {
        return this.rotation.getPitch();
    }

    public void setPitch(float pitch) {
        this.rotation.setPitch(pitch);
    }

    public float getDistance() {
        return this.rotation.getRoll();
    }

    public void setDistance(float distance) {
        this.rotation.setRoll(distance);
    }

    public void setRotation(Vector3f rotation) {
        this.rotation = rotation;
    }

    public Vector3f getOffset() {
        return offset;
    }

    public void setOffset(Vector3f offset) {
        this.offset = offset;
    }

    public void setOffset(float x, float y, float z) {
        this.offset = new Vector3f(x, y, z);
    }

    /** Flags */

    public void setDragState(Boolean isBeingDragged) {
        if (isBeingDragged) {
            this.flags.add(HexFlags.Dragging);
        } else {
            this.flags.remove(HexFlags.Dragging);
        }
    }

    public boolean isBeingDragged() {
        return this.flags.contains(HexFlags.Dragging);
    }

    @Nonnull
    @Override
    public HexComponent clone() {
        HexComponent copy = new HexComponent();
        copy.hex = this.hex.clone();
        copy.offset = new Vector3f(this.offset.getX(), this.offset.getY(), this.offset.getZ());
        copy.rotation = new Vector3f(this.rotation.getX(), this.rotation.getY(), this.rotation.getZ());
        copy.parentRef = this.parentRef;
        copy.selfRef = this.selfRef;
        copy.flags = EnumSet.copyOf(this.flags);
        return copy;
    }
}
