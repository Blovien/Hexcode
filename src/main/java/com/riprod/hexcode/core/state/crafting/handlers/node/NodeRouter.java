package com.riprod.hexcode.core.state.crafting.handlers.node;

import java.util.EnumMap;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.crafting.component.NodeComponent;
import com.riprod.hexcode.core.state.crafting.constants.NodeType;
import com.riprod.hexcode.core.state.crafting.handlers.node.Anchor.AnchorNodeHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.Container.ContainerNodeHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.Effect.EffectNodeHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.Glyph.GlyphNodeHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.Slot.SlotNodeHandler;

public class NodeRouter {

    private static final EnumMap<NodeType, NodeInterface> HANDLERS = new EnumMap<>(NodeType.class);
    static {
        HANDLERS.put(NodeType.Anchor, AnchorNodeHandler.INSTANCE);
        HANDLERS.put(NodeType.Container, ContainerNodeHandler.INSTANCE);
        HANDLERS.put(NodeType.Effect, EffectNodeHandler.INSTANCE);
        HANDLERS.put(NodeType.Glyph, GlyphNodeHandler.INSTANCE);
        HANDLERS.put(NodeType.Slot, SlotNodeHandler.INSTANCE);
    }

    public static NodeInterface getHandler(NodeType type) {
        return HANDLERS.get(type);
    }

    public static InteractionState enter(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
        NodeInterface handler = getHandler(accessor, nodeRef);
        if (handler == null)
            return InteractionState.Failed;
        return handler.enter(accessor, nodeRef, playerRef);
    }

    public static InteractionState click(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
        NodeInterface handler = getHandler(accessor, nodeRef);
        if (handler == null)
            return InteractionState.Failed;
        return handler.click(accessor, nodeRef, playerRef);
    }

    public static InteractionState drag(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef) {
        NodeInterface handler = getHandler(accessor, nodeRef);
        if (handler == null)
            return InteractionState.Failed;
        return handler.tick(accessor, nodeRef, playerRef);
    }

    public static InteractionState exit(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef) {
        NodeInterface handler = getHandler(accessor, nodeRef);
        if (handler == null)
            return InteractionState.Failed;
        return handler.exit(accessor, nodeRef, playerRef);
    }

    public static InteractionState ability(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            InteractionType inputType, Ref<EntityStore> playerRef) {
        NodeInterface handler = getHandler(accessor, nodeRef);
        if (handler == null)
            return InteractionState.Failed;
        return handler.ability(accessor, nodeRef, inputType, playerRef);
    }

    public static void hover(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef) {
        NodeInterface handler = getHandler(accessor, nodeRef);
        if (handler == null)
            return;
        handler.hover(accessor, nodeRef, playerRef);
    }

    public static void unhover(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef) {
        NodeInterface handler = getHandler(accessor, nodeRef);
        if (handler == null)
            return;
        handler.unhover(accessor, nodeRef, playerRef);
    }

    private static NodeInterface getHandler(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef) {
        NodeComponent nodeComp = accessor.getComponent(nodeRef, NodeComponent.getComponentType());
        if (nodeComp == null)
            return null;
        NodeInterface handler = HANDLERS.get(nodeComp.getNodeType());
        return handler;
    }
}
