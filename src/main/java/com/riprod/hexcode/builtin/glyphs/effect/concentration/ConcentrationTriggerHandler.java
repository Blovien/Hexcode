package com.riprod.hexcode.builtin.glyphs.effect.concentration;

import java.util.List;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.builtin.glyphs.effect.concentration.component.ConcentrationTriggerComponent;
import com.riprod.hexcode.core.common.trigger.TriggerHandler;
import com.riprod.hexcode.core.common.trigger.component.TriggerComponent;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexSignal;
import com.riprod.hexcode.core.state.execution.component.HexSignal.SignalEntry;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.core.state.execution.component.HexRoot;
import com.riprod.hexcode.core.state.execution.component.HexcasterExecutionComponent;

public class ConcentrationTriggerHandler implements TriggerHandler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public boolean onTick(float dt, Ref<EntityStore> entityRef,
            ArchetypeChunk<EntityStore> chunk, int index,
            TriggerComponent trigger, HexSignal signal,
            CommandBuffer<EntityStore> buffer) {

        ConcentrationTriggerComponent marker = chunk.getComponent(index,
                ConcentrationTriggerComponent.getComponentType());
        if (marker == null) return true;

        Ref<EntityStore> casterRef = marker.getCasterRef();
        if (casterRef == null || !casterRef.isValid()) return true;

        HexcasterExecutionComponent execComp = buffer.getComponent(
                casterRef, HexcasterExecutionComponent.getComponentType());
        if (execComp == null || !execComp.isHoldingPrimary()) {
            return true;
        }
        return false;
    }

    @Override
    public void onCleanup(Ref<EntityStore> entityRef, TriggerComponent trigger,
            HexSignal signal, CommandBuffer<EntityStore> buffer) {
        if (signal == null) return;

        ComponentAccessor<ChunkStore> chunkAccessor = buffer.getExternalData().getWorld()
                .getChunkStore().getStore();

        for (SignalEntry entry : signal.getEntries()) {
            HexContext ctx = entry.getHexContext();
            if (ctx == null) continue;

            List<String> injectedBranches = entry.getNextGlyphIds();
            if (injectedBranches != null && !injectedBranches.isEmpty()) {
                Ref<EntityStore> hexRef = entry.getHexEntityRef();
                if (hexRef != null && hexRef.isValid()) {
                    RootGlyph rootGlyph = buffer.getComponent(hexRef, RootGlyph.getComponentType());
                    HexRoot root = rootGlyph != null ? rootGlyph.getRoot() : null;
                    if (root != null && root.isAlive()) {
                        try {
                            HexContext fireCtx = ctx.copy();
                            fireCtx.UpdateAccessor(buffer);
                            fireCtx.UpdateChunkAccessor(chunkAccessor);
                            Executor.continueExecution(injectedBranches, fireCtx);
                        } catch (Exception e) {
                            LOGGER.atWarning().log("concentration: hijacked cleanup fire failed: %s",
                                    e.getMessage());
                        }
                    }
                }
            }

            Executor.fail(ctx);
        }
    }
}
