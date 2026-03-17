package com.riprod.hexcode.core.state.crafting.handlers.node.Effect;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
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
import com.riprod.hexcode.core.common.glyphs.utils.CreateGlyph;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hidden.utils.HiddenUtils;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.common.hover.utils.HoverableUtils;
import com.riprod.hexcode.core.state.casting.utils.GlyphStyler;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.NodeComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalDataComponent;
import com.riprod.hexcode.core.state.crafting.constants.CraftingColors;
import com.riprod.hexcode.core.state.crafting.constants.NodeType;
import com.riprod.hexcode.core.state.crafting.handlers.CraftingDragHandler;
import com.riprod.hexcode.core.state.crafting.handlers.CraftingDropHandler;
import com.riprod.hexcode.core.state.crafting.handlers.DetailsHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeInterface;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeRouter;
import com.riprod.hexcode.core.state.crafting.handlers.node.Glyph.GlyphNodeHandler;
import com.riprod.hexcode.core.state.crafting.utils.CraftingPositionUtil;
import com.riprod.hexcode.core.state.crafting.utils.PedestalDataUtil;
import com.riprod.hexcode.utils.CleanupUtils;

public class EffectNodeHandler implements NodeInterface {
    public static final EffectNodeHandler INSTANCE = new EffectNodeHandler();

    @Override
    public InteractionState enter(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef) {
        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null)
            return InteractionState.Failed;

        Ref<EntityStore> headAnchor = CraftingDragHandler.startDrag(accessor, playerRef, node);
        craftingComp.setHeadAnchorRef(accessor, headAnchor);
        craftingComp.setDraggingRef(node);
        return InteractionState.Finished;
    }

    @Override
    public InteractionState tick(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef) {
        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null)
            return InteractionState.Failed;

        CraftingDragHandler.updateDrag(accessor, craftingComp.getHeadAnchorRef(), playerRef);
        return InteractionState.Finished;
    }

    @Override
    public InteractionState exit(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef) {

        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(accessor, playerRef);

        TransformComponent playerTransform = accessor.getComponent(playerRef,
                TransformComponent.getComponentType());
        Ref<EntityStore> dropTargetRef = null;
        if (playerTransform != null) {
            List<Ref<EntityStore>> nearby = HoverableUtils.getNearbyHoverables(accessor,
                    playerTransform.getPosition(), 8.0);

            HiddenUtils.filterByOwner(accessor, nearby, playerRef);
            List<Ref<EntityStore>> filtered = new ArrayList<>(nearby.size());
            for (Ref<EntityStore> candidate : nearby) {
                if (!candidate.equals(nodeRef)) {
                    filtered.add(candidate);
                }
            }
            dropTargetRef = HoverableUtils.getSmallestTarget(accessor, playerRef, filtered);
        }

        CraftingDropHandler.DropResult dResult = CraftingDropHandler.handleDrop(
                accessor, nodeRef, dropTargetRef);

        Vector3f dropOffset = CraftingPositionUtil.lookToHexOffset(accessor, playerRef,
                playerData.getAnchorNodeRef(), 2.0f);
        Vector3d dropWorldPos = CraftingPositionUtil.hexOffsetToWorld(accessor,
                playerData.getAnchorNodeRef(), dropOffset);

        GlyphComponent effect = accessor.getComponent(nodeRef,
                GlyphComponent.getComponentType());
        Vector3f rotation = effect != null ? effect.getRotation() : Vector3f.ZERO;

        accessor.putComponent(nodeRef, TransformComponent.getComponentType(),
                new TransformComponent(dropWorldPos, rotation));

        if (effect != null) {
            effect.getGlyph().setPosition(dropOffset);

            Ref<EntityStore> graphNodeRef = effect.getNodeRef();
            if (graphNodeRef != null && graphNodeRef.isValid()) {
                accessor.putComponent(graphNodeRef, TransformComponent.getComponentType(),
                        new TransformComponent(dropWorldPos, Vector3f.ZERO));
            }
        }

        return InteractionState.Finished;
    }

    @Override
    public InteractionState click(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef) {

        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(accessor, playerRef);

        if (DetailsHandler.isOpenFor(accessor, playerData.getSlotNodeRefs(), nodeRef)) {
            DetailsHandler.closeDetails(accessor, playerData.getSlotNodeRefs());
            playerData.setSlotNodeRefs(null);
        } else {
            if (playerData.getSlotNodeRefs() != null
                    && !playerData.getSlotNodeRefs().isEmpty()) {
                DetailsHandler.closeDetails(accessor, playerData.getSlotNodeRefs());
            }
            List<Ref<EntityStore>> slotRefs = DetailsHandler.openDetails(
                    accessor, nodeRef, playerRef);
            playerData.setSlotNodeRefs(slotRefs);
        }

        return InteractionState.Finished;
    }

    public Ref<EntityStore> spawnNode(CommandBuffer<EntityStore> accessor, Ref<EntityStore> parentRef,
            Vector3d position, Ref<EntityStore> playerRef, GlyphComponent glyphComp,
            Ref<EntityStore> hexEntityRef) {

        Holder<EntityStore> glyphHolder = CreateGlyph.createGlyphHolder(accessor, glyphComp, position);
        HiddenUtils.addHiddenToHolder(accessor, glyphHolder, playerRef);
        glyphHolder.addComponent(HoverableComponent.getComponentType(),
                new HoverableComponent(HoverableType.NODE));
        glyphHolder.addComponent(NodeComponent.getComponentType(),
                new NodeComponent(hexEntityRef, NodeType.Effect));
        Ref<EntityStore> glyphNodeRef = accessor.addEntity(glyphHolder, AddReason.SPAWN);
        glyphComp.setSelfRef(glyphNodeRef);

        Holder<EntityStore> glyphNode = GlyphNodeHandler.INSTANCE.spawnNode(accessor, glyphNodeRef, position,
                playerRef);
        Ref<EntityStore> graphNodeRef = accessor.addEntity(glyphNode, AddReason.SPAWN);
        glyphComp.setNodeRef(graphNodeRef);

        glyphComp.setHexRef(hexEntityRef);
        glyphComp.setParentRef(hexEntityRef);

        return glyphNodeRef;
    }

    @Override
    public void despawn(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
    }

    @Override
    public void hover(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
        GlyphComponent glyph = accessor.getComponent(nodeRef, GlyphComponent.getComponentType());
        if (glyph == null)
            return;
        GlyphStyler.enterGlyphHover(accessor, glyph);
    }

    @Override
    public void unhover(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
        GlyphComponent glyph = accessor.getComponent(nodeRef, GlyphComponent.getComponentType());
        if (glyph == null)
            return;
        GlyphStyler.exitGlyphHover(accessor, glyph);
    }

    @Override
    public InteractionState ability(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            InteractionType inputType, Ref<EntityStore> playerRef) {
        return InteractionState.Finished;
    }

}
