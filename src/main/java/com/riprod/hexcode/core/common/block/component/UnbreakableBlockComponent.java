package com.riprod.hexcode.core.common.block.component;

import java.util.List;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class UnbreakableBlockComponent implements Component<ChunkStore> {

    private static ComponentType<ChunkStore, UnbreakableBlockComponent> componentType;

    public static void setComponentType(ComponentType<ChunkStore, UnbreakableBlockComponent> type) {
        componentType = type;
    }

    public static ComponentType<ChunkStore, UnbreakableBlockComponent> getComponentType() {
        return componentType;
    }

    public static void protect(World world, Vector3i pos) {
        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        protect(world, pos, chunkStore);
    }

    public static void protect(World world, Vector3i pos, Store<ChunkStore> chunkStore) {
        if (pos == null)
            return;
        Ref<ChunkStore> blockRef = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z);
        if (blockRef == null)
            return;
        chunkStore.putComponent(blockRef, componentType, new UnbreakableBlockComponent());
    }

    public static void unprotect(World world, Vector3i pos) {
        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        unprotect(world, pos, chunkStore);
    }

    public static void unprotect(World world, Vector3i pos, Store<ChunkStore> chunkStore) {
        if (pos == null)
            return;
        Ref<ChunkStore> blockRef = BlockModule.getBlockEntity(world, pos.x, pos.y, pos.z);
        if (blockRef == null)
            return;
        if (chunkStore.getComponent(blockRef, componentType) == null)
            return;
        chunkStore.removeComponent(blockRef, componentType);
    }

    public static void protectBlocks(World world, List<Vector3i> poses) {
        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        for (Vector3i pos : poses) {
            protect(world, pos, chunkStore);
        }
    }

    public static void unprotectBlocks(World world, List<Vector3i> poses) {
        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        for (Vector3i pos : poses) {
            unprotect(world, pos, chunkStore);
        }
    }

    public static boolean isProtected(World world, Vector3i pos) {
        return BlockModule.getComponent(componentType, world, pos.x, pos.y, pos.z) != null;
    }

    @Nonnull
    @Override
    public Component<ChunkStore> clone() {
        return new UnbreakableBlockComponent();
    }
}
