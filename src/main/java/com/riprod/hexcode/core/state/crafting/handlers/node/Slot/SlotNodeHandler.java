package com.riprod.hexcode.core.state.crafting.handlers.node.Slot;

import java.util.UUID;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
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
import com.riprod.hexcode.core.common.glyphs.utils.GlyphType;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hidden.utils.HiddenUtils;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.common.hover.utils.HoverableUtils;
import com.riprod.hexcode.core.common.utilities.component.DebugComponent;
import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.NodeComponent;
import com.riprod.hexcode.core.state.crafting.component.SlotComponent;
import com.riprod.hexcode.core.state.crafting.constants.CraftingColors;
import com.riprod.hexcode.core.state.crafting.constants.NodeType;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeInterface;
import com.riprod.hexcode.core.state.crafting.utils.LinkRenderer;

public class SlotNodeHandler implements NodeInterface {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final SlotNodeHandler INSTANCE = new SlotNodeHandler();

    private static final double NODE_SCALE = 0.2;

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
                    nodeTransform.getPosition(), targetPoint, CraftingColors.INPUT);
        }
        return InteractionState.Finished;
    }

    public InteractionState exit(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef) {

        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());

        Ref<EntityStore> draggedRef = craftingComp.getDraggingRef();
        if (draggedRef == null || !draggedRef.isValid()) {
            craftingComp.setDraggingRef(null);
            craftingComp.setDragTickCount(0);
            return InteractionState.Failed;
        }

        NodeComponent draggedNodeComp = accessor.getComponent(draggedRef, NodeComponent.getComponentType());
        if (draggedNodeComp == null) {
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
        if (targetGlyphRef == null || !targetGlyphRef.isValid()) {
            craftingComp.setDraggingRef(null);
            craftingComp.setDragTickCount(0);
            return InteractionState.Finished;
        }

        GlyphComponent targetGlyphComp = accessor.getComponent(targetGlyphRef,
                GlyphComponent.getComponentType());
        if (targetGlyphComp == null || targetGlyphComp.getGlyph().getType() != GlyphType.Value) {
            craftingComp.setDraggingRef(null);
            craftingComp.setDragTickCount(0);
            return InteractionState.Finished;
        }

        SlotComponent slotComp = accessor.getComponent(draggedRef, SlotComponent.getComponentType());
        if (slotComp == null) {
            craftingComp.setDraggingRef(null);
            craftingComp.setDragTickCount(0);
            return InteractionState.Failed;
        }

        Ref<EntityStore> parentEffectRef = draggedNodeComp.getParentEntity();
        GlyphComponent parentEffect = accessor.getComponent(parentEffectRef,
                GlyphComponent.getComponentType());
        if (parentEffect != null) {
            parentEffect.getGlyph().setInput(slotComp.getSlotKey(), targetGlyphComp.getId());
            LOGGER.atInfo().log("slot: connected slot '%s' on %s to value glyph %s (id=%s)",
                    slotComp.getSlotKey(), parentEffect.getGlyphId(),
                    targetGlyphComp.getGlyphId(), targetGlyphComp.getId());
        }

        craftingComp.setDraggingRef(null);
        craftingComp.setDragTickCount(0);

        return InteractionState.Finished;
    }

    public InteractionState ability3(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef) {

        NodeComponent nodeComp = accessor.getComponent(nodeRef, NodeComponent.getComponentType());
        if (nodeComp == null)
            return InteractionState.Failed;

        SlotComponent slotComp = accessor.getComponent(nodeRef, SlotComponent.getComponentType());
        if (slotComp == null)
            return InteractionState.Failed;

        Ref<EntityStore> parentRef = nodeComp.getParentEntity();
        GlyphComponent parentEffect = accessor.getComponent(parentRef, GlyphComponent.getComponentType());
        if (parentEffect == null)
            return InteractionState.Failed;

        parentEffect.getGlyph().removeInput(slotComp.getSlotKey());

        return InteractionState.Finished;
    }

    @Override
    public InteractionState click(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef) {
        return InteractionState.Finished;
    }

    public Ref<EntityStore> spawnNode(CommandBuffer<EntityStore> accessor, Ref<EntityStore> parentRef,
            Vector3d rootPos, Ref<EntityStore> playerRef) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        HiddenUtils.addHiddenToHolder(accessor, holder, playerRef);

        Vector3d nodePos = new Vector3d(rootPos.x, rootPos.y, rootPos.z);
        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(nodePos, new Vector3f(0, 0, 0)));

        NodeComponent node = new NodeComponent(parentRef, NodeType.Slot);
        holder.addComponent(NodeComponent.getComponentType(), node);

        Box nodeBox = new Box(-NODE_SCALE, -NODE_SCALE, -NODE_SCALE,
                NODE_SCALE, NODE_SCALE, NODE_SCALE);
        holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(nodeBox));

        holder.addComponent(UUIDComponent.getComponentType(),
                new UUIDComponent(UUID.randomUUID()));
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

        int networkId = accessor.getExternalData().takeNextNetworkId();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));

        holder.addComponent(DebugComponent.getComponentType(),
                new DebugComponent(DebugShape.Cube, CraftingColors.INPUT, NODE_SCALE, 2.0f, playerRef));
        holder.addComponent(HoverableComponent.getComponentType(),
                new HoverableComponent(HoverableType.NODE));

        Ref<EntityStore> nodeRef = accessor.addEntity(holder, AddReason.SPAWN);
        return nodeRef;
    }
}
