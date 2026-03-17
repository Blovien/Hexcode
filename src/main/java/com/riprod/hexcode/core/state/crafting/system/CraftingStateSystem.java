package com.riprod.hexcode.core.state.crafting.system;

import java.util.List;
import java.util.Objects;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hidden.utils.HiddenUtils;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.common.hover.utils.HoverableUtils;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalDataComponent;
import com.riprod.hexcode.core.state.crafting.handlers.CraftingDragHandler;
import com.riprod.hexcode.core.state.crafting.handlers.DetailsHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeRouter;
import com.riprod.hexcode.core.state.crafting.utils.HoverStyleUtils;
import com.riprod.hexcode.core.state.crafting.utils.LinkRenderer;
import com.riprod.hexcode.core.state.crafting.utils.PedestalDataUtil;

public class CraftingStateSystem {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String HOVER_PARTICLE = "Object_Hover";

    public static InteractionState enterInteraction(Ref<EntityStore> ref, HexcasterComponent comp,
            CommandBuffer<EntityStore> buffer) {
        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null)
            return InteractionState.Failed;

        Ref<EntityStore> hoveredRef = craftingComp.getHoveredRef();
        if (hoveredRef == null || !hoveredRef.isValid()) {
            return InteractionState.Failed;
        }

        HoverableComponent hoverComp = buffer.getComponent(hoveredRef,
                HoverableComponent.getComponentType());
        if (hoverComp == null) {
            return InteractionState.Failed;
        }

        craftingComp.setDragTickCount(0);

        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(buffer, ref);
        if (playerData == null) {
            return InteractionState.Failed;
        }

        switch (hoverComp.getType()) {
            case GLYPH:
                Ref<EntityStore> headAnchor = CraftingDragHandler.startDrag(buffer, ref, hoveredRef);
                craftingComp.setHeadAnchorRef(buffer, headAnchor);
                craftingComp.setDraggingRef(hoveredRef);
                return InteractionState.Finished;
            case NODE:
                return NodeRouter.enter(buffer, hoveredRef, ref);
            default:
                return InteractionState.Failed;
        }

    }

    public static void tickInteraction(CommandBuffer<EntityStore> accessor, float dt, Ref<EntityStore> ref,
            PedestalBlockComponent pedestal) {
        HexcasterCraftingComponent craftingComp = accessor.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null)
            return;

        craftingComp.setDragTickCount(craftingComp.getDragTickCount() + 1);

        Ref<EntityStore> draggedRef = craftingComp.getDraggingRef();
        if (draggedRef == null || !draggedRef.isValid()) {
            return;
        }

        HoverableComponent hoverComp = accessor.getComponent(draggedRef,
                HoverableComponent.getComponentType());
        HoverableType draggingType = hoverComp != null ? hoverComp.getType() : null;

        if (draggingType == null)
            return;

        switch (draggingType) {
            case GLYPH:
                CraftingDragHandler.updateDrag(accessor, craftingComp.getHeadAnchorRef(), ref);
                break;
            case NODE:
                NodeRouter.drag(accessor, craftingComp.getDraggingRef(), ref);
                break;
        }
    }

    public static InteractionState exitInteraction(CommandBuffer<EntityStore> accessor, Ref<EntityStore> ref) {

        HexcasterCraftingComponent craftingComp = accessor.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null)
            return InteractionState.Finished;

        boolean isClick = craftingComp.getDragTickCount() < 10;

        Ref<EntityStore> draggedRef = craftingComp.getDraggingRef();

        if (draggedRef == null || !draggedRef.isValid()) {
            craftingComp.setDraggingRef(null);
            craftingComp.setHeadAnchorRef(accessor, null);
            craftingComp.setDragTickCount(0);
            return InteractionState.Finished;
        }

        InteractionState result = InteractionState.Finished;

        if (isClick) {
            result = NodeRouter.click(accessor, draggedRef, ref);
        } else {
            result = NodeRouter.exit(accessor, draggedRef, ref);
        }

        craftingComp.setDraggingRef(null);
        craftingComp.setHeadAnchorRef(accessor, null);
        craftingComp.setDragTickCount(0);
        return result;
    }

    public static void tickCrafting(CommandBuffer<EntityStore> accessor, float dt, Ref<EntityStore> ref,
            PedestalBlockComponent pedestal) {
        HexcasterCraftingComponent craftingComp = accessor.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null)
            return;

        TransformComponent playerTransform = accessor.getComponent(ref,
                TransformComponent.getComponentType());
        if (playerTransform == null)
            return;

        List<Ref<EntityStore>> nearby = HoverableUtils.getNearbyHoverables(accessor,
                playerTransform.getPosition(), 8.0);

        Ref<EntityStore> draggedRef = craftingComp.getDraggingRef();
        if (draggedRef != null && draggedRef.isValid()) {
            nearby.remove(draggedRef); // remove the dragged ref from the hovered list
        }

        HiddenUtils.filterByOwner(accessor, nearby, ref);

        Ref<EntityStore> targetRef = HoverableUtils.getSmallestTarget(accessor, ref, nearby);

        Ref<EntityStore> previousHovered = craftingComp.getHoveredRef();
        boolean changed = !Objects.equals(targetRef, previousHovered);

        if (changed) {

            HoverStyleUtils.unhover(accessor, previousHovered, ref);

            craftingComp.setHoveredRef(targetRef);
            pedestal.setTickLength(HOVER_PARTICLE, 1f);

            HoverStyleUtils.hover(accessor, targetRef, ref);
        }

        Ref<EntityStore> playerRefFlag = pedestal.isPerPlayer() ? ref : null;

        HoverStyleUtils.hoverParticles(accessor, craftingComp.getHoveredRef(), dt, pedestal, ref);

        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(accessor, ref);
        if (playerData != null) {
            LinkRenderer.renderLinks(accessor, playerData, pedestal, dt, playerRefFlag);
        }
    }

    public static InteractionState enterAbility(CommandBuffer<EntityStore> accessor, Ref<EntityStore> ref, InteractionType inputType) {
        HexcasterCraftingComponent craftingComp = accessor.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null)
            return InteractionState.Finished;

        Ref<EntityStore> hoveredRef = craftingComp.getHoveredRef();
        if (hoveredRef == null)
            return InteractionState.Finished;

        InteractionState result = InteractionState.Finished;

        HoverableComponent hover = accessor.getComponent(hoveredRef,
                HoverableComponent.getComponentType());

        if (hover != null && hover.getType() == HoverableType.NODE) 
            result = NodeRouter.ability(accessor, hoveredRef, inputType, ref);
            
        return result;
    }
}
