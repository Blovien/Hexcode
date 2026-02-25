package com.riprod.hexcode.core.crafting.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PedestalBlockComponent implements Component<ChunkStore> {

    public static final BuilderCodec<PedestalBlockComponent> CODEC =
        BuilderCodec.builder(PedestalBlockComponent.class, PedestalBlockComponent::new)
            .append(
                new KeyedCodec<>("EssenceItemId", Codec.STRING),
                (state, id) -> state.essenceItemId = id,
                state -> state.essenceItemId
            )
            .add()
            .append(
                new KeyedCodec<>("MaxObelisks", Codec.INTEGER),
                (state, v) -> state.maxObelisks = v,
                state -> state.maxObelisks
            )
            .add()
            .build();

    private static ComponentType<ChunkStore, PedestalBlockComponent> componentType;

    public static void setComponentType(ComponentType<ChunkStore, PedestalBlockComponent> type) {
        componentType = type;
    }

    public static ComponentType<ChunkStore, PedestalBlockComponent> getComponentType() {
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
        PedestalBlockComponent copy = new PedestalBlockComponent();
        copy.essenceItemId = this.essenceItemId;
        copy.maxObelisks = this.maxObelisks;
        return copy;
    }
}
