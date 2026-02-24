package com.riprod.hexcode.core.execution.system;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.execution.Compiler;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.execution.component.RootGlyph;
import com.riprod.hexcode.core.glyphs.component.Glyph;
import com.riprod.hexcode.core.execution.component.HexContext;
import com.riprod.hexcode.core.execution.component.HexRoot;
import com.riprod.hexcode.core.execution.component.PendingContinue;

public class ExecutionTickSystem extends EntityTickingSystem<EntityStore> {

    @Override
    public Query<EntityStore> getQuery() {
        return RootGlyph.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        RootGlyph rootGlyph = chunk.getComponent(index, RootGlyph.getComponentType());
        Ref<EntityStore> ref = chunk.getReferenceTo(index);

        HexRoot root = rootGlyph.getRoot();
        if (root == null || !root.isAlive()) {
            return;
        }

        ComponentAccessor<ChunkStore> chunkAccessor = buffer.getExternalData().getWorld().getChunkStore().getStore();

        if (rootGlyph.needsInitialExecution()) {
            rootGlyph.setNeedsInitialExecution(false);

            // Compile the glyphComponent
            Map<UUID, Glyph> compiledHex = Compiler.compile(rootGlyph.getGlyphComponent());

            HexContext hexContext = new HexContext(root, buffer, chunkAccessor, compiledHex);
            Executor.beginExecution(List.of(rootGlyph.getGlyphComponent().getId()), hexContext);

            if (!rootGlyph.hasPendingContinues() && rootGlyph.getExternalWaiters() <= 0) {
                Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
                buffer.removeEntity(ref, holder, RemoveReason.REMOVE);
            }
            return;
        }

        Iterator<PendingContinue> it = rootGlyph.getPendingContinues().iterator();
        while (it.hasNext()) {
            PendingContinue pending = it.next();
            pending.tick();

            if (pending.isReady()) {
                it.remove();
                Executor.continueExecution(pending.getGlyphIds(), pending.getExecutionContext());
            }
        }

        if (!rootGlyph.hasPendingContinues() && rootGlyph.getExternalWaiters() <= 0) {
            Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
            buffer.removeEntity(ref, holder, RemoveReason.REMOVE);
        }
    }
}
