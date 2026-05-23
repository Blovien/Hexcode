package com.riprod.hexcode.core.common.imbuement.block;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.riprod.hexcode.core.common.imbuement.component.ImbuedBlockComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ImbuedBlockTickSystem extends EntityTickingSystem<ChunkStore> {

    @Nullable
    @Override
    public Query<ChunkStore> getQuery() {
        return ImbuedBlockComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<ChunkStore> chunk,
            @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> buffer) {

        ImbuedBlockComponent comp = chunk.getComponent(index, ImbuedBlockComponent.getComponentType());
        if (comp == null) return;

        BlockModule.BlockStateInfo info = chunk.getComponent(index, BlockModule.BlockStateInfo.getComponentType());
        if (info == null) return;

        Vector3i pos = resolvePos(buffer, info);
        if (pos == null) return;

        World world = buffer.getExternalData().getWorld();
        BlockType blockType = resolveBlockType(world, pos);
        if (blockType == null) return;

        BlockImbuementCapacity.Capacity cap = BlockImbuementCapacity.forBlock(blockType);
        if (comp.getSlotsReady() >= cap.getSlots()) return;

        long now = world.getTick();
        long anchor = comp.getLastChargeTick();
        if (anchor <= 0L) {
            comp.setLastChargeTick(now);
            return;
        }

        int interval = cap.getRechargeIntervalTicks();
        if (interval <= 0) return;

        int gained = 0;
        while (comp.getSlotsReady() + gained < cap.getSlots()
                && (now - anchor) >= (long) interval) {
            anchor += interval;
            gained++;
        }
        if (gained > 0) {
            comp.setSlotsReady(comp.getSlotsReady() + gained);
            comp.setLastChargeTick(anchor);
            info.markNeedsSaving(buffer);
        }
    }

    @Nullable
    private static Vector3i resolvePos(@Nonnull CommandBuffer<ChunkStore> buffer,
            @Nonnull BlockModule.BlockStateInfo info) {
        if (!info.getChunkRef().isValid()) return null;
        BlockChunk blockChunk = buffer.getComponent(info.getChunkRef(), BlockChunk.getComponentType());
        if (blockChunk == null) return null;
        int blockIndex = info.getIndex();
        int localX = ChunkUtil.xFromBlockInColumn(blockIndex);
        int localY = ChunkUtil.yFromBlockInColumn(blockIndex);
        int localZ = ChunkUtil.zFromBlockInColumn(blockIndex);
        int worldX = ChunkUtil.worldCoordFromLocalCoord(blockChunk.x(), localX);
        int worldZ = ChunkUtil.worldCoordFromLocalCoord(blockChunk.z(), localZ);
        return new Vector3i(worldX, localY, worldZ);
    }

    @Nullable
    private static BlockType resolveBlockType(@Nonnull World world, @Nonnull Vector3i pos) {
        int blockId = world.getBlock(pos.x, pos.y, pos.z);
        if (blockId == 0) return null;
        return BlockType.getAssetMap().getAsset(blockId);
    }
}
