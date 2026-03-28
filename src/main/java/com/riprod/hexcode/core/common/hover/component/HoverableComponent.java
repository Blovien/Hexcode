package com.riprod.hexcode.core.common.hover.component;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class HoverableComponent implements Component<EntityStore> {
    
    private static ComponentType<EntityStore, HoverableComponent> componentType;

    public HoverableComponent() {
    }

    public HoverableComponent(HoverableType type, Ref<EntityStore> ref) {
        this.entityType = type;
        this.rootRef = ref;
    }

    public HoverableComponent(HoverableType type) {
        this.entityType = type;
    }

    public static void setComponentType(ComponentType<EntityStore, HoverableComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, HoverableComponent> getComponentType() {
        return componentType;
    }

    private HoverableType entityType = HoverableType.CONTAINER;
    private Ref<EntityStore> rootRef = null;
    private Ref<EntityStore> ownerRef = null;
    private Map<String, String> tooltipText = new HashMap<>();

    public HoverableType getType() {
        return entityType;
    }

    public void setType(HoverableType type) {
        this.entityType = type;
    }

    public Ref<EntityStore> getRef() {
        return rootRef;
    }

    public void setRef(Ref<EntityStore> ref) {
        this.rootRef = ref;
    }

    public Ref<EntityStore> getOwnerRef() {
        return ownerRef;
    }

    public void setOwnerRef(Ref<EntityStore> ownerRef) {
        this.ownerRef = ownerRef;
    }

    public String getHintText(String key) {
        return tooltipText.get(key);
    }

    public void setHintText(String key, String hintText) {
        this.tooltipText.put(key, hintText);
    }

    public Map<String, String> getAllHintText() {
        return tooltipText;
    }

    @Nonnull
    @Override
    public HoverableComponent clone() {
        HoverableComponent copy = new HoverableComponent();
        copy.rootRef = this.rootRef;
        copy.entityType = this.entityType;
        copy.ownerRef = this.ownerRef;
        copy.tooltipText = new HashMap<>(this.tooltipText);
        return copy;
    }
}
