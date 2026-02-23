package com.riprod.hexcode.core.crafting.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;

public class ObeliskBlockState implements Component<ChunkStore> {

    public static final BuilderCodec<ObeliskBlockState> CODEC =
        BuilderCodec.builder(ObeliskBlockState.class, ObeliskBlockState::new)
            .addField(
                new KeyedCodec<>("Power", Codec.INTEGER),
                (state, power) -> state.power = power,
                state -> state.power
            )
            .build();

    private static ComponentType<ChunkStore, ObeliskBlockState> componentType;

    public static void setComponentType(ComponentType<ChunkStore, ObeliskBlockState> type) {
        componentType = type;
    }

    public static ComponentType<ChunkStore, ObeliskBlockState> getComponentType() {
        return componentType;
    }

    private int power = 1;

    public int getPower() {
        return power;
    }

    @Nonnull
    @Override
    public Component<ChunkStore> clone() {
        ObeliskBlockState copy = new ObeliskBlockState();
        copy.power = this.power;
        return copy;
    }
}
