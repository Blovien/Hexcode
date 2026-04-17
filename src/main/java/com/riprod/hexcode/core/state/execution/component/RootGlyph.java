package com.riprod.hexcode.core.state.execution.component;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexes.component.Hex;

public class RootGlyph implements Component<EntityStore> {

    private static ComponentType<EntityStore, RootGlyph> componentType;

    private HexRoot root;
    private Hex hex;
    private boolean needsInitialExecution;
    private final List<Ref<EntityStore>> dependents = new ArrayList<>();
    private float manaCostMultiplier = 1.0f;
    private float volatilityMultiplier = 1.0f;
    private float powerModifier = 1.0f;
    private HexContext originContext;

    public RootGlyph() {
    }

    public static void setComponentType(ComponentType<EntityStore, RootGlyph> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, RootGlyph> getComponentType() {
        return componentType;
    }

    public HexRoot getRoot() {
        return root;
    }

    public void setRoot(HexRoot root) {
        this.root = root;
    }

    public Hex getHex() {
        return hex;
    }

    public void setHex(Hex hex) {
        this.hex = hex;
    }

    public boolean needsInitialExecution() {
        return needsInitialExecution;
    }

    public void setNeedsInitialExecution(boolean needsInitialExecution) {
        this.needsInitialExecution = needsInitialExecution;
    }

    public List<Ref<EntityStore>> getDependents() {
        return dependents;
    }

    public void addDependent(Ref<EntityStore> ref) {
        if (ref != null) dependents.add(ref);
    }

    public void removeDependent(Ref<EntityStore> ref) {
        if (ref != null) dependents.remove(ref);
    }

    public void pruneDeadDependents() {
        for (int i = dependents.size() - 1; i >= 0; i--) {
            Ref<EntityStore> ref = dependents.get(i);
            if (ref == null || !ref.isValid()) {
                dependents.remove(i);
            }
        }
    }

    public boolean hasDependents() {
        return !dependents.isEmpty();
    }

    public float getManaCostMultiplier() {
        return manaCostMultiplier;
    }

    public void setManaCostMultiplier(float manaCostMultiplier) {
        this.manaCostMultiplier = manaCostMultiplier;
    }

    public float getVolatilityMultiplier() {
        return volatilityMultiplier;
    }

    public void setVolatilityMultiplier(float volatilityMultiplier) {
        this.volatilityMultiplier = volatilityMultiplier;
    }

    public float getPowerModifier() {
        return powerModifier;
    }

    public void setPowerModifier(float powerModifier) {
        this.powerModifier = powerModifier;
    }

    public HexContext getOriginContext() {
        return originContext;
    }

    public void setOriginContext(HexContext originContext) {
        this.originContext = originContext;
    }

    @Nonnull
    @Override
    public RootGlyph clone() {
        RootGlyph copy = new RootGlyph();
        copy.root = this.root;
        copy.hex = this.hex;
        copy.needsInitialExecution = this.needsInitialExecution;
        copy.dependents.addAll(this.dependents);
        copy.manaCostMultiplier = this.manaCostMultiplier;
        copy.volatilityMultiplier = this.volatilityMultiplier;
        copy.powerModifier = this.powerModifier;
        copy.originContext = this.originContext;
        return copy;
    }
}
