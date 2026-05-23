package com.riprod.hexcode.utils;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Rotation3f;

import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.glyphs.variables.PositionVar;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class BlockUtils {
    public static void moveBlock(Vector3i source, Vector3d destination, World world) {
        int srcX = source.x();
        int srcY = source.y();
        int srcZ = source.z();

        int sourceBlockId = world.getBlock(srcX, srcY, srcZ);
        if (sourceBlockId == BlockType.EMPTY_ID)
            return;

        int rotation = world.getBlockRotationIndex(srcX, srcY, srcZ);

        int destX = (int) Math.floor(destination.x());
        int destY = (int) Math.floor(destination.y());
        int destZ = (int) Math.floor(destination.z());

        Vector3i placement = findAirBlock(world, destX, destY, destZ);
        if (placement == null) {
            return;
        }

        world.setBlock(srcX, srcY, srcZ, "Empty");

        BlockType blockType = BlockType.getAssetMap().getAsset(sourceBlockId);
        WorldChunk destChunk = world.getChunk(
                ChunkUtil.indexChunkFromBlock(placement.x(), placement.z()));
        if (destChunk != null) {
            destChunk.setBlock(placement.x(), placement.y(), placement.z(),
                    sourceBlockId, blockType, rotation, 0, 0);
        }
    }

    public static Vector3i findAirBlock(World world, int x, int y, int z) {
        return findAirBlock(world, x, y, z, 5);
    }

    public static Vector3i findAirBlock(World world, int x, int y, int z, int maxSearchRadius) {
        if (world.getBlock(x, y, z) == 0)
            return new Vector3i(x, y, z);

        for (int dist = 1; dist <= maxSearchRadius; dist++) {
            for (int dy = dist; dy >= -dist; dy--) {
                for (int dx = -(dist - Math.abs(dy)); dx <= dist - Math.abs(dy); dx++) {
                    int dz = dist - Math.abs(dy) - Math.abs(dx);
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

    public static void moveToDestination(HexVar var, Vector3d dest, World world, HexContext hexContext) {
        if (var instanceof EntityVar entityVar) {
            Ref<EntityStore> entityRef = entityVar.getRef(hexContext.getAccessor());
            if (entityRef == null || !entityRef.isValid()) return;

            TransformComponent tc = hexContext.getAccessor().getComponent(entityRef,
                    TransformComponent.getComponentType());
            if (tc == null) return;

            Player player = hexContext.getAccessor().getComponent(entityRef, Player.getComponentType());
            if (player != null) {
                Rotation3f rotation = tc.getRotation();
                Teleport teleport = Teleport.createForPlayer(dest, rotation);
                hexContext.getAccessor().addComponent(entityRef, Teleport.getComponentType(), teleport);
            } else {
                tc.setPosition(new Vector3d(dest.x(), dest.y(), dest.z()));
            }
        } else if (var instanceof BlockVar blockVar && blockVar.getValue() != null) {
            moveBlock(blockVar.getValue(), dest, world);
        } else if (var instanceof PositionVar posVar && posVar.getValue() != null) {
            Vector3i sourceBlock = new Vector3i(
                    (int) Math.floor(posVar.getValue().x()),
                    (int) Math.floor(posVar.getValue().y()),
                    (int) Math.floor(posVar.getValue().z()));
            moveBlock(sourceBlock, dest, world);
        }
    }

    public static void swapPair(HexVar a, HexVar b, World world, HexContext hexContext) {
        Vector3d posA = HexVarUtil.position(a, hexContext.getAccessor());
        Vector3d posB = HexVarUtil.position(b, hexContext.getAccessor());

        if (posA != null && posB != null) {
            moveToDestination(a, posB, world, hexContext);
            moveToDestination(b, posA, world, hexContext);
        }
    }
}
