package com.riprod.hexcode.core.common.imbuement.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.riprod.hexcode.core.common.imbuement.codec.ImbuementMapCodec;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ImbuedBlockComponent implements Component<ChunkStore> {

    public static final BuilderCodec<ImbuedBlockComponent> CODEC = BuilderCodec
            .builder(ImbuedBlockComponent.class, ImbuedBlockComponent::new)
            .append(
                    new KeyedCodec<>("Slots", ImbuementMapCodec.INSTANCE),
                    (state, v) -> { if (v != null) state.slots = new HashMap<>(v); },
                    state -> state.slots)
            .documentation("Per-slot imbuement data keyed by slot name (e.g. \"Default\")")
            .add()
            .append(
                    new KeyedCodec<>("SlotsReady", Codec.INTEGER),
                    (state, v) -> { if (v != null) state.slotsReady = v; },
                    state -> state.slotsReady)
            .documentation("Number of charged spell slots currently available on this block")
            .add()
            .append(
                    new KeyedCodec<>("LastChargeTick", Codec.LONG),
                    (state, v) -> { if (v != null) state.lastChargeTick = v; },
                    state -> state.lastChargeTick)
            .documentation("World tick at which the recharge cycle is anchored; advances by rechargeInterval per grant")
            .add()
            .build();

    private static ComponentType<ChunkStore, ImbuedBlockComponent> componentType;
    public static void setComponentType(ComponentType<ChunkStore, ImbuedBlockComponent> type) {
        componentType = type;
    }
    public static ComponentType<ChunkStore, ImbuedBlockComponent> getComponentType() {
        return componentType;
    }

    private Map<String, ImbuementData> slots = new HashMap<>();
    private int slotsReady = 0;
    private long lastChargeTick = 0L;

    public Map<String, ImbuementData> getSlots() {
        return slots;
    }

    @Nullable
    public ImbuementData read(String slotKey) {
        return slots.get(slotKey);
    }

    public void write(String slotKey, @Nullable ImbuementData data) {
        if (data == null) slots.remove(slotKey);
        else slots.put(slotKey, data);
    }

    public int getSlotsReady() {
        return slotsReady;
    }

    public void setSlotsReady(int slotsReady) {
        this.slotsReady = slotsReady;
    }

    public long getLastChargeTick() {
        return lastChargeTick;
    }

    public void setLastChargeTick(long lastChargeTick) {
        this.lastChargeTick = lastChargeTick;
    }

    @Nonnull
    @Override
    public Component<ChunkStore> clone() {
        ImbuedBlockComponent copy = new ImbuedBlockComponent();
        copy.slots = new HashMap<>(this.slots);
        copy.slotsReady = this.slotsReady;
        copy.lastChargeTick = this.lastChargeTick;
        return copy;
    }
}
