package com.riprod.hexcode.core.common.construct;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.construct.component.HexConstruct;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexRoot;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;

public class ConstructTickContext {

    private final Ref<EntityStore> entityRef;
    private final ArchetypeChunk<EntityStore> chunk;
    private final int index;
    private final CommandBuffer<EntityStore> buffer;
    private final HexConstruct construct;

    public ConstructTickContext(Ref<EntityStore> entityRef, ArchetypeChunk<EntityStore> chunk,
            int index, CommandBuffer<EntityStore> buffer, HexConstruct construct) {
        this.entityRef = entityRef;
        this.chunk = chunk;
        this.index = index;
        this.buffer = buffer;
        this.construct = construct;
    }

    public Ref<EntityStore> getEntityRef() {
        return entityRef;
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

    public void fireConditional() {
        fireBranch(construct.getConditionalBranchIds(), null);
    }

    public void fireConditional(@Nullable Consumer<HexContext> injector) {
        fireBranch(construct.getConditionalBranchIds(), injector);
    }

    public void fireBranch(List<String> branchIds) {
        fireBranch(branchIds, null);
    }

    public void fireBranch(List<String> branchIds, @Nullable Consumer<HexContext> injector) {
        if (branchIds == null || branchIds.isEmpty()) return;

        Ref<EntityStore> rootRef = construct.getRootEntityRef();
        if (rootRef == null || !rootRef.isValid()) return;

        RootGlyph rootGlyph = buffer.getComponent(rootRef, RootGlyph.getComponentType());
        if (rootGlyph == null) return;

        HexRoot root = rootGlyph.getRoot();
        if (root == null || !root.isAlive()) return;

        ComponentAccessor<ChunkStore> chunkAccessor = buffer.getExternalData().getWorld()
                .getChunkStore().getStore();

        HexContext ctx = construct.getHexContext().copy();
        ctx.UpdateAccessor(buffer);
        ctx.UpdateChunkAccessor(chunkAccessor);

        VolatilityTracker tracker = ctx.getVolatilityTracker();
        if (tracker != null) {
            tracker.setFailed(false);
            tracker.setFizzled(false);
        }

        if (injector != null) {
            injector.accept(ctx);
        }

        Executor.continueExecution(branchIds, ctx);
    }
}
