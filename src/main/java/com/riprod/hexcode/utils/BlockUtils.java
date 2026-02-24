package com.riprod.hexcode.utils;

import java.util.List;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.execution.component.HexContext;
import com.riprod.hexcode.core.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.glyphs.variables.HexVar;

public class BlockUtils {
    public static void moveBlock(Vector3i source, Vector3d destination, World world) {
        int srcX = source.getX();
        int srcY = source.getY();
        int srcZ = source.getZ();

        int sourceBlockId = world.getBlock(srcX, srcY, srcZ);
        if (sourceBlockId == BlockType.EMPTY_ID)
            return;

        int rotation = world.getBlockRotationIndex(srcX, srcY, srcZ);

        int destX = (int) Math.floor(destination.getX());
        int destY = (int) Math.floor(destination.getY());
        int destZ = (int) Math.floor(destination.getZ());

        Vector3i placement = findAirBlock(world, destX, destY, destZ);
        if (placement == null) {
            return;
        }

        world.setBlock(srcX, srcY, srcZ, "Empty");

        BlockType blockType = BlockType.getAssetMap().getAsset(sourceBlockId);
        WorldChunk destChunk = world.getChunk(
                ChunkUtil.indexChunkFromBlock(placement.getX(), placement.getZ()));
        if (destChunk != null) {
            destChunk.setBlock(placement.getX(), placement.getY(), placement.getZ(),
                    sourceBlockId, blockType, rotation, 0, 0);
        }
    }

    public static Vector3i findAirBlock(World world, int x, int y, int z) {
        return findAirBlock(world, x, y, z, 5);
    }

    public static Vector3i findAirBlock(World world, int x, int y, int z, @Nullable int maxSearchRadius) {
        if (world.getBlock(x, y, z) == 0)
            return new Vector3i(x, y, z);

        for (int dist = 1; dist <= maxSearchRadius; dist++) {
            for (int dy = dist; dy >= -dist; dy--) {
                for (int dx = -(dist - Math.abs(dy)); dx <= dist - Math.abs(dy); dx++) {
                    int dz = dist - Math.abs(dy) - Math.abs(dx);
                    // check both +dz and -dz
                    for (int sz = (dz == 0 ? 0 : -1); sz <= 1; sz += 2) {
                        int cx = x + dx;
                        int cy = y + dy;
                        int cz = z + (dz * sz);
                        if (cy < 0 || cy >= 320)
                            continue;
                        if (world.getBlock(cx, cy, cz) == 0) {
                            return new Vector3i(cx, cy, cz);
                        }
                        if (dz == 0)
                            break;
                    }
                }
            }
        }
        return null;
    }

    public static void moveToDestination(HexVar var, int index, Vector3d dest, World world, HexContext hexContext) {
        if (index < 0 || index >= var.size()) return;

        if (var instanceof EntityVar entityVar) {
            Ref<EntityStore> entityRef = entityVar.getRef(index, hexContext.getAccessor());
            if (entityRef == null || !entityRef.isValid()) return;

            TransformComponent tc = hexContext.getAccessor().getComponent(entityRef,
                    TransformComponent.getComponentType());
            if (tc != null) {
                tc.setPosition(new Vector3d(dest.getX(), dest.getY(), dest.getZ()));
            }
        } else if (var instanceof BlockVar blockVar && blockVar.getAt(index) != null) {
            moveBlock(blockVar.getAt(index), dest, world);
        } else if (var instanceof PositionVar posVar && posVar.getAt(index) != null) {
            Vector3i sourceBlock = new Vector3i(
                    (int) Math.floor(posVar.getAt(index).getX()),
                    (int) Math.floor(posVar.getAt(index).getY()),
                    (int) Math.floor(posVar.getAt(index).getZ()));
            moveBlock(sourceBlock, dest, world);
        }
    }

    public static void swapPair(HexVar a, HexVar b, int index, World world, HexContext hexContext) {
        Vector3d posA = SpellVarUtil.resolvePositionAt(a, index, hexContext.getAccessor());
        Vector3d posB = SpellVarUtil.resolvePositionAt(b, index, hexContext.getAccessor());

        if (posA != null && posB != null) {
            moveToDestination(a, index, posB, world, hexContext);
            moveToDestination(b, index, posA, world, hexContext);
        }
    }
}
