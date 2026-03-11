package com.riprod.hexcode.core.common.hexcaster.component;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.state.HexState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

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
    private long drawStartTimeMillis = 0;

    // training mode
    private String trainingShapeId = null;

    // Crafting Mode
    private Ref<EntityStore> pendingPedestalRef = null;
    private Map<String, Float> lastTickMap = new HashMap<>();

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

    public Ref<EntityStore> getPendingPedestalRef() {
        return pendingPedestalRef;
    }

    public void setPendingPedestalRef(@Nullable Ref<EntityStore> ref) {
        this.pendingPedestalRef = ref;
    }

    public Ref<EntityStore> consumePendingPedestalRef() {
        Ref<EntityStore> ref = this.pendingPedestalRef;
        this.pendingPedestalRef = null;
        return ref;
    }

    public void clearCraftingState() {
        this.pendingPedestalRef = null;
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

    /** @deprecated */
    public void setTrailRef(Ref<EntityStore> trailRef) {
        this.trailRef = trailRef;
    }

    /** @deprecated */
    public Ref<EntityStore> getTrailRef() {
        return trailRef;
    }

    /** @deprecated */
    public long getDrawStartTimeMillis() {
        return drawStartTimeMillis;
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
        copy.pendingPedestalRef = this.pendingPedestalRef;
        copy.trainingShapeId = this.trainingShapeId;
        return copy;
    }
}
