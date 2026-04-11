package com.riprod.hexcode.core.state.execution.component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.state.HexState;
import com.riprod.hexcode.utils.CleanupUtils;

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

    public void clear(CommandBuffer<EntityStore> buffer) {

    }

    @Nonnull
    @Override
    public HexcasterExecutionComponent clone() {
        HexcasterExecutionComponent copy = new HexcasterExecutionComponent();
        copy.hex = this.hex != null ? this.hex.clone() : null;
        copy.castCount = this.castCount;
        copy.holdingPrimary = this.holdingPrimary;
        return copy;
    }
}
