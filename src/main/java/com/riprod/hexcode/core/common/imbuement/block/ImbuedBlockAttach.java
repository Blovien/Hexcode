package com.riprod.hexcode.core.common.imbuement.block;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ImbuedBlockAttach {

    private ImbuedBlockAttach() {
    }

    @Nullable
    public static <T extends Component<ChunkStore>> T attach(
            @Nonnull World world, int bx, int by, int bz,
            @Nonnull ComponentType<ChunkStore, T> componentType) {

        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(bx, bz));
        if (chunk == null) return null;

        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        Ref<ChunkStore> chunkRef = chunk.getReference();
        BlockComponentChunk blockComponentChunk = chunkStore.getComponent(
                chunkRef, BlockComponentChunk.getComponentType());
        if (blockComponentChunk == null) return null;

        int blockIndex = ChunkUtil.indexBlockInColumn(bx, by, bz);
        Ref<ChunkStore> existingRef = blockComponentChunk.getEntityReference(blockIndex);
        if (existingRef != null && existingRef.isValid()) {
            return chunkStore.ensureAndGetComponent(existingRef, componentType);
        }

        Holder<ChunkStore> holder = ChunkStore.REGISTRY.newHolder();
        T comp = holder.ensureAndGetComponent(componentType);
        holder.addComponent(BlockModule.BlockStateInfo.getComponentType(),
                new BlockModule.BlockStateInfo(blockIndex, chunkRef));
        chunkStore.addEntity(holder, AddReason.SPAWN);
        return comp;
    }
}
