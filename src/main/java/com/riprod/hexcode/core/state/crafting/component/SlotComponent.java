package com.riprod.hexcode.core.state.crafting.component;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.utils.GlyphSlotType;
import com.riprod.hexcode.core.state.crafting.constants.NodeType;

public class SlotComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, SlotComponent> componentType;

    public SlotComponent() {
    }

    public SlotComponent(int slotIndex, GlyphSlotType slotType) {
        this.slotIndex = slotIndex;
        this.slotType = slotType;
    }

    public static void setComponentType(ComponentType<EntityStore, SlotComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, SlotComponent> getComponentType() {
        return componentType;
    }

    private int slotIndex;
    private GlyphSlotType slotType;
    private boolean isHovered = false;
    private NodeType nodeType;

    public boolean getIsHovered() {
        return isHovered;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public GlyphSlotType getSlotType() {
        return slotType;
    }

    public void setIsHovered(boolean hoverState) {
        this.isHovered = hoverState;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    @Nonnull
    @Override
    public SlotComponent clone() {
        SlotComponent copy = new SlotComponent();
        copy.isHovered = this.isHovered;
        copy.nodeType = this.nodeType;
        return copy;
    }
}
