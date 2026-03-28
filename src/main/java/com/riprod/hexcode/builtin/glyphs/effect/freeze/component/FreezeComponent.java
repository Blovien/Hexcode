package com.riprod.hexcode.builtin.glyphs.effect.freeze.component;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class FreezeComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, FreezeComponent> componentType;

    private List<FrozenBlock> frozenBlocks;
    private float durationSeconds;
    private float elapsedSeconds;

    public FreezeComponent() {
    }

    public FreezeComponent(List<FrozenBlock> frozenBlocks, float durationSeconds) {
        this.frozenBlocks = frozenBlocks;
        this.durationSeconds = durationSeconds;
        this.elapsedSeconds = 0;
    }

    public static void setComponentType(ComponentType<EntityStore, FreezeComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, FreezeComponent> getComponentType() {
        return componentType;
    }

    public List<FrozenBlock> getFrozenBlocks() {
        return frozenBlocks;
    }

    public float getDurationSeconds() {
        return durationSeconds;
    }

    public float getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void incrementElapsed(float dt) {
        elapsedSeconds += dt;
    }

    public boolean isExpired() {
        return elapsedSeconds >= durationSeconds;
    }

    @Nonnull
    @Override
    public FreezeComponent clone() {
        FreezeComponent copy = new FreezeComponent();
        copy.frozenBlocks = this.frozenBlocks != null ? new ArrayList<>(this.frozenBlocks) : null;
        copy.durationSeconds = this.durationSeconds;
        copy.elapsedSeconds = this.elapsedSeconds;
        return copy;
    }
}
