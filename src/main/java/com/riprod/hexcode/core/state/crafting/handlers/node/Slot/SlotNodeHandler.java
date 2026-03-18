package com.riprod.hexcode.core.state.crafting.handlers.node.Slot;

import java.util.List;
import java.util.UUID;

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
import com.riprod.hexcode.core.common.glyphs.utils.GlyphSlotType;
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
import com.riprod.hexcode.core.state.crafting.component.CraftingDataComponent;
import com.riprod.hexcode.core.state.crafting.component.SlotComponent;
import com.riprod.hexcode.core.state.crafting.constants.CraftingColors;
import com.riprod.hexcode.core.state.crafting.constants.NodeType;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeInterface;
import com.riprod.hexcode.core.state.crafting.utils.LinkRenderer;
import com.riprod.hexcode.core.state.crafting.utils.CraftingDataUtil;

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

        NodeComponent draggedNodeComp = accessor.getComponent(nodeRef, NodeComponent.getComponentType());
        if (draggedNodeComp == null) {
            craftingComp.setDraggingRef(null);
            craftingComp.setDragTickCount(0);
            return InteractionState.Failed;
        }

        // hovered always ignores the dragged entity
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

        SlotComponent slotComp = accessor.getComponent(nodeRef, SlotComponent.getComponentType());
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

    @Override
    public InteractionState ability(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            InteractionType inputType, Ref<EntityStore> playerRef) {

        if (inputType != InteractionType.Ability3) { // if not R - ignore
            return InteractionState.Failed;
        }

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

    public Holder<EntityStore> spawnSlot(CommandBuffer<EntityStore> accessor, Vector3f offset,
            Ref<EntityStore> glyphRef, SlotComponent slotComp,
            Ref<EntityStore> playerRef, String hintText) {

        TransformComponent glyphTransform = accessor.getComponent(glyphRef,
                TransformComponent.getComponentType());
        if (glyphTransform == null)
            return null;
        Vector3d parentPos = glyphTransform.getPosition();

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        HiddenUtils.addHiddenToHolder(accessor, holder, playerRef);

        Vector3d slotPos = new Vector3d(
                parentPos.x + offset.x,
                parentPos.y + offset.y,
                parentPos.z + offset.z);

        Vector3f color = slotComp.getSlotType() == GlyphSlotType.Input ? CraftingColors.INPUT : CraftingColors.OUTPUT;

        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(slotPos, new Vector3f(0, 0, 0)));

        holder.addComponent(SlotComponent.getComponentType(), slotComp);
        holder.addComponent(DebugComponent.getComponentType(),
                new DebugComponent(DebugShape.Cube, color, NODE_SCALE, 2.0f));

        Box slotBox = new Box(-NODE_SCALE, -NODE_SCALE, -NODE_SCALE,
                NODE_SCALE, NODE_SCALE, NODE_SCALE);
        holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(slotBox));

        holder.addComponent(UUIDComponent.getComponentType(),
                new UUIDComponent(UUID.randomUUID()));
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

        int networkId = accessor.getExternalData().takeNextNetworkId();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));
        HoverableComponent hoverable = new HoverableComponent(HoverableType.NODE);
        hoverable.setHintText(hintText);
        holder.addComponent(HoverableComponent.getComponentType(), hoverable);

        holder.addComponent(NodeComponent.getComponentType(),
                new NodeComponent(glyphRef, NodeType.Slot));

        holder.addComponent(MountedComponent.getComponentType(),
                new MountedComponent(glyphRef, offset, MountController.Minecart));
        return holder;
    }

    @Override
    public void despawn(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
        CraftingDataComponent playerData = CraftingDataUtil.getPedestalData(accessor, playerRef);

        if (playerData == null)
            return;
        this.despawn(accessor, playerData);
    }

    public void despawn(CommandBuffer<EntityStore> accessor, CraftingDataComponent playerData) {

        if (playerData == null)
            return;

        List<Ref<EntityStore>> slotRefs = playerData.getSlotNodeRefs();
        if (slotRefs == null)
            return;

        for (Ref<EntityStore> ref : slotRefs) {
            if (ref == null || !ref.isValid())
                continue;
            NodeComponent nodeComp = accessor.getComponent(ref, NodeComponent.getComponentType());
            if (nodeComp != null && nodeComp.getParentEntity() != null && nodeComp.getParentEntity().isValid()) {
                GlyphComponent glyphComp = accessor.getComponent(nodeComp.getParentEntity(),
                        GlyphComponent.getComponentType());
                if (glyphComp != null) {
                    glyphComp.setDetailsOpen(false);
                }
            }
            accessor.tryRemoveEntity(ref, RemoveReason.REMOVE);
        }
        slotRefs.clear();
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
