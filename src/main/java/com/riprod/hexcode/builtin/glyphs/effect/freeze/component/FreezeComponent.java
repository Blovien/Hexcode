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
    private int durationTicks;
    private int elapsedTicks;

    public FreezeComponent() {
    }

    public FreezeComponent(List<FrozenBlock> frozenBlocks, int durationTicks) {
        this.frozenBlocks = frozenBlocks;
        this.durationTicks = durationTicks;
        this.elapsedTicks = 0;
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

    public int getDurationTicks() {
        return durationTicks;
    }

    public int getElapsedTicks() {
        return elapsedTicks;
    }

    public void incrementElapsed() {
        elapsedTicks++;
    }

    public boolean isExpired() {
        return elapsedTicks >= durationTicks;
    }

    @Nonnull
    @Override
    public FreezeComponent clone() {
        FreezeComponent copy = new FreezeComponent();
        copy.frozenBlocks = this.frozenBlocks != null ? new ArrayList<>(this.frozenBlocks) : null;
        copy.durationTicks = this.durationTicks;
        copy.elapsedTicks = this.elapsedTicks;
        return copy;
    }
}
