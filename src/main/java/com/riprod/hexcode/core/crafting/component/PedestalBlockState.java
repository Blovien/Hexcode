package com.riprod.hexcode.core.crafting.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PedestalBlockState implements Component<ChunkStore> {

    public static final BuilderCodec<PedestalBlockState> CODEC =
        BuilderCodec.builder(PedestalBlockState.class, PedestalBlockState::new)
            .addField(
                new KeyedCodec<>("EssenceItemId", Codec.STRING),
                (state, id) -> state.essenceItemId = id,
                state -> state.essenceItemId
            )
            .addField(
                new KeyedCodec<>("MaxObelisks", Codec.INTEGER),
                (state, v) -> state.maxObelisks = v,
                state -> state.maxObelisks
            )
            .build();

    private static ComponentType<ChunkStore, PedestalBlockState> componentType;

    public static void setComponentType(ComponentType<ChunkStore, PedestalBlockState> type) {
        componentType = type;
    }

    public static ComponentType<ChunkStore, PedestalBlockState> getComponentType() {
        return componentType;
    }

    private String essenceItemId = null;
    private int maxObelisks = 4;

    @Nullable
    public String getEssenceItemId() {
        return essenceItemId;
    }

    public void setEssenceItemId(@Nullable String essenceItemId) {
        this.essenceItemId = essenceItemId;
    }

    public int getMaxObelisks() {
        return maxObelisks;
    }

    @Nonnull
    @Override
    public Component<ChunkStore> clone() {
        PedestalBlockState copy = new PedestalBlockState();
        copy.essenceItemId = this.essenceItemId;
        copy.maxObelisks = this.maxObelisks;
        return copy;
    }
}
