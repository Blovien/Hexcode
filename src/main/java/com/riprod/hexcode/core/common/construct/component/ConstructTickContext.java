package com.riprod.hexcode.core.common.construct.component;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ConstructTickContext {

    private final ArchetypeChunk<EntityStore> chunk;
    private final int index;
    private final CommandBuffer<EntityStore> buffer;
    private final Ref<EntityStore> entityRef;

    public ConstructTickContext(ArchetypeChunk<EntityStore> chunk,
            int index, CommandBuffer<EntityStore> buffer, Ref<EntityStore> entityRef) {
        this.chunk = chunk;
        this.index = index;
        this.buffer = buffer;
        this.entityRef = entityRef;
    }

    public ArchetypeChunk<EntityStore> getChunk() {
        return chunk;
    }

    public int getIndex() {
        return index;
    }

    public CommandBuffer<EntityStore> getBuffer() {
        return buffer;
    }

    public Ref<EntityStore> getEntityRef() {
        return entityRef;
    }
}
