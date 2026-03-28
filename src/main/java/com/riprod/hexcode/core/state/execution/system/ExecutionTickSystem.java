package com.riprod.hexcode.core.state.execution.system;

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
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexRoot;
import com.riprod.hexcode.core.state.execution.component.PendingContinue;
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

            if (rootGlyph.needsInitialExecution()) {
                rootGlyph.setNeedsInitialExecution(false);

                HexContext hexContext = new HexContext(root, buffer, chunkAccessor, rootGlyph.getHex());
                Executor.beginExecution(List.of(rootGlyph.getHex().getFirstGlyphId()), hexContext);

                if (!rootGlyph.hasPendingContinues() && rootGlyph.getExternalWaiters() <= 0) {
                    Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
                    buffer.removeEntity(ref, holder, RemoveReason.REMOVE);
                }
                return;
            }

            List<PendingContinue> ready = null;
            Iterator<PendingContinue> it = rootGlyph.getPendingContinues().iterator();
            while (it.hasNext()) {
                PendingContinue pending = it.next();
                pending.tick(dt);

                if (pending.isReady()) {
                    it.remove();
                    if (ready == null) ready = new java.util.ArrayList<>();
                    ready.add(pending);
                }
            }

            if (ready != null) {
                for (PendingContinue pending : ready) {
                    Executor.continueExecution(pending.getGlyphIds(), pending.getExecutionContext());
                }
            }

            if (!rootGlyph.hasPendingContinues() && rootGlyph.getExternalWaiters() <= 0) {
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
