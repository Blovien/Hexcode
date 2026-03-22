package com.riprod.hexcode.core.state.crafting.handlers.node.Effect;

import java.util.List;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.utils.CreateGlyph;
import com.riprod.hexcode.core.common.hidden.utils.HiddenUtils;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.state.casting.utils.GlyphStyler;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.NodeComponent;
import com.riprod.hexcode.core.state.crafting.component.CraftingDataComponent;
import com.riprod.hexcode.core.state.crafting.constants.NodeType;
import com.riprod.hexcode.core.state.crafting.handlers.CraftingDragHandler;
import com.riprod.hexcode.core.state.crafting.handlers.CraftingDropHandler;
import com.riprod.hexcode.core.state.crafting.handlers.DetailsHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeInterface;
import com.riprod.hexcode.core.state.crafting.handlers.node.Glyph.GlyphNodeHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.Slot.SlotNodeHandler;
import com.riprod.hexcode.core.state.crafting.utils.CraftingPositionUtil;
import com.riprod.hexcode.core.state.crafting.utils.CraftingDataUtil;

public class EffectNodeHandler implements NodeInterface {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
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

        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());

        CraftingDataComponent playerData = CraftingDataUtil.getPedestalData(accessor, playerRef);

        Ref<EntityStore> dropTargetRef = craftingComp.getHoveredRef();

        // handles linking logic
        CraftingDropHandler.DropResult dResult = CraftingDropHandler.handleDrop(
                accessor, nodeRef, dropTargetRef);

        Vector3f dropOffset = CraftingPositionUtil.lookToHexOffset(accessor, playerRef,
                playerData.getAnchorNodeRef(), 2.0f);
        Vector3d dropWorldPos = CraftingPositionUtil.hexOffsetToWorld(accessor,
                playerData.getAnchorNodeRef(), dropOffset);

        GlyphComponent effect = accessor.getComponent(nodeRef,
                GlyphComponent.getComponentType());

        Ref<EntityStore> headAnchorRef = craftingComp.getHeadAnchorRef();
        TransformComponent headTransform = accessor.getComponent(headAnchorRef, TransformComponent.getComponentType());
        Vector3f playerRotation = headTransform.getRotation();

        TransformComponent nodeTransform = accessor.getComponent(nodeRef, TransformComponent.getComponentType());
        nodeTransform.getPosition().assign(dropWorldPos);
        nodeTransform.getRotation().assign(playerRotation);

        if (effect != null) {
            effect.setOffset(dropOffset);
            effect.setRotation(playerRotation);
            LOGGER.atInfo().log("effect node: dropped at offset %s world pos %s", dropOffset, dropWorldPos);

            Ref<EntityStore> graphNodeRef = effect.getNodeRef();
            if (graphNodeRef != null && graphNodeRef.isValid()) {
                TransformComponent graphTransform = accessor.getComponent(graphNodeRef, TransformComponent.getComponentType());
                graphTransform.getPosition().assign(dropWorldPos);
                graphTransform.getRotation().assign(Vector3f.ZERO);
            }
        }

        return InteractionState.Finished;
    }

    @Override
    public InteractionState click(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef) {

        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());

        Ref<EntityStore> headAnchorRef = craftingComp.getHeadAnchorRef();
        if (headAnchorRef != null && headAnchorRef.isValid()) {
            // despawn it 
            craftingComp.setHeadAnchorRef(accessor, null);
        }

        CraftingDataComponent playerData = CraftingDataUtil.getPedestalData(accessor, playerRef);

        if (DetailsHandler.isOpenFor(accessor, playerData.getSlotNodeRefs(), nodeRef)) {
            SlotNodeHandler.INSTANCE.despawn(accessor, playerData);
            playerData.setSlotNodeRefs(null);
        } else {
            if (playerData.getSlotNodeRefs() != null
                    && !playerData.getSlotNodeRefs().isEmpty()) {
                SlotNodeHandler.INSTANCE.despawn(accessor, playerData);
            }
            List<Ref<EntityStore>> slotRefs = DetailsHandler.openDetails(
                    accessor, nodeRef, playerRef);
            playerData.setSlotNodeRefs(slotRefs);
        }

        // reset position

        Vector3f playerRotation = accessor.getComponent(playerRef, HeadRotation.getComponentType()).getRotation();

        GlyphComponent effect = accessor.getComponent(nodeRef,
                GlyphComponent.getComponentType());

        Vector3d dropWorldPos = CraftingPositionUtil.hexOffsetToWorld(accessor,
                playerData.getAnchorNodeRef(), effect.getOffset());

        TransformComponent clickTransform = accessor.getComponent(nodeRef, TransformComponent.getComponentType());
        clickTransform.getPosition().assign(dropWorldPos);
        clickTransform.getRotation().assign(playerRotation);

        return InteractionState.Finished;
    }

    public Ref<EntityStore> spawnNode(CommandBuffer<EntityStore> accessor, Ref<EntityStore> parentRef,
            Vector3d position, Ref<EntityStore> playerRef, GlyphComponent glyphComp,
            Ref<EntityStore> hexEntityRef) {

        Glyph glyph = glyphComp.getGlyph();
        Vector3f glyphRot = new Vector3f(glyph.getRotation().getPitch(), glyph.getRotation().getYaw(), 0);
        Holder<EntityStore> glyphHolder = CreateGlyph.createGlyphHolder(accessor, glyphComp, position, glyphRot);

        HiddenUtils.addHiddenToHolder(accessor, glyphHolder, playerRef);
        HoverableComponent hoverComp = new HoverableComponent(HoverableType.NODE);
        try {
            GlyphAsset glyphAsset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
            hoverComp.setHintText(glyphAsset.getTitle());
        } catch (Exception e) {
        }
        glyphHolder.addComponent(HoverableComponent.getComponentType(),
                hoverComp);
        glyphHolder.addComponent(NodeComponent.getComponentType(),
                new NodeComponent(hexEntityRef, NodeType.Effect));
        Ref<EntityStore> glyphNodeRef = accessor.addEntity(glyphHolder, AddReason.SPAWN);
        glyphComp.setSelfRef(glyphNodeRef);

        Holder<EntityStore> glyphNodeHolder = GlyphNodeHandler.INSTANCE.spawnNode(accessor, glyphNodeRef, position,
                playerRef);

        Ref<EntityStore> graphNodeRef = accessor.addEntity(glyphNodeHolder, AddReason.SPAWN);
        glyphComp.setNodeRef(graphNodeRef);

        glyphComp.setHexRef(hexEntityRef);
        glyphComp.setParentRef(hexEntityRef);

        return glyphNodeRef;
    }

    @Override
    public void despawn(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {

        GlyphComponent effect = accessor.getComponent(nodeRef,
                GlyphComponent.getComponentType());
        if (effect == null)
            return;

        Ref<EntityStore> graphNodeRef = effect.getNodeRef();

        if (graphNodeRef == null || !graphNodeRef.isValid()) {
            return;
        }

        GlyphNodeHandler.INSTANCE.despawn(accessor, graphNodeRef, playerRef);
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
    public InteractionState ability(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            InteractionType inputType, Ref<EntityStore> playerRef) {

        if (inputType != InteractionType.Ability3) { // if not R - ignore
            return InteractionState.Failed;
        }

        GlyphComponent effect = accessor.getComponent(nodeRef,
                GlyphComponent.getComponentType());

        Ref<EntityStore> graphNodeRef = effect.getNodeRef();
        if (graphNodeRef == null || !graphNodeRef.isValid()) {
            return InteractionState.Failed;
        }

        GlyphNodeHandler.INSTANCE.ability(accessor, graphNodeRef, inputType, playerRef); // let the node handle the
                                                                                         // logic (it's the same)

        return InteractionState.Finished;
    }

}
