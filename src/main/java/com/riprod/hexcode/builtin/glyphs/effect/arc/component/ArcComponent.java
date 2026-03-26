package com.riprod.hexcode.builtin.glyphs.effect.arc.component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;

public class ArcComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, ArcComponent> componentType;

    public static ComponentType<EntityStore, ArcComponent> getComponentType() {
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, ArcComponent> type) {
        componentType = type;
    }

    private Glyph arcGlyph;
    private List<String> branches;
    private Set<UUID> visitedEntities;
    private float maxJumpDistance;
    private float delay;
    private float tickTimer = 0f;
    private int branchIndex = 0;
    private boolean hasFired = false;

    public ArcComponent() {
    }

    public ArcComponent(Glyph arcGlyph, List<String> branches, Set<UUID> visitedEntities,
            float maxJumpDistance, float delay) {
        this.arcGlyph = arcGlyph;
        this.branches = branches;
        this.visitedEntities = visitedEntities;
        this.maxJumpDistance = maxJumpDistance;
        this.delay = delay;
    }

    public Glyph getArcGlyph() {
        return arcGlyph;
    }

    public List<String> getBranches() {
        return branches;
    }

    public String getCurrentBranch() {
        if (branchIndex >= branches.size()) return null;
        return branches.get(branchIndex);
    }

    public boolean hasMoreBranches() {
        return branchIndex < branches.size();
    }

    public Set<UUID> getVisitedEntities() {
        return visitedEntities;
    }

    public float getMaxJumpDistance() {
        return maxJumpDistance;
    }

    public float getDelay() {
        return delay;
    }

    public float incrementTimer(float dt) {
        tickTimer += dt;
        return tickTimer;
    }

    public void resetTimer() {
        tickTimer = 0f;
    }

    public int getBranchIndex() {
        return branchIndex;
    }

    public void advanceBranch() {
        branchIndex++;
    }

    public boolean hasFired() {
        return hasFired;
    }

    public void setHasFired(boolean hasFired) {
        this.hasFired = hasFired;
    }

    public ArcComponent createNextHop() {
        ArcComponent next = new ArcComponent(
                arcGlyph, branches, visitedEntities, maxJumpDistance, delay);
        next.branchIndex = this.branchIndex;
        return next;
    }

    @Nonnull
    @Override
    public ArcComponent clone() {
        ArcComponent copy = new ArcComponent();
        copy.arcGlyph = this.arcGlyph;
        copy.branches = this.branches;
        copy.visitedEntities = this.visitedEntities;
        copy.maxJumpDistance = this.maxJumpDistance;
        copy.delay = this.delay;
        copy.tickTimer = this.tickTimer;
        copy.branchIndex = this.branchIndex;
        copy.hasFired = this.hasFired;
        return copy;
    }
}
