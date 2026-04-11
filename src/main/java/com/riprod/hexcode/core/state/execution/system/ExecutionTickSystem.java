package com.riprod.hexcode.core.state.execution.system;

import java.util.List;

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
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexRoot;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;

public class ExecutionTickSystem extends EntityTickingSystem<EntityStore> {
    private static final com.hypixel.hytale.logger.HytaleLogger LOGGER =
            com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    @Override
    public Query<EntityStore> getQuery() {
        return RootGlyph.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        try {
            RootGlyph rootGlyph = chunk.getComponent(index, RootGlyph.getComponentType());

            HexRoot root = rootGlyph.getRoot();
            if (root == null || !root.isAlive()) {
                return;
            }

            ComponentAccessor<ChunkStore> chunkAccessor = buffer.getExternalData().getWorld().getChunkStore().getStore();

            rootGlyph.pruneDeadDependents();

            if (rootGlyph.needsInitialExecution()) {
                rootGlyph.setNeedsInitialExecution(false);

                HexContext hexContext = new HexContext(root, buffer, chunkAccessor, rootGlyph.getHex());
                Executor.beginExecution(List.of(rootGlyph.getHex().getFirstGlyphId()), hexContext);

                if (!rootGlyph.hasDependents()) {
                    Executor.unregisterActiveHex(hexContext);
                    Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
                    buffer.removeEntity(ref, holder, RemoveReason.REMOVE);
                }
                return;
            }

            if (!rootGlyph.hasDependents()) {
                if (rootGlyph.getOriginContext() != null) {
                    Executor.unregisterActiveHex(rootGlyph.getOriginContext());
                }
                Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
                buffer.removeEntity(ref, holder, RemoveReason.REMOVE);
            }
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] ExecutionTickSystem failed: %s", e.getMessage());
            try {
                Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
                buffer.removeEntity(ref, holder, RemoveReason.REMOVE);
            } catch (Exception cleanup) {
                LOGGER.atSevere().log("[hexcode] ExecutionTickSystem cleanup failed: %s", cleanup.getMessage());
            }
        }
    }
}
