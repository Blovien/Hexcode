package com.riprod.hexcode.core.common.construct.component;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class HexConstruct implements Component<EntityStore> {

    private static ComponentType<EntityStore, HexConstruct> componentType;

    @Nullable
    private String handlerId;
    private float remainingLifetime;
    private float initialLifetime;
    private float manaDrainPerSecond;
    private boolean killRequested;

    @Nonnull
    private List<String> immediateBranchIds;
    @Nonnull
    private List<String> conditionalBranchIds;
    @Nonnull
    private List<String> cleanupBranchIds;
    private boolean firedImmediate;

    @Nonnull
    private HexContext hexContext;
    @Nullable
    private Glyph triggeringGlyph;
    @Nullable
    private Ref<EntityStore> rootEntityRef;

    public HexConstruct() {
        this.immediateBranchIds = new ArrayList<>();
        this.conditionalBranchIds = new ArrayList<>();
        this.cleanupBranchIds = new ArrayList<>();
        this.hexContext = new HexContext();
    }

    public HexConstruct(@Nullable String handlerId, float lifetime, float manaDrainPerSecond,
            @Nullable List<String> immediateBranchIds,
            @Nullable List<String> conditionalBranchIds,
            @Nullable List<String> cleanupBranchIds,
            @Nonnull HexContext hexContext,
            @Nullable Glyph triggeringGlyph,
            @Nullable Ref<EntityStore> rootEntityRef) {
        this.handlerId = handlerId;
        this.remainingLifetime = lifetime;
        this.initialLifetime = lifetime;
        this.manaDrainPerSecond = manaDrainPerSecond;
        this.killRequested = false;
        this.immediateBranchIds = immediateBranchIds != null ? new ArrayList<>(immediateBranchIds) : new ArrayList<>();
        this.conditionalBranchIds = conditionalBranchIds != null ? new ArrayList<>(conditionalBranchIds) : new ArrayList<>();
        this.cleanupBranchIds = cleanupBranchIds != null ? new ArrayList<>(cleanupBranchIds) : new ArrayList<>();
        this.firedImmediate = false;
        this.hexContext = hexContext;
        this.triggeringGlyph = triggeringGlyph;
        this.rootEntityRef = rootEntityRef;
    }

    public static void setComponentType(ComponentType<EntityStore, HexConstruct> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, HexConstruct> getComponentType() {
        return componentType;
    }

    @Nullable
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

    public float getManaDrainPerSecond() {
        return manaDrainPerSecond;
    }

    public void setManaDrainPerSecond(float manaDrainPerSecond) {
        this.manaDrainPerSecond = manaDrainPerSecond;
    }

    public boolean isKillRequested() {
        return killRequested;
    }

    public void requestKill() {
        this.killRequested = true;
    }

    @Nonnull
    public List<String> getImmediateBranchIds() {
        return immediateBranchIds;
    }

    @Nonnull
    public List<String> getConditionalBranchIds() {
        return conditionalBranchIds;
    }

    public void setConditionalBranchIds(@Nonnull List<String> conditionalBranchIds) {
        this.conditionalBranchIds = conditionalBranchIds;
    }

    @Nonnull
    public List<String> getCleanupBranchIds() {
        return cleanupBranchIds;
    }

    public boolean hasFiredImmediate() {
        return firedImmediate;
    }

    public void markImmediateFired() {
        this.firedImmediate = true;
    }

    @Nonnull
    public HexContext getHexContext() {
        return hexContext;
    }

    @Nullable
    public Glyph getTriggeringGlyph() {
        return triggeringGlyph;
    }

    @Nullable
    public Ref<EntityStore> getRootEntityRef() {
        return rootEntityRef;
    }

    @Nonnull
    @Override
    public HexConstruct clone() {
        HexConstruct copy = new HexConstruct();
        copy.handlerId = this.handlerId;
        copy.remainingLifetime = this.remainingLifetime;
        copy.initialLifetime = this.initialLifetime;
        copy.manaDrainPerSecond = this.manaDrainPerSecond;
        copy.killRequested = this.killRequested;
        copy.immediateBranchIds = new ArrayList<>(this.immediateBranchIds);
        copy.conditionalBranchIds = new ArrayList<>(this.conditionalBranchIds);
        copy.cleanupBranchIds = new ArrayList<>(this.cleanupBranchIds);
        copy.firedImmediate = this.firedImmediate;
        copy.hexContext = this.hexContext.copy();
        copy.triggeringGlyph = this.triggeringGlyph;
        copy.rootEntityRef = this.rootEntityRef;
        return copy;
    }
}
