package com.riprod.hexcode.builtin.glyphs.phase;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class PhaseComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, PhaseComponent> componentType;

    private List<PhasedBlock> phasedBlocks;
    private List<String> next;

    public PhaseComponent() {
    }

    public PhaseComponent(List<PhasedBlock> phasedBlocks, List<String> next) {
        this.phasedBlocks = phasedBlocks;
        this.next = next;
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

    public List<String> getNext() {
        return next;
    }

    @Nonnull
    @Override
    public PhaseComponent clone() {
        PhaseComponent copy = new PhaseComponent();
        copy.phasedBlocks = this.phasedBlocks != null ? new ArrayList<>(this.phasedBlocks) : null;
        return copy;
    }
}
