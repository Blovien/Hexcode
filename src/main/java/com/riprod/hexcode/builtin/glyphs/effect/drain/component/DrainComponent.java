package com.riprod.hexcode.builtin.glyphs.effect.drain.component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexColors;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class DrainComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, DrainComponent> componentType;

    public static ComponentType<EntityStore, DrainComponent> getComponentType() {
        return componentType;
    }

    public static void setComponentType(ComponentType<EntityStore, DrainComponent> type) {
        componentType = type;
    }

    private final List<DrainEntry> entries = new ArrayList<>();

    public DrainComponent() {
    }

    public void addEntry(DrainEntry entry) {
        entries.add(entry);
    }

    public List<DrainEntry> getEntries() {
        return entries;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Nonnull
    @Override
    public DrainComponent clone() {
        DrainComponent copy = new DrainComponent();
        for (DrainEntry entry : entries) {
            copy.entries.add(entry.clone());
        }
        return copy;
    }

    public static class DrainEntry {
        private final int sourceStatIndex;
        private final float conversionRate;
        private final float totalDrainAmount;
        private float drainedSoFar;
        private final float drainPerTick;
        private int ticksRemaining;
        private final HexContext hexContext;
        private final List<String> nextGlyphIds;
        private final Ref<EntityStore> hexEntityRef;
        private final HexColors colors;

        public DrainEntry(int sourceStatIndex, float conversionRate, float totalDrainAmount,
                int durationTicks, HexContext hexContext, List<String> nextGlyphIds,
                Ref<EntityStore> hexEntityRef, HexColors colors) {
            this.sourceStatIndex = sourceStatIndex;
            this.conversionRate = conversionRate;
            this.totalDrainAmount = totalDrainAmount;
            this.drainedSoFar = 0f;
            this.drainPerTick = totalDrainAmount / durationTicks;
            this.ticksRemaining = durationTicks;
            this.hexContext = hexContext;
            this.nextGlyphIds = nextGlyphIds;
            this.hexEntityRef = hexEntityRef;
            this.colors = colors;
        }

        public int getSourceStatIndex() {
            return sourceStatIndex;
        }

        public float getConversionRate() {
            return conversionRate;
        }

        public float getTotalDrainAmount() {
            return totalDrainAmount;
        }

        public float getDrainedSoFar() {
            return drainedSoFar;
        }

        public void addDrained(float amount) {
            drainedSoFar += amount;
        }

        public float getDrainPerTick() {
            return drainPerTick;
        }

        public int getTicksRemaining() {
            return ticksRemaining;
        }

        public void decrementTicks() {
            ticksRemaining--;
        }

        public HexContext getHexContext() {
            return hexContext;
        }

        public List<String> getNextGlyphIds() {
            return nextGlyphIds;
        }

        public Ref<EntityStore> getHexEntityRef() {
            return hexEntityRef;
        }

        public HexColors getColors() {
            return colors;
        }

        @Nonnull
        public DrainEntry clone() {
            DrainEntry copy = new DrainEntry(sourceStatIndex, conversionRate, totalDrainAmount,
                    ticksRemaining, hexContext != null ? hexContext.copy() : null,
                    new ArrayList<>(nextGlyphIds), hexEntityRef, colors);
            copy.drainedSoFar = this.drainedSoFar;
            return copy;
        }
    }
}
