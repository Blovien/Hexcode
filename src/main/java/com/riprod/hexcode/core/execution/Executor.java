package com.riprod.hexcode.core.execution;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.Glyph;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execution.component.ExecutionComponent;
import com.riprod.hexcode.core.execution.component.PendingContinue;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.registry.GlyphRegistry;
import com.riprod.hexcode.core.glyphs.variables.EntityVar;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

public class Executor {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int MAX_EXECUTION_DEPTH = 10000;

    private Executor() {
    }

    public static void beginExecution(@Nonnull HexContext hexContext) {
        ExecutionContext executionContext = new ExecutionContext();

        UUIDComponent uuidComponent = hexContext.accessor.getComponent(
                hexContext.casterRef, UUIDComponent.getComponentType());
        EntityVar casterVar = new EntityVar(EntityVar.createRef(uuidComponent.getUuid(), hexContext.casterRef));
        executionContext.setVariable(1, casterVar);
        executionContext.setCurrentNode(hexContext.spellGraph.rootId);

        Glyph rootNode = hexContext.spellGraph.nodes.get(hexContext.spellGraph.rootId);
        GlyphHandler handler = GlyphRegistry.get(rootNode.getGlyphId());
        if (handler == null) {
            LOGGER.atSevere().log("no handler found for root glyph %s", rootNode.getGlyphId());
            return;
        }

        handler.execute(rootNode, hexContext, executionContext);
    }

    public static void continueExecution(HexContext hexContext, ExecutionContext executionContext) {
        if (executionContext.incrementDepth() > MAX_EXECUTION_DEPTH) {
            LOGGER.atWarning().log("max execution depth reached, aborting spell");
            return;
        }

        UUID currentNodeId = executionContext.getCurrentNode();
        Glyph currentNode = hexContext.spellGraph.nodes.get(currentNodeId);
        List<UUID> nextNodes = currentNode.getNext();

        if (nextNodes.isEmpty()) {
            return;
        }

        if (nextNodes.size() == 1) {
            UUID nextId = nextNodes.get(0);
            Glyph nextNode = hexContext.spellGraph.nodes.get(nextId);
            GlyphHandler nextHandler = GlyphRegistry.get(nextNode.getGlyphId());
            if (nextHandler == null) {
                LOGGER.atSevere().log("no handler found for glyph %s, skipping", nextNode.getGlyphId());
                return;
            }
            executionContext.setCurrentNode(nextId);
            nextHandler.execute(nextNode, hexContext, executionContext);
            return;
        }

        for (UUID nextNodeId : nextNodes) {
            Glyph nextNode = hexContext.spellGraph.nodes.get(nextNodeId);
            GlyphHandler nextHandler = GlyphRegistry.get(nextNode.getGlyphId());
            if (nextHandler == null) {
                LOGGER.atSevere().log("no handler found for glyph %s, skipping", nextNode.getGlyphId());
                continue;
            }
            ExecutionContext branchedContext = executionContext.copy();
            branchedContext.setCurrentNode(nextNodeId);
            nextHandler.execute(nextNode, hexContext, branchedContext);
        }
    }

    public static void delayContinuation(HexContext hexContext, ExecutionContext executionContext, int delayTicks) {
        ExecutionComponent execComp = hexContext.accessor.getComponent(
                hexContext.root.getRootEntityRef(), ExecutionComponent.getComponentType());
        PendingContinue pending = new PendingContinue(
                executionContext.getCurrentNode(),
                executionContext.copy(),
                delayTicks);
        execComp.addPendingContinue(pending);
    }
}
