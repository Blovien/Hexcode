package com.riprod.hexcode.core.execute;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.components.ExecutionContext;
import com.riprod.hexcode.components.Glyph;
import com.riprod.hexcode.components.HexContext;
import com.riprod.hexcode.core.execute.component.HexGraph;
import com.riprod.hexcode.core.glyphs.component.GlyphHandler;
import com.riprod.hexcode.core.glyphs.registry.GlyphRegistry;
import com.riprod.hexcode.core.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.glyphs.variables.SpellVar;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

public class Executor {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private Executor() {
    }

    public static void beginExecution(@Nonnull HexGraph spell, @Nonnull Ref<EntityStore> casterRef,
            @Nonnull ComponentAccessor<EntityStore> accessor) {
        HexContext hexContext = new HexContext(casterRef, accessor, spell);
        ExecutionContext executionContext = new ExecutionContext();

        UUIDComponent uuidComponent = accessor.getComponent(casterRef, UUIDComponent.getComponentType());
        EntityVar casterVar = new EntityVar(uuidComponent.getUuid(), casterRef);
        executionContext.setVariable(0, List.of(casterVar));
        executionContext.setCurrentNode(hexContext.spellGraph.rootId);

        Glyph rootNode = hexContext.spellGraph.nodes.get(hexContext.spellGraph.rootId);
        GlyphHandler handler = GlyphRegistry.get(rootNode.getGlyphId());
        if (handler == null) {
            LOGGER.atSevere().log("no handler found for root glyph %s", rootNode.getGlyphId());
            return;
        }

        handler.execute(hexContext, executionContext);
    }

    public static void continueExecution(HexContext hexContext, ExecutionContext executionContext) {
        UUID currentNodeId = executionContext.getCurrentNode();
        Glyph currentNode = hexContext.spellGraph.nodes.get(currentNodeId);
        List<UUID> nextNodes = currentNode.getNext();

        LOGGER.atInfo().log("Continuing execution from node %s with %d next nodes", currentNodeId, nextNodes.size());

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
            nextHandler.execute(hexContext, executionContext);
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
            nextHandler.execute(hexContext, branchedContext);
        }
    }
}
