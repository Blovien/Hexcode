package com.riprod.hexcode.builtin.glyphs.effect.conjure.component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ConjureZoneComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, ConjureZoneComponent> componentType;

    public static void setComponentType(ComponentType<EntityStore, ConjureZoneComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, ConjureZoneComponent> getComponentType() {
        return componentType;
    }

    private Vector3d halfExtents;
    private float interval;
    private float intervalTimer;
    private float remainingLifetime;
    private Set<Ref<EntityStore>> lastOccupants;
    private Set<Ref<EntityStore>> newOccupants;
    private Ref<EntityStore> zoneRef;
    private List<String> firstBranchIds;
    private boolean firedFirstBranch;

    public ConjureZoneComponent() {
    }

    public ConjureZoneComponent(Vector3d halfExtents, float interval, float remainingLifetime,
            List<String> firstBranchIds) {
        this.halfExtents = halfExtents;
        this.interval = interval;
        this.intervalTimer = interval;
        this.remainingLifetime = remainingLifetime;
        this.lastOccupants = new HashSet<>();
        this.newOccupants = new HashSet<>();
        this.firstBranchIds = firstBranchIds;
        this.firedFirstBranch = false;
    }

    public Vector3d getHalfExtents() {
        return halfExtents;
    }

    public float getInterval() {
        return interval;
    }

    public float getIntervalTimer() {
        return intervalTimer;
    }

    public void setIntervalTimer(float intervalTimer) {
        this.intervalTimer = intervalTimer;
    }

    public float getRemainingLifetime() {
        return remainingLifetime;
    }

    public void setRemainingLifetime(float remainingLifetime) {
        this.remainingLifetime = remainingLifetime;
    }

    public Set<Ref<EntityStore>> getLastOccupants() {
        return lastOccupants;
    }

    public void setLastOccupants(Set<Ref<EntityStore>> lastOccupants) {
        this.lastOccupants = lastOccupants;
    }

    public Set<Ref<EntityStore>> getNewOccupants() {
        return newOccupants;
    }

    public void setNewOccupants(Set<Ref<EntityStore>> newOccupants) {
        this.newOccupants = newOccupants;
    }

    public Ref<EntityStore> getZoneRef() {
        return zoneRef;
    }

    public void setZoneRef(Ref<EntityStore> zoneRef) {
        this.zoneRef = zoneRef;
    }

    public List<String> getFirstBranchIds() {
        return firstBranchIds;
    }

    public boolean firedFirstBranch() {
        return firedFirstBranch;
    }

    public void markFirstBranchFired() {
        this.firedFirstBranch = true;
    }

    @Nonnull
    @Override
    public ConjureZoneComponent clone() {
        ConjureZoneComponent copy = new ConjureZoneComponent();
        copy.halfExtents = this.halfExtents != null ? new Vector3d(this.halfExtents) : null;
        copy.interval = this.interval;
        copy.intervalTimer = this.intervalTimer;
        copy.remainingLifetime = this.remainingLifetime;
        copy.lastOccupants = new HashSet<>();
        copy.newOccupants = new HashSet<>();
        copy.zoneRef = this.zoneRef;
        copy.firstBranchIds = this.firstBranchIds;
        copy.firedFirstBranch = this.firedFirstBranch;
        return copy;
    }
}
