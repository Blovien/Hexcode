package com.riprod.hexcode.core.common.trigger;

import javax.annotation.Nullable;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.trigger.component.TriggerComponent;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexRoot;
import com.riprod.hexcode.core.state.execution.component.HexSignal;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;

public class TriggerUtils {

    private TriggerUtils() {
    }

    public static void fireFirstBranch(TriggerComponent trigger, @Nullable HexSignal signal,
            Ref<EntityStore> entityRef, CommandBuffer<EntityStore> buffer) {
        if (signal == null || signal.getPrimary() == null) return;

        HexSignal.SignalEntry entry = signal.getPrimary();
        Ref<EntityStore> hexEntityRef = entry.getHexEntityRef();
        if (hexEntityRef == null || !hexEntityRef.isValid()) return;

        RootGlyph execComp = buffer.getComponent(hexEntityRef, RootGlyph.getComponentType());
        if (execComp == null) return;

        HexRoot root = execComp.getRoot();
        if (root == null || !root.isAlive()) return;

        ComponentAccessor<ChunkStore> chunkAccessor = buffer.getExternalData().getWorld()
                .getChunkStore().getStore();

        HexContext ctx = entry.getHexContext().copy();
        ctx.UpdateAccessor(buffer);
        ctx.UpdateChunkAccessor(chunkAccessor);

        for (var outputEntry : entry.getOutputSlots().entrySet()) {
            if (entityRef.isValid()) {
                UUIDComponent uuid = buffer.getComponent(entityRef, UUIDComponent.getComponentType());
                if (uuid != null) {
                    ctx.setVariable(outputEntry.getValue(), new EntityVar(uuid.getUuid(), entityRef));
                }
            }
        }

        Executor.continueExecution(trigger.getFirstBranchIds(), ctx);
    }

    public static void decrementWaiters(@Nullable HexSignal signal, CommandBuffer<EntityStore> buffer) {
        if (signal == null) return;
        signal.decrementAllWaiters(buffer);
    }

    public static void removeEntity(Ref<EntityStore> ref, CommandBuffer<EntityStore> buffer) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        buffer.removeEntity(ref, holder, RemoveReason.REMOVE);
    }
}
