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
        // get the hex context ready
        HexContext hexContext = new HexContext(casterRef, accessor, spell);
        // execute the root glyph
        ExecutionContext executionContext = new ExecutionContext();

        UUIDComponent uuidComponent = accessor.getComponent(casterRef, UUIDComponent.getComponentType());

        EntityVar casterVar = new EntityVar(uuidComponent.getUuid(), casterRef);

        List<SpellVar> rootVars = List.of(casterVar);

        // set the first var as the player ref
        executionContext.setVariable(0, rootVars);

        // setup the first glyph
        executionContext.setCurrentNode(hexContext.spellGraph.rootId);

        continueExecution(hexContext, executionContext);
    }

    public static void continueExecution(HexContext hexContext, ExecutionContext executionContext) {
        UUID rootNode = hexContext.spellGraph.rootId;

        Glyph node = hexContext.spellGraph.nodes.get(rootNode);

        GlyphHandler handler = GlyphRegistry.get(node.getGlyphId());

        if (handler == null) {
            LOGGER.atSevere().log("No handler found for glyph id %s, skipping execution of this node", node.getGlyphId());
            return;
        }

        List<UUID> nextNodes = node.getNext();

        if (nextNodes.isEmpty()) {
            LOGGER.atInfo().log("No next nodes to execute after node %s", rootNode);
            return;
        }

        if (nextNodes.size() == 1) {
            // send to child without cloning context since no branching will occur
            executionContext.setCurrentNode(nextNodes.get(0));
            handler.execute(hexContext, executionContext);
        }

        for (UUID nextNodeId : nextNodes) {
            // clone for neighbors
            ExecutionContext newContext = executionContext.copy();
            newContext.setCurrentNode(nextNodeId);
            handler.execute(hexContext, newContext);
        }
    }
}
