package com.riprod.hexcode.core.state.crafting.handlers.node;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

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
import com.riprod.hexcode.core.state.crafting.component.PedestalDataComponent;
import com.riprod.hexcode.core.state.crafting.constants.NodeType;
import com.riprod.hexcode.core.state.crafting.handlers.DetailsHandler;
import com.riprod.hexcode.core.state.crafting.utils.LinkRenderer;
import com.riprod.hexcode.core.state.crafting.utils.PedestalDataUtil;

public class SlotNodeHandler implements NodeInterface {
    public static final SlotNodeHandler INSTANCE = new SlotNodeHandler();

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

    public InteractionState drag(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
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
                    new Vector3f(0.0f, 0.8f, 0.8f));
        }
        return InteractionState.Finished;
    }

    public InteractionState drop(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
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
                && sourceGlyphRef.isValid()
                && !targetGlyphRef.equals(sourceGlyphRef)) {
            GlyphComponent sourceEffect = accessor.getComponent(sourceGlyphRef,
                    GlyphComponent.getComponentType());
            GlyphComponent targetEffect = accessor.getComponent(targetGlyphRef,
                    GlyphComponent.getComponentType());
            if (sourceEffect != null && targetEffect != null) {
                sourceEffect.getGlyph().addNext(targetEffect.getId());
                targetEffect.getGlyph().addPrevious(sourceEffect.getId());
            }
        }

        craftingComp.setDraggingRef(null);
        craftingComp.setDragTickCount(0);

        return InteractionState.Finished;
    }

    public InteractionState ability(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef) {

        NodeComponent nodeComp = accessor.getComponent(nodeRef, NodeComponent.getComponentType());
        if (nodeComp == null) {
            return InteractionState.Failed;
        }

        List<Ref<EntityStore>> outgoingRefs = nodeComp.getOutgoingRefs();
        List<Ref<EntityStore>> incomingRefs = nodeComp.getIncomingRefs();
        Ref<EntityStore> glyphRef = nodeComp.getParentEntity();

        GlyphComponent effectComp = accessor.getComponent(glyphRef, GlyphComponent.getComponentType());
        if (effectComp == null) {
            return InteractionState.Failed; // always a hex component on the hex ref
        }

        // check if there is nothing connected to the glyph, in which case we can just
        // remove it without worrying about breaking the hex
        if ((outgoingRefs == null || outgoingRefs.isEmpty()) && (incomingRefs == null || incomingRefs.isEmpty())) {
            // remove from root hex first
            Ref<EntityStore> hexRootRef = effectComp.getHexRef();
            HexComponent hexComp = accessor.getComponent(hexRootRef, HexComponent.getComponentType());
            if (hexComp != null) {
                hexComp.getHex().remove(effectComp.getId());
            }

            // then remove the glyph entity itself
            accessor.removeEntity(glyphRef, RemoveReason.REMOVE);
            accessor.removeEntity(nodeRef, RemoveReason.REMOVE);
            return InteractionState.Finished;
        }

        removeOutgoingLinks(accessor, nodeComp, nodeRef, effectComp);
        removeIncomingLinks(accessor, nodeComp, nodeRef, effectComp);

        // remove all outgoing refs from the node, effectively disconnecting the hex
        // from all glyphs and making it an empty root node
        nodeComp.getOutgoingRefs().clear();
        nodeComp.getIncomingRefs().clear();
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

    public Ref<EntityStore> spawnNode(CommandBuffer<EntityStore> accessor, Ref<EntityStore> parentRef, Vector3d rootPos,
            Ref<EntityStore> playerRef) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        

        // BELOW IS ALL JUNK - SHOULD BE MIGRATED

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
                new DebugComponent(DebugShape.Sphere, new Vector3f(0.8f, 0.8f, 0.2f),
                        NODE_SCALE * 2.5, 2.0f, playerRef));
        holder.addComponent(HoverableComponent.getComponentType(),
                new HoverableComponent(HoverableType.NODE));

        Ref<EntityStore> nodeRef = accessor.addEntity(holder, AddReason.SPAWN);
        return nodeRef;
    }

    @Override
    public InteractionState click(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef) {

        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(accessor, playerRef);
        NodeComponent nodeComp = accessor.getComponent(node, NodeComponent.getComponentType());

        if (craftingComp == null || playerData == null || nodeComp == null) {
            return InteractionState.Failed;
        }

        Ref<EntityStore> effectRef = nodeComp.getParentEntity();
        if (effectRef == null || !effectRef.isValid()) {
            return InteractionState.Failed;
        }

        // first, delete the old slots
        if (playerData.getSlotNodeRefs() != null && !playerData.getSlotNodeRefs().isEmpty()) {
            DetailsHandler.closeDetails(accessor, playerData.getSlotNodeRefs());
        }

        GlyphComponent effect = accessor.getComponent(effectRef,
                GlyphComponent.getComponentType());
        if (effect != null) {
            List<Ref<EntityStore>> slotRefs = DetailsHandler.openDetails(
                    accessor, effectRef, playerRef);

            playerData.setSlotNodeRefs(slotRefs);
        }

        return InteractionState.Finished;
    }
}
