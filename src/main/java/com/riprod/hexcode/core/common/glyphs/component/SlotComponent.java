package com.riprod.hexcode.core.common.glyphs.component;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.utils.GlyphSlotType;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

public class SlotComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, SlotComponent> componentType;

    public static ComponentType<EntityStore, SlotComponent> getComponentType() {
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, SlotComponent> type) {
        componentType = type;
    }

    private GlyphSlotType type;
    private int index;
    private Vector3f offset;
    private Ref<EntityStore> parentRef;
    private Boolean isHovered;
    private Ref<EntityStore> self;

    public SlotComponent() {
    }

    public SlotComponent(int index, GlyphSlotType type, Vector3f offset, Ref<EntityStore> parentRef) {
        this.index = index;
        this.type = type;
        this.offset = offset;
        this.parentRef = parentRef;
    }

    public List<Vector3f> getBounds(Vector3f parentPosition) {
        List<Vector3f> bounds = new ArrayList<>();

        // Assume that the slot is a 0.25x0.25 square
        Vector3f min = new Vector3f(parentPosition.x + offset.x - 0.125f, parentPosition.y + offset.y - 0.125f,
                parentPosition.z + offset.z - 0.125f);
        Vector3f max = new Vector3f(parentPosition.x + offset.x + 0.125f, parentPosition.y + offset.y + 0.125f,
                parentPosition.z + offset.z + 0.125f);
        bounds.add(min);
        bounds.add(max);

        return bounds;
    }

    // getters and setters
    public GlyphSlotType getType() {
        return type;
    }

    public void setType(GlyphSlotType type) {
        this.type = type;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Vector3f getOffset() {
        return offset;
    }

    public void setOffset(Vector3f offset) {
        this.offset = offset;
    }

    public Ref<EntityStore> getParentRef() {
        return parentRef;
    }

    public void setParentRef(Ref<EntityStore> parentRef) {
        this.parentRef = parentRef;
    }

    public Boolean getIsHovered() {
        return isHovered;
    }

    public void setIsHovered(Boolean isHovered) {
        this.isHovered = isHovered;
    }

    public Ref<EntityStore> getSelf() {
        return self;
    }

    public void setSelf(Ref<EntityStore> self) {
        this.self = self;
    }

    @Nonnull
    @Override
    public SlotComponent clone() {
        SlotComponent copy = new SlotComponent();
        copy.type = this.type;
        copy.index = this.index;
        copy.offset = this.offset;
        copy.parentRef = this.parentRef;
        copy.isHovered = this.isHovered;
        copy.self = this.self;
        return copy;
    }
}
