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

    // Drawing Mode
    private Ref<EntityStore> trailRef = null;
    private long lastParticleSpawnMillis = 0;

    // training mode
    private String trainingShapeId = null;

    // Crafting Mode
    private Map<String, Float> lastTickMap = new HashMap<>();

    private transient List<HexContext> activeContexts = new ArrayList<>();

    public void registerActiveHex(HexContext ctx) {
        if (activeContexts == null) activeContexts = new ArrayList<>();
        activeContexts.add(ctx);
    }

    public void removeActiveHex(HexContext ctx) {
        if (activeContexts == null) return;
        activeContexts.remove(ctx);
    }

    public void cancelAll() {
        if (activeContexts == null || activeContexts.isEmpty()) return;
        for (HexContext ctx : new ArrayList<>(activeContexts)) {
            com.riprod.hexcode.core.state.execution.Executor.fail(ctx);
        }
        activeContexts.clear();
    }

    public int getActiveCount() {
        return activeContexts == null ? 0 : activeContexts.size();
    }

    public List<HexContext> getActiveContexts() {
        if (activeContexts == null) activeContexts = new ArrayList<>();
        return activeContexts;
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
        copy.lastTickMap = new HashMap<>(this.lastTickMap);
        return copy;
    }
}
