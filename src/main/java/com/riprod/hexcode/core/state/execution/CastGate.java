package com.riprod.hexcode.core.state.execution;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.execution.component.HexcasterIdleComponent;
import com.riprod.hexcode.core.state.execution.component.PlayerHexRoot;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.core.state.execution.events.CastingEventData;

// the central pre-execute policy gate. every cast that goes through HexCastEvent
// passes through here; per-cast policy flags on CastingEventData decide whether
// the player charge cap, decay accumulation, and tracker eviction apply.
public final class CastGate {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private CastGate() {
    }

    public static boolean admit(@Nonnull CommandBuffer<EntityStore> buffer, @Nonnull CastingEventData castData) {
        // non-player roots (block, npc, system) skip the player-scoped gate entirely.
        if (!(castData.getHexRoot() instanceof PlayerHexRoot playerRoot)) {
            return true;
        }
        Ref<EntityStore> casterRef = playerRoot.getSourceRef();
        if (casterRef == null || !casterRef.isValid()) return true;

        HexcasterIdleComponent idle = buffer.getComponent(casterRef, HexcasterIdleComponent.getComponentType());
        if (idle == null) return true;

        VolatilityTracker tracker = castData.getVolatilityTracker();
        if (tracker == null) return true;

        String slotKey = castData.getCastSlotKey();
        tracker.setSlotKey(slotKey);

        if (slotKey == null) {
            // staff path: enforce maxCharges cap, decay-adjust budget, accumulate decay.
            int max = (int) playerRoot.resolveMaxMagicCharges(buffer);
            if (max <= 0) {
                sendNoSlotsMessage(buffer, casterRef);
                return false;
            }
            while (idle.getActiveCount() >= max) {
                idle.evictOldest();
            }
            float volMax = playerRoot.resolveVolatility(buffer);
            float startingBudget = Math.max(0f, volMax - idle.getCumulativeDecay());
            tracker.setBudget(startingBudget);
            tracker.setStartingBudget(startingBudget);
            idle.advanceCast(castData.getCastDecayRate(), volMax);
        } else {
            // slot-bound path: one cast per slot key. fizzle any prior cast on
            // this slot; skip cap, decay-subtraction, and decay-accumulation.
            idle.fizzleSlot(slotKey);
        }

        idle.registerActiveTracker(tracker);
        return true;
    }

    private static void sendNoSlotsMessage(CommandBuffer<EntityStore> buffer, Ref<EntityStore> casterRef) {
        try {
            PlayerRef pr = buffer.getComponent(casterRef, PlayerRef.getComponentType());
            if (pr != null) pr.sendMessage(Message.raw("no spell slots available"));
        } catch (Exception e) {
            LOGGER.atWarning().log("CastGate noSlots message failed: %s", e.getMessage());
        }
    }
}
