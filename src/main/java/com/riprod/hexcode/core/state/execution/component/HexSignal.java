package com.riprod.hexcode.core.state.execution.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.state.execution.Executor;

public class HexSignal implements Component<EntityStore> {

    private static ComponentType<EntityStore, HexSignal> componentType;

    public static void setComponentType(ComponentType<EntityStore, HexSignal> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, HexSignal> getComponentType() {
        return componentType;
    }

    private List<SignalEntry> entries;

    public HexSignal() {
        this.entries = new ArrayList<>();
    }

    public HexSignal(HexContext hexContext, Ref<EntityStore> hexEntityRef,
            Glyph sourceGlyph, List<String> nextGlyphIds,
            @Nullable Map<String, Integer> outputSlots) {
        this.entries = new ArrayList<>();
        this.entries.add(new SignalEntry(hexContext, hexEntityRef, sourceGlyph, nextGlyphIds, outputSlots));
    }

    public SignalEntry getPrimary() {
        if (entries.isEmpty()) return null;
        return entries.get(0);
    }

    public List<SignalEntry> getEntries() {
        return entries;
    }

    public void addEntry(SignalEntry entry) {
        entries.add(entry);
    }

    public void replacePrimary(HexContext hexContext, List<String> nextGlyphIds) {
        if (entries.isEmpty()) return;
        SignalEntry primary = entries.get(0);
        primary.hexContext = hexContext;
        primary.nextGlyphIds = nextGlyphIds;
    }

    public void replaceAll(HexContext hexContext, List<String> nextGlyphIds) {
        for (SignalEntry entry : entries) {
            entry.hexContext = hexContext;
            entry.nextGlyphIds = nextGlyphIds;
        }
    }

    public boolean hasLiveEntries(CommandBuffer<EntityStore> buffer) {
        for (SignalEntry entry : entries) {
            Ref<EntityStore> ref = entry.getHexEntityRef();
            if (ref != null && ref.isValid()) {
                RootGlyph root = buffer.getComponent(ref, RootGlyph.getComponentType());
                if (root != null && root.getRoot() != null && root.getRoot().isAlive()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void fireAllEntries(CommandBuffer<EntityStore> buffer) {
        ComponentAccessor<ChunkStore> chunkAccessor = buffer.getExternalData().getWorld()
                .getChunkStore().getStore();

        for (SignalEntry entry : entries) {
            if (entry.getNextGlyphIds() == null || entry.getNextGlyphIds().isEmpty()) continue;

            Ref<EntityStore> hexRef = entry.getHexEntityRef();
            if (hexRef == null || !hexRef.isValid()) continue;

            RootGlyph execComp = buffer.getComponent(hexRef, RootGlyph.getComponentType());
            if (execComp == null) continue;

            HexRoot root = execComp.getRoot();
            if (root == null || !root.isAlive()) continue;

            HexContext ctx = entry.getHexContext().copy();
            ctx.UpdateAccessor(buffer);
            ctx.UpdateChunkAccessor(chunkAccessor);

            Executor.continueExecution(entry.getNextGlyphIds(), ctx);
        }
    }

    public void decrementAllWaiters(CommandBuffer<EntityStore> buffer) {
        for (SignalEntry entry : entries) {
            Ref<EntityStore> hexRef = entry.getHexEntityRef();
            if (hexRef == null || !hexRef.isValid()) continue;

            RootGlyph execComp = buffer.getComponent(hexRef, RootGlyph.getComponentType());
            if (execComp != null) {
                execComp.decrementExternalWaiters();
            }
        }
    }

    @Nonnull
    @Override
    public HexSignal clone() {
        HexSignal copy = new HexSignal();
        for (SignalEntry entry : entries) {
            copy.entries.add(entry.clone());
        }
        return copy;
    }

    public static class SignalEntry {
        HexContext hexContext;
        Ref<EntityStore> hexEntityRef;
        Glyph sourceGlyph;
        List<String> nextGlyphIds;
        Map<String, Integer> outputSlots;

        public SignalEntry() {
        }

        public SignalEntry(HexContext hexContext, Ref<EntityStore> hexEntityRef,
                Glyph sourceGlyph, List<String> nextGlyphIds,
                @Nullable Map<String, Integer> outputSlots) {
            this.hexContext = hexContext;
            this.hexEntityRef = hexEntityRef;
            this.sourceGlyph = sourceGlyph;
            this.nextGlyphIds = nextGlyphIds;
            this.outputSlots = outputSlots != null ? outputSlots : new HashMap<>();
        }

        public HexContext getHexContext() {
            return hexContext;
        }

        public void setHexContext(HexContext hexContext) {
            this.hexContext = hexContext;
        }

        public Ref<EntityStore> getHexEntityRef() {
            return hexEntityRef;
        }

        public Glyph getSourceGlyph() {
            return sourceGlyph;
        }

        public List<String> getNextGlyphIds() {
            return nextGlyphIds;
        }

        public void setNextGlyphIds(List<String> nextGlyphIds) {
            this.nextGlyphIds = nextGlyphIds;
        }

        @Nullable
        public Integer getOutputSlot(String key) {
            return outputSlots != null ? outputSlots.get(key) : null;
        }

        public Map<String, Integer> getOutputSlots() {
            return outputSlots;
        }

        @Nonnull
        public SignalEntry clone() {
            SignalEntry copy = new SignalEntry();
            copy.hexContext = this.hexContext != null ? this.hexContext.copy() : null;
            copy.hexEntityRef = this.hexEntityRef;
            copy.sourceGlyph = this.sourceGlyph;
            copy.nextGlyphIds = this.nextGlyphIds != null ? new ArrayList<>(this.nextGlyphIds) : null;
            copy.outputSlots = this.outputSlots != null ? new HashMap<>(this.outputSlots) : null;
            return copy;
        }
    }
}
