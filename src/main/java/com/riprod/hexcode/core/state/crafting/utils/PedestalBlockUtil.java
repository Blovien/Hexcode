package com.riprod.hexcode.core.state.crafting.utils;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalAnchorComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;

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

        String baseKey = blockType.getDefaultStateKey();
        if (baseKey != null) {
            BlockType baseType = BlockType.getAssetMap().getAsset(baseKey);
            if (baseType != null) {
                blockType = baseType;
            }
        }

        chunk.setBlockInteractionState(pos, blockType, stateName);
    }

    public static PedestalBlockComponent resolvePedestal(Ref<EntityStore> playerRef,
            CommandBuffer<EntityStore> buffer) {

        HexcasterCraftingComponent craftingComp = buffer.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null) {
            return null;
        }

        Ref<EntityStore> anchorRef = craftingComp.getPedestalEntityRef();
        if (anchorRef == null || !anchorRef.isValid()) {
            return null;
        }

        PedestalAnchorComponent anchor = buffer.getComponent(anchorRef,
                PedestalAnchorComponent.getComponentType());
        if (anchor == null || anchor.getPedestalLoc() == null) {
            return null;
        }

        Vector3i pos = anchor.getPedestalLoc();
        return BlockModule.getComponent(
                PedestalBlockComponent.getComponentType(),
                buffer.getExternalData().getWorld(),
                pos.getX(), pos.getY(), pos.getZ());
    }
}
