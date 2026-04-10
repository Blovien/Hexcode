package com.riprod.hexcode.core.common.hexcaster.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.state.HexState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.riprod.hexcode.core.state.execution.component.HexContext;

public class HexcasterComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, HexcasterComponent> componentType;

    public static final BuilderCodec<HexcasterComponent> CODEC = BuilderCodec
            .builder(HexcasterComponent.class, HexcasterComponent::new)
            .append(new KeyedCodec<>("State", new EnumCodec<>(HexState.class)),
                    (c, v) -> c.currentState = v,
                    c -> c.currentState)
            .add()
            .append(new KeyedCodec<>("Hex", Hex.CODEC),
                    (c, v) -> c.hex = v,
                    c -> c.hex)
            .add()
            .append(new KeyedCodec<>("CastCount", Codec.INTEGER),
                    (c, v) -> c.castCount = v,
                    c -> c.castCount)
            .add()
            .build();

    private HexState currentState = HexState.IDLE;
    private HexState pendingState = null;
    private Hex hex;
    private int castCount = 0;
    private boolean holdingPrimary = false;

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

    public void incrementCastCount() {
        this.castCount++;
    }

    public HexState getState() {
        return currentState;
    }

    public void requestStateChange(HexState newMode) {
        if (this.currentState == newMode || this.pendingState == newMode) {
            return;
        }
        this.pendingState = newMode;
    }

    public HexState consumePendingState() {
        HexState previousState = this.pendingState;
        pendingState = null;
        return previousState;
    }

    public void applyState(HexState newState) {
        if (this.currentState == newState) {
            return;
        }
        this.currentState = newState;
    }

    // Drawing Mode
    private Ref<EntityStore> trailRef = null;
    private long lastParticleSpawnMillis = 0;
    private long drawStartTimeMillis = 0;

    // training mode
    private String trainingShapeId = null;

    // Crafting Mode
    private Map<String, Float> lastTickMap = new HashMap<>();

    // OnRelease trigger subscriptions — fires when primary interaction ends
    public static class PendingRelease {
        public final List<String> childIds;
        public final HexContext hexContext;
        public PendingRelease(List<String> childIds, HexContext hexContext) {
            this.childIds = childIds;
            this.hexContext = hexContext;
        }
    }
    private transient List<PendingRelease> pendingReleases = new ArrayList<>();

    public void addPendingRelease(PendingRelease release) {
        if (pendingReleases == null) pendingReleases = new ArrayList<>();
        pendingReleases.add(release);
    }

    public List<PendingRelease> consumePendingReleases() {
        if (pendingReleases == null || pendingReleases.isEmpty()) return List.of();
        List<PendingRelease> out = pendingReleases;
        pendingReleases = new ArrayList<>();
        return out;
    }

    public HexcasterComponent() {
    }

    public static void setComponentType(ComponentType<EntityStore, HexcasterComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, HexcasterComponent> getComponentType() {
        return componentType;
    }

    /** @deprecated */
    public void clearDrawingState() {
        this.trailRef = null;
        this.lastParticleSpawnMillis = 0;
        this.drawStartTimeMillis = 0;
    }

    /** @deprecated */
    public String getTrainingShapeId() {
        return trainingShapeId;
    }

    /** @deprecated */
    public void setTrainingShapeId(String shapeId) {
        this.trainingShapeId = shapeId;
    }

    /** @deprecated */
    public String consumeTrainingShapeId() {
        String id = this.trainingShapeId;
        this.trainingShapeId = null;
        return id;
    }

    public void setDrawStartTimeMillis(long drawStartTimeMillis) {
        this.drawStartTimeMillis = drawStartTimeMillis;
    }

    public float getTickLength(String keyId) {
        return this.lastTickMap.getOrDefault(keyId, 0f);
    }

    public void setTickLength(String keyId, float value) {
        this.lastTickMap.put(keyId, value);
    }

    public void incrementTickLength(String keyId, float dt) {
        this.lastTickMap.merge(keyId, dt, Float::sum);
    }

    @Nonnull
    @Override
    public HexcasterComponent clone() {
        HexcasterComponent copy = new HexcasterComponent();
        copy.currentState = this.currentState;
        copy.trailRef = this.trailRef;
        copy.lastParticleSpawnMillis = this.lastParticleSpawnMillis;
        copy.drawStartTimeMillis = this.drawStartTimeMillis;
        copy.trainingShapeId = this.trainingShapeId;
        copy.hex = this.hex != null ? this.hex.clone() : null;
        copy.castCount = this.castCount;
        copy.holdingPrimary = this.holdingPrimary;
        copy.lastTickMap = new HashMap<>(this.lastTickMap);
        return copy;
    }
}
