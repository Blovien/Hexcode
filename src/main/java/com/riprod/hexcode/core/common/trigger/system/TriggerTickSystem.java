package com.riprod.hexcode.core.common.trigger.system;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.trigger.TriggerHandler;
import com.riprod.hexcode.core.common.trigger.TriggerRegistry;
import com.riprod.hexcode.core.common.trigger.TriggerUtils;
import com.riprod.hexcode.core.common.trigger.component.TriggerComponent;
import com.riprod.hexcode.core.state.execution.component.HexSignal;

public class TriggerTickSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public Query<EntityStore> getQuery() {
        return TriggerComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {
        try {
            TriggerComponent trigger = chunk.getComponent(index, TriggerComponent.getComponentType());
            Ref<EntityStore> entityRef = chunk.getReferenceTo(index);

            if (trigger == null) {
                TriggerUtils.removeEntity(entityRef, buffer);
                return;
            }

            TriggerHandler handler = TriggerRegistry.get(trigger.getHandlerId());
            if (handler == null) {
                LOGGER.atSevere().log("[hexcode] no trigger handler for: %s", trigger.getHandlerId());
                TriggerUtils.removeEntity(entityRef, buffer);
                return;
            }

            HexSignal signal = buffer.getComponent(entityRef, HexSignal.getComponentType());

            if (trigger.getRemainingLifetime() >= 0) {
                trigger.setRemainingLifetime(trigger.getRemainingLifetime() - dt);
                if (trigger.getRemainingLifetime() <= 0) {
                    handler.onCleanup(entityRef, trigger, signal, buffer);
                    TriggerUtils.decrementWaiters(signal, buffer);
                    TriggerUtils.removeEntity(entityRef, buffer);
                    return;
                }
            }

            if (!trigger.firedFirstBranch() && trigger.getFirstBranchIds() != null) {
                TriggerUtils.fireFirstBranch(trigger, signal, entityRef, buffer);
                trigger.markFirstBranchFired();
                handler.onFirstTick(entityRef, trigger, signal, buffer);
            }

            boolean killRequested = handler.onTick(dt, entityRef, chunk, index, trigger, signal, buffer);

            if (killRequested || trigger.isKillRequested()) {
                handler.onCleanup(entityRef, trigger, signal, buffer);
                TriggerUtils.decrementWaiters(signal, buffer);
                TriggerUtils.removeEntity(entityRef, buffer);
            }
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] TriggerTickSystem failed: %s", e.getMessage());
        }
    }
}
