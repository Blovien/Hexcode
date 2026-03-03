package com.riprod.hexcode.core.state.execution;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphRegistry;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.PendingContinue;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

public class Executor {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private Executor() {
    }

    public static void beginExecution(List<String> nextGlyphs, @Nonnull HexContext hexContext) {

        UUIDComponent uuidComponent = hexContext.getAccessor().getComponent(
                hexContext.getCasterRef(), UUIDComponent.getComponentType());
        EntityVar casterVar = new EntityVar(EntityVar.createRef(uuidComponent.getUuid(), hexContext.getCasterRef()));
        
        hexContext.setVariable(1, casterVar);
        
        continueExecution(nextGlyphs, hexContext);
    }

    public static void continueExecution(List<String> nextGlyphs, HexContext hexContext) {

        if (nextGlyphs.isEmpty()) {
            return;
        }

        if (nextGlyphs.size() == 1) {
            String nextId = nextGlyphs.get(0);
            Glyph nextNode = hexContext.getGlyph(nextId);
            GlyphHandler nextHandler = GlyphRegistry.get(nextNode.getGlyphId());
            if (nextHandler == null) {
                LOGGER.atSevere().log("no handler found for glyph %s, skipping", nextNode.getGlyphId());
                return;
            }
            try {
                nextHandler.execute(nextNode, hexContext);
            } catch (Exception e) {
                LOGGER.atSevere().log("error executing glyph %s: %s", nextNode.getGlyphId(), e.getMessage());
            }
            return;
        }

        for (String nextNodeId : nextGlyphs) {
            Glyph nextNode = hexContext.getGlyph(nextNodeId);
            GlyphHandler nextHandler = GlyphRegistry.get(nextNode.getGlyphId());
            if (nextHandler == null) {
                LOGGER.atSevere().log("no handler found for glyph %s, skipping", nextNode.getGlyphId());
                continue;
            }
            HexContext copiedBranch = hexContext.copy();
            try {
                nextHandler.execute(nextNode, copiedBranch);
            } catch (Exception e) {
                LOGGER.atSevere().log("error executing glyph %s: %s", nextNode.getGlyphId(), e.getMessage());
            }
        }
    }

    public static void delayContinuation(List<String> nextGlyphs, HexContext hexContext, int delayTicks) {
        RootGlyph rootGlyph = hexContext.getAccessor().getComponent(
                hexContext.getRoot().getRootEntityRef(), RootGlyph.getComponentType());

        PendingContinue pending = new PendingContinue(
                nextGlyphs,
                hexContext,
                delayTicks);
        rootGlyph.addPendingContinue(pending);
    }
}
