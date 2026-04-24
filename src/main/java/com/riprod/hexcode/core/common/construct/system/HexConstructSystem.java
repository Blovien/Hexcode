package com.riprod.hexcode.core.common.construct.system;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.construct.handler.ConstructHandler;
import com.riprod.hexcode.core.common.construct.component.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.component.HexEffectsComponent;
import com.riprod.hexcode.core.common.construct.component.HexStatus;
import com.riprod.hexcode.core.common.construct.registry.ConstructRegistry;
import com.riprod.hexcode.core.common.construct.state.ConstructState;

public class HexConstructSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public Query<EntityStore> getQuery() {
        return HexEffectsComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {
        try {
            HexEffectsComponent construct = chunk.getComponent(index, HexEffectsComponent.getComponentType());
            Ref<EntityStore> entityRef = chunk.getReferenceTo(index);

            if (construct == null || construct.getEffects().isEmpty()) {
                buffer.tryRemoveComponent(entityRef, HexEffectsComponent.getComponentType());
                return;
            }

            Iterator<Map.Entry<UUID, HexStatus<?>>> it = construct.getEffects().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, HexStatus<?>> entry = it.next();
                UUID effectId = entry.getKey();
                HexStatus<?> status = entry.getValue();
                ConstructHandler<?> handler = ConstructRegistry.get(status.getHandlerId());
                ConstructTickContext ctx = new ConstructTickContext(chunk, index, buffer, entityRef);

                if (handler == null) {
                    LOGGER.atSevere().log("no construct handler for: %s", status.getHandlerId());
                    cleanupWithoutHandler(status, ctx);
                    it.remove();
                    continue;
                }

                boolean removed = tickEffect(effectId, handler, status, ctx, dt, it);
                if (removed) continue;

                if (status.isKillRequested()
                        || (status.getHexContext() != null
                                && status.getHexContext().getVolatilityTracker().getRemainingBudget() <= 0)) {
                    cleanup(handler, status, ctx);
                    it.remove();
                }
            }
        } catch (Exception e) {
            LOGGER.atSevere().log("HexConstructSystem failed: %s", e.getMessage());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean tickEffect(UUID effectId, ConstructHandler<?> handler, HexStatus<?> status,
            ConstructTickContext ctx, float dt, Iterator<Map.Entry<UUID, HexStatus<?>>> it) {
        try {
            ConstructHandler raw = handler;
            HexStatus rawStatus = status;

            if (!status.hasFiredImmediate()) {
                Object initial = raw.createInitialState(rawStatus, ctx);
                if (initial instanceof ConstructState cs) {
                    rawStatus.setState(cs);
                }
                raw.onFirstTick(rawStatus, ctx);
                status.markImmediateFired();
            } else {
                boolean kill = raw.onTick(dt, rawStatus, ctx);
                if (kill) {
                    cleanup(handler, status, ctx);
                    it.remove();
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.atSevere().log("error ticking construct effect %s: %s", effectId, e.getMessage());
        }
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void cleanup(ConstructHandler<?> handler, HexStatus<?> status, ConstructTickContext ctx) {
        if (handler != null) {
            try {
                ConstructHandler raw = handler;
                HexStatus rawStatus = status;
                raw.onCleanup(rawStatus, ctx);
            } catch (Exception e) {
                LOGGER.atWarning().log("construct handler cleanup failed: %s", e.getMessage());
            }
        }
    }

    private void cleanupWithoutHandler(HexStatus<?> status, ConstructTickContext ctx) {
        // nothing to invoke; caller removes the map entry
    }
}
