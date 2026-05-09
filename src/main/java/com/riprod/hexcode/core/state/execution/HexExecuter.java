package com.riprod.hexcode.core.state.execution;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.api.event.GlyphFizzleEvent;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.component.Slot;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphRegistry;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.VolatilityTracker;
import com.riprod.hexcode.core.state.execution.events.CastingEventData;

import java.util.Arrays;
import java.util.List;

public class HexExecuter {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private HexExecuter() {
    }

    public static void cast(CommandBuffer<EntityStore> buffer, CastingEventData castingData) {

        ComponentAccessor<ChunkStore> chunkAccessor = buffer.getExternalData().getWorld().getChunkStore()
                .getStore();

        HexContext hexContext = new HexContext(buffer, chunkAccessor, castingData);

        if (!castingData.getHexRoot().tryConsumeMana(castingData.getManaCost(), buffer)) {
            HytaleServer.get().getEventBus().dispatchFor(GlyphFizzleEvent.class)
                    .dispatch(new GlyphFizzleEvent(null, GlyphFizzleEvent.Reason.INSUFFICIENT_MANA, hexContext));
            return;
        }

        Hex hex = castingData.getHex();
        String startingGlyph = hex.getFirstGlyphId();
        if (startingGlyph == null) {
            HytaleServer.get().getEventBus().dispatchFor(GlyphFizzleEvent.class)
                    .dispatch(new GlyphFizzleEvent(null, GlyphFizzleEvent.Reason.ERROR, hexContext));
            return;
        }

        HexVar defaultVar = castingData.getDefaultVariable();
        if (defaultVar != null) {
            hexContext.setVariable(Glyph.DEFAULT_SLOT, defaultVar);
        } else {
            Ref<EntityStore> targetRef = castingData.getTargetRef();
            UUIDComponent uuidComponent = hexContext.getAccessor().getComponent(
                    targetRef, UUIDComponent.getComponentType());
            if (uuidComponent != null) {
                EntityVar targetVar = new EntityVar(
                        EntityVar.createRef(uuidComponent.getUuid(), targetRef));
                hexContext.setVariable(Glyph.DEFAULT_SLOT, targetVar);
            }
        }

        continueExecution(List.of(startingGlyph), hexContext);
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
