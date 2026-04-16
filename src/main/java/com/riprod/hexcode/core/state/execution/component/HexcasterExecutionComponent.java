package com.riprod.hexcode.core.state.execution.component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
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
