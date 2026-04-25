package com.riprod.hexcode.core.common.construct.handler;

import java.util.List;

import com.riprod.hexcode.core.common.construct.component.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.component.HexStatus;
import com.riprod.hexcode.core.common.construct.state.ConstructState;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;

public interface ConstructHandler<S extends ConstructState> {

    default S createInitialState(HexStatus<S> status, ConstructTickContext ctx) {
        return null;
    }

    default void onFirstTick(HexStatus<S> status, ConstructTickContext ctx) {
    }

    default boolean onTick(float dt, HexStatus<S> status, ConstructTickContext ctx) {
        return !drainSustain(dt, status);
    }

    default void onCleanup(HexStatus<S> status, ConstructTickContext ctx) {
    }

    // natural-end path: fires the pending chain, runs end visuals, despawns carriers.
    // construct system routes here on natural expiry; Resonate/Interfere call directly.
    default void onEnd(HexStatus<S> status, ConstructTickContext ctx) {
        onCleanup(status, ctx);
    }

    // early-termination path: cleanup without firing the chain.
    // construct system routes here on killRequested or budget exhaustion.
    default void onAbort(HexStatus<S> status, ConstructTickContext ctx) {
        onCleanup(status, ctx);
    }

    // pending chain accessors for splice operations (Resonate / Interfere only).
    // handlers that don't manage a chain leave defaults (empty list / no-op).
    default List<String> getPendingNextGlyphIds(HexStatus<S> status) {
        return List.of();
    }

    default void setPendingNextGlyphIds(HexStatus<S> status, List<String> ids) {
    }

    // pulls drainPerSecond from the triggering glyph's asset and bills the tracker.
    // single source of truth for sustain drain — replaces hardcoded dt*0.15f in handlers.
    // returns false when the tracker is depleted (dispelled or naturally drained); the
    // caller should propagate by returning true from onTick to trigger onCleanup.
    default boolean drainSustain(float dt, HexStatus<S> status) {
        VolatilityTracker tracker = status.getHexContext().getVolatilityTracker();
        if (tracker == null) return true;
        float rate = resolveDrainRate(status);
        if (rate > 0f) {
            if (!tracker.consumeVolatility(dt * rate)) return false;
        }
        return tracker.getRemainingBudget() > 0f;
    }

    static float resolveDrainRate(HexStatus<?> status) {
        Glyph trigger = status.getTriggeringGlyph();
        if (trigger == null) return 0.15f;
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(trigger.getGlyphId());
        if (asset == null) return 0.15f;
        return asset.getVolatility().getDrainPerSecond();
    }
}
