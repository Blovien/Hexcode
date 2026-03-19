package com.riprod.hexcode.core.state.crafting.handlers.node;

import java.util.EnumMap;
import java.util.List;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.state.crafting.component.NodeComponent;
import com.riprod.hexcode.core.state.crafting.constants.NodeType;
import com.riprod.hexcode.core.state.crafting.handlers.node.Anchor.AnchorNodeHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.Container.ContainerNodeHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.Effect.EffectNodeHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.Glyph.GlyphNodeHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.Slot.SlotNodeHandler;
import com.riprod.hexcode.utils.VfxUtil;

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
        InteractionState result = handler.enter(accessor, nodeRef, playerRef);
        if (result == InteractionState.Finished) {
            playSound(NodeSounds.DRAG, accessor, nodeRef);
        }
        return result;
    }

    public static InteractionState click(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
        NodeInterface handler = getHandler(accessor, nodeRef);
        if (handler == null)
            return InteractionState.Failed;
        InteractionState result = handler.click(accessor, nodeRef, playerRef);
        if (result == InteractionState.Finished) {
            playSound(NodeSounds.CLICK, accessor, nodeRef);
        }
        return result;
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

        // check if this node has outgoing links before exit (to detect new link creation)
        NodeComponent nodeComp = accessor.getComponent(nodeRef, NodeComponent.getComponentType());
        int linksBefore = 0;
        if (nodeComp != null) {
            List<Ref<EntityStore>> outgoing = nodeComp.getOutgoingRefs();
            linksBefore = outgoing != null ? outgoing.size() : 0;
        }

        InteractionState result = handler.exit(accessor, nodeRef, playerRef);

        if (result == InteractionState.Finished) {
            // re-read to check if a link was added
            nodeComp = accessor.getComponent(nodeRef, NodeComponent.getComponentType());
            int linksAfter = 0;
            if (nodeComp != null) {
                List<Ref<EntityStore>> outgoing = nodeComp.getOutgoingRefs();
                linksAfter = outgoing != null ? outgoing.size() : 0;
            }

            if (linksAfter > linksBefore) {
                playSound(NodeSounds.LINK, accessor, nodeRef);
            } else {
                playSound(NodeSounds.DROP, accessor, nodeRef);
            }
        }
        return result;
    }

    public static InteractionState ability(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            InteractionType inputType, Ref<EntityStore> playerRef) {
        NodeInterface handler = getHandler(accessor, nodeRef);
        if (handler == null)
            return InteractionState.Failed;

        // check state before dispatch to determine if this will unlink or delete
        boolean hadLinks = false;
        NodeComponent nodeComp = accessor.getComponent(nodeRef, NodeComponent.getComponentType());
        if (nodeComp != null) {
            List<Ref<EntityStore>> outgoing = nodeComp.getOutgoingRefs();
            List<Ref<EntityStore>> incoming = nodeComp.getIncomingRefs();
            hadLinks = (outgoing != null && !outgoing.isEmpty())
                    || (incoming != null && !incoming.isEmpty());
        }

        InteractionState result = handler.ability(accessor, nodeRef, inputType, playerRef);

        if (result == InteractionState.Finished) {
            if (hadLinks) {
                playSound(NodeSounds.UNLINK, accessor, nodeRef);
            } else {
                playSound(NodeSounds.DELETE, accessor, nodeRef);
            }
        }
        return result;
    }

    public static void hover(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef) {
        NodeInterface handler = getHandler(accessor, nodeRef);
        if (handler == null)
            return;
        handler.hover(accessor, nodeRef, playerRef);
        playSound(NodeSounds.HOVER, accessor, nodeRef);
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

    private static void playSound(String soundId, CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> nodeRef) {
        TransformComponent transform = accessor.getComponent(nodeRef, TransformComponent.getComponentType());
        if (transform != null) {
            VfxUtil.sound(soundId, transform.getPosition(), accessor);
        }
    }
}
