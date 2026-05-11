package com.riprod.hexcode.core.state.execution;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.GlyphFizzleEvent;
import com.riprod.hexcode.api.event.HexCastEvent;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.component.Slot;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphRegistry;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.registry.HexStyleAsset;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;

import java.util.Arrays;
import java.util.List;

public class HexExecuter {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private HexExecuter() {
    }

    /**
     * Unified entry point for any cast (block imbuement, item-held proc, armor proc, staff,
     * chat command, future continuation/resume). Injects runtime accessors onto the context
     * and fires HexCastEvent for listeners/cancellation. The HexCastEventSystem handler picks
     * up the event, runs CastGate.admit, then calls runPostGate to do the mana/glyph/continue
     * work.
     *
     * <p>Callers no longer construct and dispatch HexCastEvent manually — they build a
     * HexContext and hand it to this method. The buffer is the EntityStore CommandBuffer
     * the cast will execute against (live cast path); cross-store callers (ChunkStore tick)
     * must hop via world.execute first to obtain an EntityStore buffer.
     */
    public static void cast(HexContext context, CommandBuffer<EntityStore> buffer) {
        ComponentAccessor<ChunkStore> chunkAccessor = buffer.getExternalData().getWorld().getChunkStore().getStore();
        context.UpdateAccessor(buffer);
        context.UpdateChunkAccessor(chunkAccessor);
        if (context.getStyle() == null) context.setStyle(HexStyleAsset.empty());
        buffer.invoke(new HexCastEvent(context));
    }

    /**
     * Post-gate execution: called by HexCastEventSystem after CastGate.admit returns true.
     * Runs the mana check, default-variable resolution, and dispatches to continueExecution
     * starting from the hex's first glyph.
     */
    public static void runPostGate(HexContext context, CommandBuffer<EntityStore> buffer) {
        // ensure accessors are present (idempotent — already set by cast(), but
        // defensive for any callers that fire HexCastEvent without going through cast()).
        if (context.getAccessor() == null) {
            context.UpdateAccessor(buffer);
            context.UpdateChunkAccessor(buffer.getExternalData().getWorld().getChunkStore().getStore());
        }

        if (context.getHexRoot() == null) {
            HytaleServer.get().getEventBus().dispatchFor(GlyphFizzleEvent.class)
                    .dispatch(new GlyphFizzleEvent(null, GlyphFizzleEvent.Reason.ERROR, context));
            return;
        }

        if (!context.getHexRoot().tryConsumeMana(context.getManaCost(), buffer)) {
            HytaleServer.get().getEventBus().dispatchFor(GlyphFizzleEvent.class)
                    .dispatch(new GlyphFizzleEvent(null, GlyphFizzleEvent.Reason.INSUFFICIENT_MANA, context));
            return;
        }

        Hex hex = context.getHex();
        if (hex == null) {
            HytaleServer.get().getEventBus().dispatchFor(GlyphFizzleEvent.class)
                    .dispatch(new GlyphFizzleEvent(null, GlyphFizzleEvent.Reason.ERROR, context));
            return;
        }
        String startingGlyph = hex.getFirstGlyphId();
        if (startingGlyph == null) {
            HytaleServer.get().getEventBus().dispatchFor(GlyphFizzleEvent.class)
                    .dispatch(new GlyphFizzleEvent(null, GlyphFizzleEvent.Reason.ERROR, context));
            return;
        }

        HexVar defaultVar = context.getDefaultVariable();
        if (defaultVar == null) {
            defaultVar = context.getHexRoot().getRootVar(context);
        }
        if (defaultVar != null) {
            context.setVariable(Glyph.DEFAULT_SLOT, defaultVar);
        }

        continueExecution(List.of(startingGlyph), context);
    }

    public static void continueFromSlot(Glyph glyph, String slotKey, HexContext hexContext) {
        Slot slot = glyph.getSlot(slotKey);
        if (slot == null)
            return;
        String[] links = slot.getLinks();
        if (links.length == 0)
            return;
        continueExecution(Arrays.asList(links), hexContext);
    }

    public static void fail(HexContext hexContext) {
        fail(null, hexContext, GlyphFizzleEvent.Reason.VOLATILITY_DEPLETED);
    }

    public static void fail(Glyph glyph, HexContext hexContext) {
        fail(glyph, hexContext, GlyphFizzleEvent.Reason.VOLATILITY_DEPLETED);
    }

    public static void fail(Glyph glyph, HexContext hexContext, GlyphFizzleEvent.Reason reason) {
        fail(glyph, hexContext, reason, null, null);
    }

    public static void fail(Glyph glyph, HexContext hexContext, GlyphFizzleEvent.Reason reason,
            String detail) {
        fail(glyph, hexContext, reason, detail, null);
    }

    public static void fail(Glyph glyph, HexContext hexContext, GlyphFizzleEvent.Reason reason,
            Throwable cause) {
        fail(glyph, hexContext, reason, null, cause);
    }

    public static void fail(Glyph glyph, HexContext hexContext, GlyphFizzleEvent.Reason reason,
            String detail, Throwable cause) {
        HytaleServer.get().getEventBus().dispatchFor(GlyphFizzleEvent.class)
                .dispatch(new GlyphFizzleEvent(glyph, reason, hexContext, detail, cause));
    }

    public static void continueExecution(List<String> nextGlyphs, HexContext hexContext) {
        if (nextGlyphs.isEmpty()) {
            return;
        }

        boolean multiBranch = nextGlyphs.size() > 1;

        for (String nextNodeId : nextGlyphs) {
            try {
                executeNode(nextNodeId, multiBranch ? hexContext.branch() : hexContext);
            } catch (Exception e) {
                LOGGER.atSevere().log("error executing glyph %s: %s", nextNodeId, e.getMessage());
            }
        }
    }

    private static void executeNode(String nodeId, HexContext hexContext) {
        Glyph nextNode = hexContext.getGlyph(nodeId);

        VolatilityTracker tracker = hexContext.getVolatilityTracker();
        if (tracker != null && tracker.getRemainingBudget() <= 0) {
            fail(nextNode, hexContext, GlyphFizzleEvent.Reason.VOLATILITY_DEPLETED);
            return;
        }

        if (nextNode == null) {
            fail(null, hexContext);
            return;
        }
        GlyphHandler nextHandler = GlyphRegistry.get(nextNode.getGlyphId());
        if (nextHandler == null) {
            fail(nextNode, hexContext);
            return;
        }
        try {
            if (!nextHandler.consumeVolatility(nextNode, hexContext)) {
                fail(nextNode, hexContext);
                return;
            }
            if (tracker != null) {
                tracker.incrementGlyphUsage(nextNode.getId());
            }
            nextHandler.execute(nextNode, hexContext);
        } catch (Exception e) {
            fail(nextNode, hexContext, GlyphFizzleEvent.Reason.HANDLER_FAILED, e);
        }
    }
}
