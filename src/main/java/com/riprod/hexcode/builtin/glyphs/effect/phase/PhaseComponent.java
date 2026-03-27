package com.riprod.hexcode.builtin.glyphs.effect.phase;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PhaseComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, PhaseComponent> componentType;

    private List<PhasedBlock> phasedBlocks;
    private int durationTicks;
    private int elapsedTicks;

    public PhaseComponent() {
    }

    public PhaseComponent(List<PhasedBlock> phasedBlocks, int durationTicks) {
        this.phasedBlocks = phasedBlocks;
        this.durationTicks = durationTicks;
        this.elapsedTicks = 0;
    }

    public static void setComponentType(ComponentType<EntityStore, PhaseComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, PhaseComponent> getComponentType() {
        return componentType;
    }

    public List<PhasedBlock> getPhasedBlocks() {
        return phasedBlocks;
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
    public PhaseComponent clone() {
        PhaseComponent copy = new PhaseComponent();
        copy.phasedBlocks = this.phasedBlocks != null ? new ArrayList<>(this.phasedBlocks) : null;
        copy.durationTicks = this.durationTicks;
        copy.elapsedTicks = this.elapsedTicks;
        return copy;
    }
}
