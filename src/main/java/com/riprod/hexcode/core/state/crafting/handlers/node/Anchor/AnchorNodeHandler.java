package com.riprod.hexcode.core.state.crafting.handlers.node.Anchor;

import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hidden.utils.HiddenUtils;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.common.hover.utils.HoverableUtils;
import com.riprod.hexcode.core.common.utilities.component.DebugComponent;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.core.state.crafting.utils.LinkRenderer;
import com.riprod.hexcode.core.state.crafting.component.NodeComponent;
import com.riprod.hexcode.core.state.crafting.constants.CraftingColors;
import com.riprod.hexcode.core.state.crafting.constants.NodeType;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeInterface;
import com.riprod.hexcode.core.state.crafting.component.CraftingData;
import com.riprod.hexcode.core.state.crafting.utils.CraftingDataUtil;
import com.riprod.hexcode.core.state.crafting.handlers.node.Glyph.GlyphNodeHandler;

/**
 * ParentRef = Root hex ref
 */
public class AnchorNodeHandler implements NodeInterface {

    private static final double ROOT_NODE_SCALE = 0.2;

    public static final AnchorNodeHandler INSTANCE = new AnchorNodeHandler();


    public InteractionState enter(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef) {

        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null)
            return InteractionState.Failed;
        craftingComp.setDraggingRef(nodeRef);
        return InteractionState.NotFinished;
    }

    public InteractionState tick(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef) {

        TransformComponent nodeTransform = accessor.getComponent(nodeRef, TransformComponent.getComponentType());

        Transform look = TargetUtil.getLook(playerRef, accessor);
        Vector3d targetPoint = new Vector3d(
                look.getPosition().x + look.getDirection().x * 5,
                look.getPosition().y + look.getDirection().y * 5,
                look.getPosition().z + look.getDirection().z * 5);

        if (nodeTransform != null) {
            LinkRenderer.renderActiveLink(accessor, accessor.getExternalData().getWorld(),
                    nodeTransform.getPosition(), targetPoint,
                    CraftingColors.ANCHOR);
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

        Ref<EntityStore> hexRootRef = nodeComp.getParentEntity();

        Ref<EntityStore> dropTargetRef = craftingComp.getHoveredRef();

        if (dropTargetRef == null || !dropTargetRef.isValid()) {
            craftingComp.setDraggingRef(null);
            craftingComp.setDragTickCount(0);
            return InteractionState.Finished;
        }

        Ref<EntityStore> targetGlyphRef = HoverableUtils.getGlyphFromHoverable(accessor, dropTargetRef);

        if (targetGlyphRef == null || !targetGlyphRef.isValid()) {
            craftingComp.setDraggingRef(null);
            craftingComp.setDragTickCount(0);
            return InteractionState.Failed;
        }

        // clear existing connections from the root to glyphs if there is a valid hex
        // root ref, effectively making the root an empty node
        if (nodeComp.getOutgoingRefs() != null) {
            nodeComp.getOutgoingRefs().clear();
        }

        HexComponent hexComp = accessor.getComponent(hexRootRef,
                HexComponent.getComponentType());
        GlyphComponent targetEffect = accessor.getComponent(targetGlyphRef,
                GlyphComponent.getComponentType());
        if (hexComp != null && targetEffect != null) {
            hexComp.getHex().setFirstGlyphId(targetEffect.getId());
        }

        craftingComp.setDraggingRef(null);
        craftingComp.setDragTickCount(0);
        return InteractionState.Finished;
    }

    public InteractionState ability(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            InteractionType type, Ref<EntityStore> playerRef) {

        NodeComponent nodeComp = accessor.getComponent(nodeRef, NodeComponent.getComponentType());
        if (nodeComp == null) {
            return InteractionState.Failed;
        }

        List<Ref<EntityStore>> outgoingRefs = nodeComp.getOutgoingRefs();
        Ref<EntityStore> hexRef = nodeComp.getParentEntity();

        // no hex ref - no connections. Just return finished if there are no outgoing
        // connections, otherwise fail due to unexpected state
        if (hexRef == null || !hexRef.isValid()) {
            if (outgoingRefs.isEmpty()) {
                return InteractionState.Finished;
            } else {
                return InteractionState.Failed; // there should always be an outgoing ref if there is a valid hex ref
            }
        }

        HexComponent hexComp = accessor.getComponent(hexRef, HexComponent.getComponentType());
        if (hexComp == null) {
            return InteractionState.Failed; // always a hex component on the hex ref
        }
        hexComp.getHex().setFirstGlyphId(null);

        // remove all outgoing refs from the node, effectively disconnecting the hex
        // from all glyphs and making it an empty root node
        nodeComp.getOutgoingRefs().clear();

        return InteractionState.Finished;
    }

    public Ref<EntityStore> spawnNode(CommandBuffer<EntityStore> accessor, Hex coreHex, Ref<EntityStore> parentRef,
            Vector3d rootPos,
            Ref<EntityStore> playerRef) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(rootPos, new Vector3f(0, 0, 0)));

        NodeComponent node = new NodeComponent(parentRef, NodeType.Anchor);

        holder.addComponent(NodeComponent.getComponentType(), node);

        Box nodeBox = new Box(-ROOT_NODE_SCALE, -ROOT_NODE_SCALE, -ROOT_NODE_SCALE,
                ROOT_NODE_SCALE, ROOT_NODE_SCALE, ROOT_NODE_SCALE);
        holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(nodeBox));

        holder.addComponent(UUIDComponent.getComponentType(),
                new UUIDComponent(UUID.randomUUID()));
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

        int networkId = accessor.getExternalData().takeNextNetworkId();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));

        holder.addComponent(DebugComponent.getComponentType(),
                new DebugComponent(DebugShape.Sphere, CraftingColors.ANCHOR,
                        ROOT_NODE_SCALE * 2.5, 2.0f));
        holder.addComponent(HoverableComponent.getComponentType(),
                new HoverableComponent(HoverableType.NODE));

        // spawn the children of the hex
        HexComponent hexComp = accessor.getComponent(parentRef, HexComponent.getComponentType());
        Ref<EntityStore> nodeGlyph = accessor.addEntity(holder, AddReason.SPAWN);

        if (hexComp == null) {
            return nodeGlyph;
        }

        List<Glyph> children = hexComp.getHex().getGlyphs();

        // spawn the original glyphs from the original hex
        for (Glyph glyph : children) {

            Vector3f offset = glyph.getPosition();
            Vector3d worldPos = new Vector3d(
                    rootPos.x + offset.x,
                    rootPos.y + offset.y,
                    rootPos.z + offset.z);

            GlyphComponent glyphComp = new GlyphComponent(glyph);

            Ref<EntityStore> glyphRef = GlyphNodeHandler.INSTANCE.spawnNode(accessor, nodeGlyph, worldPos,
                    playerRef, glyphComp, parentRef);

            hexComp.addChildGlyphRef(glyph.getId(), glyphRef);
        }

        accessor.putComponent(parentRef, HexComponent.getComponentType(), hexComp);
        return nodeGlyph;
    }

    @Override
    public InteractionState click(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef) {

        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        craftingComp.setDraggingRef(null);
        craftingComp.setDragTickCount(0);
        return InteractionState.Finished;
    }

    @Override
    public void despawn(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'despawn'");
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
