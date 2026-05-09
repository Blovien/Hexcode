package com.riprod.hexcode.core.common.hexcaster.component;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.state.HexState;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class HexcasterComponent implements Component<EntityStore> {

    private static ComponentType<EntityStore, HexcasterComponent> componentType;

    public static final BuilderCodec<HexcasterComponent> CODEC = BuilderCodec
            .builder(HexcasterComponent.class, HexcasterComponent::new)
            .build();

    private HexState currentState = HexState.IDLE;
    private HexState pendingState = null;

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

    private Ref<EntityStore> trailRef = null;
    private long lastParticleSpawnMillis = 0;

    private String trainingShapeId = null;
    private String trainingPackOverride = null;

    private Map<String, Float> lastTickMap = new HashMap<>();

    public HexcasterComponent() {
    }

    public static void setComponentType(ComponentType<EntityStore, HexcasterComponent> type) {
        componentType = type;
    }

    public static ComponentType<EntityStore, HexcasterComponent> getComponentType() {
        return componentType;
    }

    public String getTrainingShapeId() {
        return trainingShapeId;
    }

    public void setTrainingShapeId(String shapeId) {
        this.trainingShapeId = shapeId;
    }

    public String consumeTrainingShapeId() {
        String id = this.trainingShapeId;
        this.trainingShapeId = null;
        return id;
    }

    public String getTrainingPackOverride() {
        return trainingPackOverride;
    }

    public void setTrainingPackOverride(String packName) {
        this.trainingPackOverride = packName;
    }

    public String consumeTrainingPackOverride() {
        String p = this.trainingPackOverride;
        this.trainingPackOverride = null;
        return p;
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
        copy.trainingShapeId = this.trainingShapeId;
        copy.trainingPackOverride = this.trainingPackOverride;
        copy.lastTickMap = new HashMap<>(this.lastTickMap);
        return copy;
    }
}
