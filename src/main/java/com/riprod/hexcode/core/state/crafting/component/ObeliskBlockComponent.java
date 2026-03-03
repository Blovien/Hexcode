package com.riprod.hexcode.core.state.crafting.component;

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
            .build();

    private static ComponentType<ChunkStore, ObeliskBlockComponent> componentType;

    public static void setComponentType(ComponentType<ChunkStore, ObeliskBlockComponent> type) {
        componentType = type;
    }

    public static ComponentType<ChunkStore, ObeliskBlockComponent> getComponentType() {
        return componentType;
    }

    private int power = 1;

    public int getPower() {
        return power;
    }

    @Nonnull
    @Override
    public Component<ChunkStore> clone() {
        ObeliskBlockComponent copy = new ObeliskBlockComponent();
        copy.power = this.power;
        return copy;
    }
}
