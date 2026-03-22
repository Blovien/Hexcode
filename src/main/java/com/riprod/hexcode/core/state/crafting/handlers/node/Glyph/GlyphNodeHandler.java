package com.riprod.hexcode.core.state.crafting.handlers.node.Glyph;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.utils.GlyphType;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hidden.utils.HiddenUtils;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.common.hover.utils.HoverableUtils;
import com.riprod.hexcode.core.common.utilities.component.DebugComponent;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.NodeComponent;
import com.riprod.hexcode.core.state.crafting.component.CraftingDataComponent;
import com.riprod.hexcode.core.state.crafting.constants.CraftingColors;
import com.riprod.hexcode.core.state.crafting.constants.NodeType;
import com.riprod.hexcode.core.state.crafting.handlers.DetailsHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeInterface;
import com.riprod.hexcode.core.state.crafting.handlers.node.Slot.SlotNodeHandler;
import com.riprod.hexcode.core.state.crafting.utils.CraftingGlyphRemover;
import com.riprod.hexcode.core.state.crafting.utils.LinkRenderer;
import com.riprod.hexcode.core.state.crafting.utils.CraftingDataUtil;

public class GlyphNodeHandler implements NodeInterface {
    public static final GlyphNodeHandler INSTANCE = new GlyphNodeHandler();

    private static final double NODE_SCALE = 0.05;

    public InteractionState enter(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef) {

        NodeComponent nodeComp = accessor.getComponent(node, NodeComponent.getComponentType());
        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());

        if (nodeComp == null || craftingComp == null)
            return InteractionState.Failed;

        Ref<EntityStore> effectRef = nodeComp.getParentEntity();
        if (effectRef == null || !effectRef.isValid())
            return InteractionState.Failed;

        GlyphComponent parentEffect = accessor.getComponent(
                effectRef, GlyphComponent.getComponentType());
        if (parentEffect == null)
            return InteractionState.Failed;

        HexComponent hexComp = accessor.getComponent(parentEffect.getHexRef(),
                HexComponent.getComponentType());
        if (hexComp == null)
            return InteractionState.Failed;

        // allow drag only if glyph is reachable from root
        String firstId = hexComp.getHex().getFirstGlyphId();
        boolean isFirstGlyph = firstId != null && parentEffect.getId().equals(firstId);
        boolean hasIncoming = !parentEffect.getGlyph().getPrevious().isEmpty();
        if (!isFirstGlyph && !hasIncoming)
            return InteractionState.Failed;

        craftingComp.setDraggingRef(node);
        return InteractionState.Finished;
    }

    public InteractionState tick(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef) {

        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());

        TransformComponent nodeTransform = accessor.getComponent(
                craftingComp.getDraggingRef(), TransformComponent.getComponentType());

        Transform look = TargetUtil.getLook(playerRef, accessor);
        Vector3d targetPoint = new Vector3d(
                look.getPosition().x + look.getDirection().x * 5,
                look.getPosition().y + look.getDirection().y * 5,
                look.getPosition().z + look.getDirection().z * 5);

        if (nodeTransform != null) {
            LinkRenderer.renderActiveLink(accessor, accessor.getExternalData().getWorld(),
                    nodeTransform.getPosition(), targetPoint,
                    CraftingColors.GLYPH_LINK);
        }
        return InteractionState.Finished;
    }

    public InteractionState exit(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef) {

        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());

        NodeComponent nodeComp = accessor.getComponent(nodeRef, NodeComponent.getComponentType());
        if (nodeComp == null) {
            craftingComp.setDraggingRef(null);
            craftingComp.setDragTickCount(0);
            return InteractionState.Failed;
        }

        Ref<EntityStore> dropTargetRef = craftingComp.getHoveredRef();

        if (dropTargetRef == null || !dropTargetRef.isValid()) {
            craftingComp.setDraggingRef(null);
            craftingComp.setDragTickCount(0);
            return InteractionState.Finished;
        }

        Ref<EntityStore> targetGlyphRef = HoverableUtils.getGlyphFromHoverable(accessor, dropTargetRef);

        Ref<EntityStore> sourceGlyphRef = nodeComp.getParentEntity();
        if (targetGlyphRef != null && targetGlyphRef.isValid()
                && sourceGlyphRef != null && sourceGlyphRef.isValid()
                && !targetGlyphRef.equals(sourceGlyphRef)) {
            GlyphComponent sourceEffect = accessor.getComponent(sourceGlyphRef,
                    GlyphComponent.getComponentType());
            GlyphComponent targetEffect = accessor.getComponent(targetGlyphRef,
                    GlyphComponent.getComponentType());
            if (sourceEffect != null && targetEffect != null) {
                sourceEffect.getGlyph().addNext(targetEffect.getId());
                targetEffect.getGlyph().addPrevious(sourceEffect.getId());

                NodeComponent sourceNode = accessor.getComponent(nodeRef, NodeComponent.getComponentType());
                Ref<EntityStore> targetNodeRef = targetEffect.getNodeRef();
                if (sourceNode != null && targetNodeRef != null && targetNodeRef.isValid()) {
                    NodeComponent targetNode = accessor.getComponent(targetNodeRef, NodeComponent.getComponentType());
                    if (targetNode != null) {
                        sourceNode.addOutgoingRef(targetNodeRef);
                        targetNode.addIncomingRef(nodeRef);
                    }
                }
            }
        }

        craftingComp.setDraggingRef(null);
        craftingComp.setDragTickCount(0);

        return InteractionState.Finished;
    }

    @Override
    public InteractionState ability(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            InteractionType inputType, Ref<EntityStore> playerRef) {

        if (inputType != InteractionType.Ability3) { // if not R - ignore
            return InteractionState.Failed;
        }

        NodeComponent nodeComp = accessor.getComponent(nodeRef, NodeComponent.getComponentType());
        if (nodeComp == null) {
            return InteractionState.Failed;
        }
        
        Ref<EntityStore> glyphRef = nodeComp.getParentEntity();
        GlyphComponent effectComp = accessor.getComponent(glyphRef, GlyphComponent.getComponentType());
        if (effectComp == null) {
            return InteractionState.Failed;
        }
        
        Glyph glyph = effectComp.getGlyph();
        
        // step 1: if glyph has input/output connections, clear those first
        boolean hasInputs = glyph.getInputs() != null && !glyph.getInputs().isEmpty();
        if (hasInputs) {
            Ref<EntityStore> hexRootRef = effectComp.getHexRef();
            HexComponent hexComp = accessor.getComponent(hexRootRef, HexComponent.getComponentType());
            if (hexComp != null) {
                CraftingGlyphRemover.clearInputOutputConnections(glyph, hexComp.getHex());
            }
        }

        List<Ref<EntityStore>> outgoingRefs = nodeComp.getOutgoingRefs();
        List<Ref<EntityStore>> incomingRefs = nodeComp.getIncomingRefs();
        boolean hasLinks = (outgoingRefs != null && !outgoingRefs.isEmpty())
                || (incomingRefs != null && !incomingRefs.isEmpty());

        // step 2: if glyph has graph links, disconnect those
        if (hasLinks) {
            removeOutgoingLinks(accessor, nodeComp, nodeRef, effectComp);
            removeIncomingLinks(accessor, nodeComp, nodeRef, effectComp);
            nodeComp.getOutgoingRefs().clear();
            nodeComp.getIncomingRefs().clear();
        }
        if (hasInputs || hasLinks) {
            return InteractionState.Finished;
        }

        this.despawn(accessor, glyphRef, nodeRef, playerRef, effectComp);
        return InteractionState.Finished;
    }

    private void removeOutgoingLinks(CommandBuffer<EntityStore> accessor, NodeComponent nodeComp,
            Ref<EntityStore> nodeRef,
            GlyphComponent effectComp) {

        nodeComp.getOutgoingRefs().forEach(outRef -> {

            NodeComponent outNodeComp = accessor.getComponent(outRef, NodeComponent.getComponentType());
            if (outNodeComp == null) {
                // error state - there should always be a node component on the outgoing refs of
                // an anchor node
                return;
            }
            Ref<EntityStore> outGlyphRef = outNodeComp.getParentEntity();

            if (!outGlyphRef.isValid() || outGlyphRef == null) {
                // error state - there should always be a valid parent entity ref on the
                // outgoing node components of an anchor node of the effect
                return;
            }

            GlyphComponent outEffectComp = accessor.getComponent(outGlyphRef, GlyphComponent.getComponentType());
            if (outEffectComp == null) {
                // error state - there should always be an effect component on the outgoing refs
                // of an anchor node
                return;
            }

            Glyph glyph = outEffectComp.getGlyph();

            if (glyph.getType() == GlyphType.Value) {
                // cannot connect a value glyph to the root. This is an error state.
                return;
            }

            // remove the links
            outNodeComp.removeIncomingRef(nodeRef);
            glyph.getPrevious().remove(effectComp.getId());
            effectComp.getGlyph().getNext().remove(outEffectComp.getId());
        });
    }

    private void removeIncomingLinks(CommandBuffer<EntityStore> accessor, NodeComponent nodeComp,
            Ref<EntityStore> nodeRef,
            GlyphComponent effectComp) {

        nodeComp.getIncomingRefs().forEach(outRef -> {

            NodeComponent outNodeComp = accessor.getComponent(outRef, NodeComponent.getComponentType());
            if (outNodeComp == null) {
                // error state - there should always be a node component on the outgoing refs of
                // an anchor node
                return;
            }
            Ref<EntityStore> outGlyphRef = outNodeComp.getParentEntity();

            if (!outGlyphRef.isValid() || outGlyphRef == null) {
                // error state - there should always be a valid parent entity ref on the
                // outgoing node components of an anchor node of the effect
                return;
            }

            GlyphComponent outEffectComp = accessor.getComponent(outGlyphRef, GlyphComponent.getComponentType());
            if (outEffectComp == null) {
                // error state - there should always be an effect component on the outgoing refs
                // of an anchor node
                return;
            }

            Glyph glyph = outEffectComp.getGlyph();

            if (glyph.getType() == GlyphType.Value) {
                // cannot connect a value glyph to the root. This is an error state.
                return;
            }

            // remove the links
            outNodeComp.removeOutgoingRef(nodeRef);
            glyph.getNext().remove(effectComp.getId());
            effectComp.getGlyph().getPrevious().remove(outEffectComp.getId());
        });
    }

    public Holder<EntityStore> spawnNode(CommandBuffer<EntityStore> accessor, Ref<EntityStore> parentRef,
            Vector3d rootPos,
            Ref<EntityStore> playerRef) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        HiddenUtils.addHiddenToHolder(accessor, holder, playerRef);

        Vector3d nodePos = new Vector3d(rootPos.x, rootPos.y, rootPos.z);
        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(nodePos, new Vector3f(0, 0, 0)));

        NodeComponent node = new NodeComponent(parentRef, NodeType.Glyph);

        holder.addComponent(NodeComponent.getComponentType(), node);

        Box nodeBox = new Box(-NODE_SCALE, -NODE_SCALE, -NODE_SCALE,
                NODE_SCALE, NODE_SCALE, NODE_SCALE);
        holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(nodeBox));

        holder.addComponent(UUIDComponent.getComponentType(),
                new UUIDComponent(UUID.randomUUID()));
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

        int networkId = accessor.getExternalData().takeNextNetworkId();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));

        // ModelAsset anchor = ModelAsset.getAssetMap().getAsset("Selection_Anchor");
        // Model model = Model.createScaledModel(anchor, 1.0f);

        // holder.addComponent(ModelComponent.getComponentType(), new
        // ModelComponent(model));
        // holder.addComponent(PersistentModel.getComponentType(), new
        // PersistentModel(model.toReference()));

        holder.addComponent(DebugComponent.getComponentType(),
                new DebugComponent(DebugShape.Sphere, CraftingColors.GLYPH_LINK,
                        NODE_SCALE * 2.5, 2.0f, playerRef));
        holder.addComponent(HoverableComponent.getComponentType(),
                new HoverableComponent(HoverableType.NODE));

        // mount the node to the parent entity so that it moves with it
        holder.addComponent(MountedComponent.getComponentType(),
                new MountedComponent(parentRef, Vector3f.ZERO, MountController.Minecart));

        return holder;
    }

    @Override
    public InteractionState click(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef) {

        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        CraftingDataComponent playerData = CraftingDataUtil.getPedestalData(accessor, playerRef);
        NodeComponent nodeComp = accessor.getComponent(node, NodeComponent.getComponentType());

        if (craftingComp == null || playerData == null || nodeComp == null) {
            return InteractionState.Failed;
        }

        Ref<EntityStore> effectRef = nodeComp.getParentEntity();
        if (effectRef == null || !effectRef.isValid()) {
            return InteractionState.Failed;
        }

        GlyphComponent effect = accessor.getComponent(effectRef,
                GlyphComponent.getComponentType());
        if (effect != null && effect.getGlyph().getType() != GlyphType.Value) { // hybrid and effect both open details
            if (DetailsHandler.isOpenFor(accessor, playerData.getSlotNodeRefs(), effectRef)) {
                SlotNodeHandler.INSTANCE.despawn(accessor, playerData);
                playerData.setSlotNodeRefs(null);
            } else {
                if (playerData.getSlotNodeRefs() != null && !playerData.getSlotNodeRefs().isEmpty()) {
                    SlotNodeHandler.INSTANCE.despawn(accessor, playerData);
                }
                List<Ref<EntityStore>> slotRefs = DetailsHandler.openDetails(
                        accessor, effectRef, playerRef);
                playerData.setSlotNodeRefs(slotRefs);
            }
        }

        return InteractionState.Finished;
    }

    @Override
    public void despawn(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {

        NodeComponent nodeComp = accessor.getComponent(nodeRef, NodeComponent.getComponentType());
        if (nodeComp == null) {
            return;
        }

        Ref<EntityStore> glyphRef = nodeComp.getParentEntity();
        GlyphComponent effectComp = accessor.getComponent(glyphRef, GlyphComponent.getComponentType());
        if (effectComp == null) {
            return;
        }

        Glyph glyph = effectComp.getGlyph();

        // step 1: if glyph has input/output connections, clear those first
        if (!glyph.getInputs().isEmpty() || !glyph.getOutputs().isEmpty()) {
            Ref<EntityStore> hexRootRef = effectComp.getHexRef();
            HexComponent hexComp = accessor.getComponent(hexRootRef, HexComponent.getComponentType());
            if (hexComp != null) {
                CraftingGlyphRemover.clearInputOutputConnections(glyph, hexComp.getHex());
            }
        }

        List<Ref<EntityStore>> outgoingRefs = nodeComp.getOutgoingRefs();
        List<Ref<EntityStore>> incomingRefs = nodeComp.getIncomingRefs();

        // step 2: if glyph has graph links, disconnect those
        if ((outgoingRefs != null && !outgoingRefs.isEmpty())
                || (incomingRefs != null && !incomingRefs.isEmpty())) {
            removeOutgoingLinks(accessor, nodeComp, nodeRef, effectComp);
            removeIncomingLinks(accessor, nodeComp, nodeRef, effectComp);
            nodeComp.getOutgoingRefs().clear();
            nodeComp.getIncomingRefs().clear();
        }

        this.despawn(accessor, glyphRef, nodeRef, playerRef, effectComp);
    }

    public void despawn(CommandBuffer<EntityStore> accessor, Ref<EntityStore> glyphRef, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef, GlyphComponent effectComp) {

        Glyph glyph = effectComp.getGlyph();

        // step 3: nothing connected, remove the glyph
        Ref<EntityStore> hexRootRef = effectComp.getHexRef();
        HexComponent hexComp = accessor.getComponent(hexRootRef, HexComponent.getComponentType());
        if (hexComp != null) {
            CraftingGlyphRemover.clearInputOutputConnections(glyph, hexComp.getHex());
            hexComp.getHex().remove(effectComp.getId());
            hexComp.removeChildGlyph(effectComp.getId());
        }

        // remove the slots as cleanup

        accessor.tryRemoveEntity(glyphRef, RemoveReason.REMOVE);
        accessor.tryRemoveEntity(nodeRef, RemoveReason.REMOVE);
    }

    @Override
    public void hover(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
        DebugComponent debug = accessor.getComponent(nodeRef, DebugComponent.getComponentType());
        if (debug == null)
            return;
        debug.setScaleMultiplier(1.3f);
        debug.setIntervalMultiplier(0.25f);
        debug.setFadeMultiplier(0.25f);
        debug.setTimer(0);
    }

    @Override
    public void unhover(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
        DebugComponent debug = accessor.getComponent(nodeRef, DebugComponent.getComponentType());
        if (debug == null)
            return;
        debug.resetScaleMultiplier();
        debug.resetFadeMultipler();
        debug.resetIntervalMultiplier();
    }
}
