package com.riprod.hexcode.core.execution.component;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ExecutionComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, ExecutionComponent> componentType;

    private HexRoot root;
    private HexGraph spellGraph;
    private final List<PendingContinue> pendingContinues = new ArrayList<>();
    private boolean needsInitialExecution;
    private int externalWaiters;

    public ExecutionComponent() {
    }

    public static void setComponentType(ComponentType<EntityStore, ExecutionComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, ExecutionComponent> getComponentType() {
        return componentType;
    }

    public HexRoot getRoot() {
        return root;
    }

    public void setRoot(HexRoot root) {
        this.root = root;
    }

    public HexGraph getSpellGraph() {
        return spellGraph;
    }

    public void setSpellGraph(HexGraph spellGraph) {
        this.spellGraph = spellGraph;
    }

    public List<PendingContinue> getPendingContinues() {
        return pendingContinues;
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
    public ExecutionComponent clone() {
        ExecutionComponent copy = new ExecutionComponent();
        copy.root = this.root;
        copy.spellGraph = this.spellGraph != null ? this.spellGraph.clone() : null;
        return copy;
    }
}
