package com.riprod.hexcode.core.execution.system;

import java.util.Iterator;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.Executor;
import com.riprod.hexcode.core.execution.component.ExecutionComponent;
import com.riprod.hexcode.core.execution.component.HexRoot;
import com.riprod.hexcode.core.execution.component.PendingContinue;

public class ExecutionTickSystem extends EntityTickingSystem<EntityStore> {

    @Override
    public Query<EntityStore> getQuery() {
        return ExecutionComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        ExecutionComponent execComp = chunk.getComponent(index, ExecutionComponent.getComponentType());
        Ref<EntityStore> ref = chunk.getReferenceTo(index);

        HexRoot root = execComp.getRoot();
        if (root == null || !root.isAlive()) {
            return;
        }

        if (execComp.needsInitialExecution()) {
            execComp.setNeedsInitialExecution(false);
            HexContext hexContext = new HexContext(root, buffer, execComp.getSpellGraph());
            Executor.beginExecution(hexContext);

            if (!execComp.hasPendingContinues()) {
                Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
                buffer.removeEntity(ref, holder, RemoveReason.REMOVE);
            }
            return;
        }

        Iterator<PendingContinue> it = execComp.getPendingContinues().iterator();
        while (it.hasNext()) {
            PendingContinue pending = it.next();
            pending.tick();

            if (pending.isReady()) {
                it.remove();
                HexContext hexContext = new HexContext(root, buffer, execComp.getSpellGraph());
                Executor.continueExecution(hexContext, pending.getExecutionContext());
            }
        }

        if (!execComp.hasPendingContinues()) {
            Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
            buffer.removeEntity(ref, holder, RemoveReason.REMOVE);
        }
    }
}
