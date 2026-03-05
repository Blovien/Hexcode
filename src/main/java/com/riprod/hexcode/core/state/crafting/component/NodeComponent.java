package com.riprod.hexcode.core.state.crafting.component;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class NodeComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, NodeComponent> componentType;

    public NodeComponent() {
    }

    public static void setComponentType(ComponentType<EntityStore, NodeComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, NodeComponent> getComponentType() {
        return componentType;
    }

    private List<Ref<EntityStore>> outputRefs = new ArrayList<>();
    private List<Ref<EntityStore>> inputRefs = new ArrayList<>();
    private Ref<EntityStore> parentGlyphRef;
    private boolean isHovered = false;

    public List<Ref<EntityStore>> getOutputRefs() {
        return outputRefs;
    }

    public void setOutputRefs(List<Ref<EntityStore>> refs) {
        this.outputRefs = refs;
    }

    public void addOutputRef(Ref<EntityStore> ref) {
        this.outputRefs.add(ref);
    }

    public void removeOutputRef(Ref<EntityStore> ref) {
        this.outputRefs.remove(ref);
    }

    public List<Ref<EntityStore>> getInputRefs() {
        return inputRefs;
    }

    public void setInputRefs(List<Ref<EntityStore>> refs) {
        this.inputRefs = refs;
    }

    public void addInputRef(Ref<EntityStore> ref) {
        this.inputRefs.add(ref);
    }

    public void removeInputRef(Ref<EntityStore> ref) {
        this.inputRefs.remove(ref);
    }

    public boolean getIsHovered() {
        return isHovered;
    }

    public void setIsHovered(boolean hoverState) {
        this.isHovered = hoverState;
    }

    public Ref<EntityStore> getParentGlyphRef() {
        return parentGlyphRef;
    }

    public void setParentGlyphRef(Ref<EntityStore> parentGlyphRef) {
        this.parentGlyphRef = parentGlyphRef;
    }

    @Nonnull
    @Override
    public NodeComponent clone() {
        NodeComponent copy = new NodeComponent();
        copy.outputRefs = new ArrayList<>(this.outputRefs);
        copy.inputRefs = new ArrayList<>(this.inputRefs);
        copy.parentGlyphRef = this.parentGlyphRef;
        copy.isHovered = this.isHovered;
        return copy;
    }
}
