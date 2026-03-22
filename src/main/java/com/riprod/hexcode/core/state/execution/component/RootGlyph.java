package com.riprod.hexcode.core.state.execution.component;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexes.component.Hex;

public class RootGlyph implements Component<EntityStore> {

    private static ComponentType<EntityStore, RootGlyph> componentType;

    private HexRoot root;
    private Hex hex;
    private final List<PendingContinue> pendingContinues = new ArrayList<>();
    private boolean needsInitialExecution;
    private int externalWaiters;
    private float powerModifier;
    private float manaCostMultiplier = 1.0f;
    private float volatilityMultiplier = 1.0f;

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

    public List<PendingContinue> getPendingContinues() {
        return pendingContinues;
    }

    public Hex getHex() {
        return hex;
    }

    public void setHex(Hex hex) {
        this.hex = hex;
    }

    public void addPendingContinue(PendingContinue pending) {
        pendingContinues.add(pending);
    }

    public boolean hasPendingContinues() {
        return !pendingContinues.isEmpty();
    }

    public boolean needsInitialExecution() {
        return needsInitialExecution;
    }

    public void setNeedsInitialExecution(boolean needsInitialExecution) {
        this.needsInitialExecution = needsInitialExecution;
    }

    public int getExternalWaiters() {
        return externalWaiters;
    }

    public void incrementExternalWaiters() {
        externalWaiters++;
    }

    public void decrementExternalWaiters() {
        externalWaiters--;
    }

    public float getPowerModifier() {
        return powerModifier;
    }

    public void setPowerModifier(float powerModifier) {
        this.powerModifier = powerModifier;
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

    @Nonnull
    @Override
    public RootGlyph clone() {
        RootGlyph copy = new RootGlyph();
        copy.root = this.root;
        copy.hex = this.hex;
        copy.pendingContinues.addAll(this.pendingContinues);
        copy.needsInitialExecution = this.needsInitialExecution;
        copy.externalWaiters = this.externalWaiters;
        copy.powerModifier = this.powerModifier;
        copy.manaCostMultiplier = this.manaCostMultiplier;
        copy.volatilityMultiplier = this.volatilityMultiplier;
        return copy;
    }
}
