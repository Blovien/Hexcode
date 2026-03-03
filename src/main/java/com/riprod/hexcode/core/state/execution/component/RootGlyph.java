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

    @Nonnull
    @Override
    public RootGlyph clone() {
        RootGlyph copy = new RootGlyph();
        copy.root = this.root;
        copy.hex = this.hex;
        copy.pendingContinues.addAll(this.pendingContinues);
        copy.needsInitialExecution = this.needsInitialExecution;
        copy.externalWaiters = this.externalWaiters;
        return copy;
    }
}
