package com.riprod.hexcode.core.state.execution.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexes.component.Hex;

public class HexcasterExecutionComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, HexcasterExecutionComponent> componentType;

    public static final BuilderCodec<HexcasterExecutionComponent> CODEC = BuilderCodec
            .builder(HexcasterExecutionComponent.class, HexcasterExecutionComponent::new)
            .append(new KeyedCodec<>("Hex", Hex.CODEC),
                    (c, v) -> c.hex = v,
                    c -> c.hex)
            .add()
            .append(new KeyedCodec<>("CastCount", Codec.INTEGER),
                    (c, v) -> c.castCount = v,
                    c -> c.castCount)
            .add()
            .append(new KeyedCodec<>("CumulativeDecay", Codec.FLOAT),
                    (c, v) -> c.cumulativeDecay = v,
                    c -> c.cumulativeDecay)
            .add()
            .build();

    public HexcasterExecutionComponent() {
    }

    public static void setComponentType(ComponentType<EntityStore, HexcasterExecutionComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, HexcasterExecutionComponent> getComponentType() {
        return componentType;
    }

    private Hex hex;
    private int castCount = 0;
    private float cumulativeDecay = 0f;
    private boolean holdingPrimary = false;
    private Map<UUID, List<Ref<EntityStore>>> dependencies = new HashMap<>();

    private transient List<VolatilityTracker> activeTrackers = new ArrayList<>();

    public void registerActiveTracker(VolatilityTracker tracker) {
        if (activeTrackers == null)
            activeTrackers = new ArrayList<>();
        if (tracker == null)
            return;
        activeTrackers.add(tracker);
    }

    /**
     * Drops trackers whose budget has reached 0. Natural end state for any
     * spell (it either consumed all volatility or was cancelled to 0).
     */
    public void pruneCompletedTrackers() {
        if (activeTrackers == null || activeTrackers.isEmpty())
            return;
        activeTrackers.removeIf(t -> t == null || t.getRemainingBudget() <= 0f);
    }

    /**
     * Active spell count. Prunes completed trackers first so callers see the
     * live count, not stale entries left by natural completion.
     */
    public int getActiveCount() {
        pruneCompletedTrackers();
        return activeTrackers == null ? 0 : activeTrackers.size();
    }

    public List<VolatilityTracker> getActiveTrackers() {
        if (activeTrackers == null)
            activeTrackers = new ArrayList<>();
        return activeTrackers;
    }

    /**
     * Cancels every active spell by zeroing its volatility and firing
     * {@link SpellCancelledEvent}. Individual constructs / multi-tick glyphs
     * observe the fizzled tracker on their own tick and self-remove. Trackers
     * are not removed from the list here — they drain lazily on the next
     * {@link #pruneCompletedTrackers()} call, which runs from
     * {@link #getActiveCount()}.
     */
    public void cancelAll(Ref<EntityStore> casterRef) {
        if (activeTrackers == null || activeTrackers.isEmpty())
            return;
        for (VolatilityTracker tracker : new ArrayList<>(activeTrackers)) {
            if (tracker == null)
                continue;
            if (tracker.getRemainingBudget() <= 0f)
                continue;
            tracker.setBudget(0f);
        }
    }

    public boolean isHoldingPrimary() {
        return holdingPrimary;
    }

    public void setHoldingPrimary(boolean holding) {
        this.holdingPrimary = holding;
    }

    @Nullable
    public Hex getActiveHex() {
        return hex;
    }

    public void setActiveHex(@Nullable Hex hex) {
        this.hex = hex;
    }

    public boolean hasActiveHex() {
        return hex != null;
    }

    public int getCastCount() {
        return castCount;
    }

    public float getCumulativeDecay() {
        return cumulativeDecay;
    }

    public void addDependency(UUID hexId, Ref<EntityStore> dependent) {
        dependencies.computeIfAbsent(hexId, k -> new java.util.ArrayList<>()).add(dependent);
    }

    public Map<UUID, List<Ref<EntityStore>>> getDependencies() {
        return dependencies;
    }

    public List<Ref<EntityStore>> getDependenciesForHex(UUID hexId) {
        return dependencies.getOrDefault(hexId, java.util.Collections.emptyList());
    }

    public List<Ref<EntityStore>> getDependencyList() {
        return dependencies.values().stream().flatMap(List::stream).toList();
    }

    public void advanceCast(float decayRate, float maxVolatility) {
        this.castCount++;
        this.cumulativeDecay += decayRate * maxVolatility;
    }

    public void resetCastState() {
        this.castCount = 0;
        this.cumulativeDecay = 0f;
    }

    public void clear(CommandBuffer<EntityStore> buffer) {

    }

    @Nonnull
    @Override
    public HexcasterExecutionComponent clone() {
        HexcasterExecutionComponent copy = new HexcasterExecutionComponent();
        copy.hex = this.hex != null ? this.hex.clone() : null;
        copy.castCount = this.castCount;
        copy.cumulativeDecay = this.cumulativeDecay;
        copy.holdingPrimary = this.holdingPrimary;
        return copy;
    }
}
