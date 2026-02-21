package com.riprod.hexcode.components;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.execution.component.HexGraph;
import com.riprod.hexcode.core.execution.component.HexRoot;

public class HexContext {
    public final HexRoot root;
    public final Ref<EntityStore> casterRef;
    public final CommandBuffer<EntityStore> accessor;
    public final ComponentAccessor<ChunkStore> chunkAccessor;
    public final HexGraph spellGraph;

    public HexContext(HexRoot root, CommandBuffer<EntityStore> accessor,
            ComponentAccessor<ChunkStore> chunkAccessor, HexGraph spellGraph) {
        this.root = root;
        this.casterRef = root.getSourceRef();
        this.accessor = accessor;
        this.chunkAccessor = chunkAccessor;
        this.spellGraph = spellGraph;
    }
}
