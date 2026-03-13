package com.riprod.hexcode.core.state.crafting.system;

import java.util.List;
import java.util.Objects;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
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

            HoverStyleUtils.unhover(accessor, previousHovered);

            craftingComp.setHoveredRef(targetRef);
            pedestal.setTickLength(HOVER_PARTICLE, 1f);

            HoverStyleUtils.hover(accessor, targetRef);
        }

        Ref<EntityStore> playerRefFlag = pedestal.isPerPlayer() ? ref : null;

        HoverStyleUtils.hoverParticles(accessor, craftingComp.getHoveredRef(), dt, pedestal, ref);

        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(accessor, ref);
        if (playerData != null) {
            LinkRenderer.renderLinks(accessor, playerData, pedestal, dt, playerRefFlag);
        }
    }
}
