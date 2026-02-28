package com.riprod.hexcode.core.crafting.utils;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

public class PedestalBlockUtil {

    public static void changeBlockState(World world, Vector3i pos, String stateName) {
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
        if (chunk == null) {
            return;
        }

        BlockType blockType = chunk.getBlockType(pos.x, pos.y, pos.z);
        if (blockType == null || blockType.isUnknown()) {
            return;
        }

        chunk.setBlockInteractionState(pos, blockType, stateName);
    }

    public static boolean isObelisk(BlockType type) {
        if (type == null) {
            return false;
        }
        return "Hexcode_Obelisk".equals(type.getId());
    }

    public static boolean isPedestal(BlockType type) {
        if (type == null) {
            return false;
        }
        return type.getId() != null && type.getId().startsWith("Hexcode_Pedestal");
    }
}
