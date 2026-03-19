package com.riprod.hexcode.core.common.obelisk.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;

public class ObeliskBlockComponent implements Component<ChunkStore> {

    public static final BuilderCodec<ObeliskBlockComponent> CODEC =
        BuilderCodec.builder(ObeliskBlockComponent.class, ObeliskBlockComponent::new)
            .append(
                new KeyedCodec<>("Power", Codec.INTEGER),
                (state, power) -> state.power = power,
                state -> state.power
            )
            .add()
            .append(
                new KeyedCodec<>("HandlerId", Codec.STRING),
                (state, v) -> state.handlerId = v,
                state -> state.handlerId
            )
            .add()
            .build();

    private static ComponentType<ChunkStore, ObeliskBlockComponent> componentType;

    public static void setComponentType(ComponentType<ChunkStore, ObeliskBlockComponent> type) {
        componentType = type;
    }

    public static ComponentType<ChunkStore, ObeliskBlockComponent> getComponentType() {
        return componentType;
    }

    private int power = 1;
    private String handlerId = "";

    public int getPower() {
        return power;
    }

    public String getHandlerId() {
        return handlerId;
    }

    @Nonnull
    @Override
    public Component<ChunkStore> clone() {
        ObeliskBlockComponent copy = new ObeliskBlockComponent();
        copy.power = this.power;
        copy.handlerId = this.handlerId;
        return copy;
    }
}
