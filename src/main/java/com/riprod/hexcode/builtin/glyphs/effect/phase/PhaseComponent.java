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
    private float durationSeconds;
    private float elapsedDelta;

    public PhaseComponent() {
    }

    public PhaseComponent(List<PhasedBlock> phasedBlocks, float durationSeconds) {
        this.phasedBlocks = phasedBlocks;
        this.durationSeconds = durationSeconds;
        this.elapsedDelta = 0;
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

    public float getDurationSeconds() {
        return durationSeconds;
    }

    public float getElapsedDelta() {
        return elapsedDelta;
    }

    public void incrementElapsed(float dt) {
        elapsedDelta += dt;
    }

    public boolean isExpired() {
        return elapsedDelta >= durationSeconds;
    }

    @Nonnull
    @Override
    public PhaseComponent clone() {
        PhaseComponent copy = new PhaseComponent();
        copy.phasedBlocks = this.phasedBlocks != null ? new ArrayList<>(this.phasedBlocks) : null;
        copy.durationSeconds = this.durationSeconds;
        copy.elapsedDelta = this.elapsedDelta;
        return copy;
    }
}
