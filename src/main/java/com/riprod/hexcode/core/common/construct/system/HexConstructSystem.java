package com.riprod.hexcode.core.common.construct.system;

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
            Map<UUID, HexStatus> effects = construct.getEffects();

            if (construct == null || effects.isEmpty()) {
                buffer.tryRemoveComponent(entityRef, HexEffectsComponent.getComponentType());
                return;
            }

            for (Map.Entry<UUID, HexStatus> entry : effects.entrySet()) {
                UUID effectId = entry.getKey();
                HexStatus status = entry.getValue();
                ConstructHandler handler = ConstructRegistry.get(status.getHandlerId());
                ConstructTickContext ctx = new ConstructTickContext(chunk, index, buffer, entityRef);

                if (handler == null) {
                    LOGGER.atSevere().log("no construct handler for: %s", status.getHandlerId());
                    cleanup(construct, status, handler, ctx);
                    return;
                }

                tickEffect(effectId, handler, status, construct, ctx, dt);
            }
        } catch (Exception e) {
            LOGGER.atSevere().log("HexConstructSystem failed: %s", e.getMessage());
        }
    }

    private void tickEffect(UUID effectId, ConstructHandler handler, HexStatus status, HexEffectsComponent component, ConstructTickContext ctx, float dt) {
        try {            

            if (!status.hasFiredImmediate()) {
                handler.onFirstTick(status, ctx);
            } else {
                boolean kill = handler.onTick(dt, status, ctx);
                if (kill) {
                    cleanup(component, status, handler, ctx);
                    return;
                }
            }

            if (status.isKillRequested()) {
                cleanup(component, status, handler, ctx);
            }

            // cleanup once volatility is depleted
            if (status.getHexContext() != null && status.getHexContext().getVolatilityTracker().getRemainingBudget() <= 0) {
                cleanup(component, status, handler, ctx);
            }

        } catch (Exception e) {
            LOGGER.atSevere().log("error ticking construct effect %s: %s", effectId, e.getMessage());
        }
    }

    private void cleanup(HexEffectsComponent construct, HexStatus status, ConstructHandler handler,
            ConstructTickContext ctx) {

        if (handler != null) {
            try {
                handler.onCleanup(status, ctx);
            } catch (Exception e) {
                LOGGER.atWarning().log("construct handler cleanup failed: %s", e.getMessage());
            }
        }

        construct.removeEffect(status.getConstructId());
    }
}
