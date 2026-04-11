package com.riprod.hexcode.core.common.trigger.component;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class TriggerComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, TriggerComponent> componentType;

    private String handlerId;
    private float remainingLifetime;
    private float initialLifetime;
    private String[] firstBranchIds;
    private boolean firedFirstBranch;
    private boolean killRequested;

    public TriggerComponent() {
    }

    public TriggerComponent(@Nonnull String handlerId, float lifetime,
            @Nullable String[] firstBranchIds) {
        this.handlerId = handlerId;
        this.remainingLifetime = lifetime;
        this.initialLifetime = lifetime;
        this.firstBranchIds = firstBranchIds;
        this.firedFirstBranch = false;
        this.killRequested = false;
    }

    public static void setComponentType(ComponentType<EntityStore, TriggerComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, TriggerComponent> getComponentType() {
        return componentType;
    }

    public String getHandlerId() {
        return handlerId;
    }

    public float getRemainingLifetime() {
        return remainingLifetime;
    }

    public void setRemainingLifetime(float remainingLifetime) {
        this.remainingLifetime = remainingLifetime;
    }

    public float getInitialLifetime() {
        return initialLifetime;
    }

    @Nullable
    public String[] getFirstBranchIds() {
        return firstBranchIds;
    }

    public boolean firedFirstBranch() {
        return firedFirstBranch;
    }

    public void markFirstBranchFired() {
        this.firedFirstBranch = true;
    }

    public boolean isKillRequested() {
        return killRequested;
    }

    public void setKillRequested(boolean killRequested) {
        this.killRequested = killRequested;
    }

    @Nonnull
    @Override
    public TriggerComponent clone() {
        TriggerComponent copy = new TriggerComponent();
        copy.handlerId = this.handlerId;
        copy.remainingLifetime = this.remainingLifetime;
        copy.initialLifetime = this.initialLifetime;
        copy.firstBranchIds = this.firstBranchIds != null ? this.firstBranchIds.clone() : null;
        copy.firedFirstBranch = this.firedFirstBranch;
        copy.killRequested = this.killRequested;
        return copy;
    }
}
