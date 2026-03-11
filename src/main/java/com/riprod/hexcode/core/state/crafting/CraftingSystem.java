package com.riprod.hexcode.core.state.crafting;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hidden.utils.HiddenUtils;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.common.hover.utils.HoverableUtils;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalDataComponent;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;
import com.riprod.hexcode.core.state.crafting.handlers.CraftingDragHandler;
import com.riprod.hexcode.core.state.crafting.handlers.CraftingDropHandler;
import com.riprod.hexcode.core.state.crafting.handlers.DetailsHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeRouter;
import com.riprod.hexcode.core.state.crafting.system.CraftingStateSystem;
import com.riprod.hexcode.core.state.crafting.system.SelectingStateSystem;
import com.riprod.hexcode.core.state.crafting.utils.CraftingPositionUtil;
import com.riprod.hexcode.core.state.crafting.utils.GravityUtil;
import com.riprod.hexcode.core.state.crafting.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.state.crafting.utils.PedestalDataUtil;
import com.riprod.hexcode.state.HexState;
import com.riprod.hexcode.state.HexcodeManager;

public class CraftingSystem extends HexcodeManager {



    @Override
    public void firstTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer,
            HexState previousState) {

        if (previousState == HexState.DRAWING) {
            return;
        }

        Ref<EntityStore> anchorRef = comp.consumePendingPedestalRef();

        if (anchorRef == null || !anchorRef.isValid()) {
            comp.requestStateChange(HexState.IDLE);
            return;
        }

        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());

        if (craftingComp == null) {
            craftingComp = new HexcasterCraftingComponent();
            buffer.putComponent(ref, HexcasterCraftingComponent.getComponentType(), craftingComp);
        }
        craftingComp.setPedestalEntityRef(anchorRef);
        GravityUtil.enterFly(buffer, ref);
    }

    @Override
    public void lastTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer,
            HexState nextState) {

        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(buffer, ref);
        if (craftingComp == null) {
            comp.clearCraftingState();
            return;
        }

        DetailsHandler.closeDetails(buffer, playerData.getSlotNodeRefs());

        CraftingDragHandler.endDrag(buffer, craftingComp.getDraggingRef(),
                craftingComp.getHeadAnchorRef());

        if (nextState == HexState.DRAWING) {
            return; //
        }

        // exiting crafting mode for real
        GravityUtil.exitFly(buffer, ref);

        if (!playerData.isPerPlayer()) {
            return; // will be cleaned up by pedestal exit
        }

        Ref<EntityStore> rootNodeRef = playerData.getAnchorNodeRef();
        if (rootNodeRef != null && rootNodeRef.isValid()) {
            buffer.tryRemoveEntity(rootNodeRef, RemoveReason.REMOVE);
        }
        craftingComp.clearCraftingState();
        comp.clearCraftingState();

        PedestalDataUtil.dropContents(buffer, null, playerData, null);

        List<Ref<EntityStore>> allRefs = playerData.getAllRefs();
        allRefs.forEach(r -> {
            if (r != null && r.isValid()) {
                buffer.tryRemoveEntity(r, RemoveReason.REMOVE);
            }
        });

    }

    @Override
    public void tick0(Ref<EntityStore> ref, HexcasterComponent comp, float dt,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

        PedestalBlockComponent pedestal = PedestalBlockUtil.resolvePedestal(ref, buffer);
        if (pedestal == null) {
            return;
        }

        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(buffer, ref);

        switch (playerData.getState()) {
            case SELECTING:
                SelectingStateSystem.tickSelecting(buffer, dt, ref, pedestal, playerData);
                break;
            case CRAFTING:
                CraftingStateSystem.tickCrafting(buffer, dt, ref, pedestal);
                HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                        HexcasterCraftingComponent.getComponentType());
                if (craftingComp != null && craftingComp.getDraggingRef() == null
                        && craftingComp.getHeadAnchorRef() != null
                        && craftingComp.getHeadAnchorRef().isValid()) {

                    buffer.tryRemoveEntity(craftingComp.getHeadAnchorRef(), RemoveReason.REMOVE);
                    craftingComp.setHeadAnchorRef(null);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public InteractionState enterInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref,
            HexcasterComponent comp) {

        PedestalBlockComponent pedestal = PedestalBlockUtil.resolvePedestal(ref, buffer);
        if (pedestal == null) {
            return InteractionState.Failed;
        }
        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(buffer, ref);

        switch (playerData.getState()) {
            case SELECTING:
                return SelectingStateSystem.enterInteraction(ref, comp, buffer);
            case CRAFTING:
                return CraftingStateSystem.enterInteraction(ref, comp, buffer);
            default:
                break;
        }

        return InteractionState.Finished;
    }

    @Override
    public InteractionState enterAbilityTwo(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref,
            HexcasterComponent comp) {
        PedestalBlockComponent pedestal = PedestalBlockUtil.resolvePedestal(ref, buffer);

        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(buffer, ref);

        if (pedestal == null || playerData.getState() != PedestalState.CRAFTING) {
            return InteractionState.Finished;
        }

        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null)
            return InteractionState.Finished;

        Ref<EntityStore> hoveredRef = craftingComp.getHoveredRef();
        if (hoveredRef == null)
            return InteractionState.Finished;

        HoverableComponent hover = buffer.getComponent(hoveredRef,
                HoverableComponent.getComponentType());
        if (hover == null)
            return InteractionState.Finished;

        if (hover.getType() == HoverableType.NODE) {
            NodeRouter.ability(buffer, hoveredRef, ref);
            return InteractionState.Finished;
        }

        if (hover.getType() != HoverableType.GLYPH) {
            return InteractionState.Finished;
        }

        return InteractionState.Finished;
    }

    @Override
    public InteractionState tickInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref, float dt,
            HexcasterComponent comp) {

        PedestalBlockComponent pedestal = PedestalBlockUtil.resolvePedestal(ref, buffer);
        if (pedestal == null)
            return InteractionState.NotFinished;

        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(buffer, ref);

        if (playerData.getState() == PedestalState.CRAFTING) {
            CraftingStateSystem.tickInteraction(buffer, dt, ref, pedestal);
        }

        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState exitInteraction(CommandBuffer<EntityStore> accessor, Ref<EntityStore> ref,
            HexcasterComponent comp) {
        PedestalBlockComponent pedestal = PedestalBlockUtil.resolvePedestal(ref, accessor);

        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(accessor, ref);

        if (pedestal == null || playerData.getState() != PedestalState.CRAFTING) {
            return InteractionState.Finished;
        }
        Ref<EntityStore> playerRefFlag = pedestal.isPerPlayer() ? ref : null;

        HexcasterCraftingComponent craftingComp = accessor.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null)
            return InteractionState.Finished;

        if (craftingComp.getDragTickCount() < 10) {
            Ref<EntityStore> clickedRef = craftingComp.getDraggingRef();
            CraftingDragHandler.endDrag(accessor, clickedRef, craftingComp.getHeadAnchorRef());

            craftingComp.setDraggingRef(null);
            craftingComp.setHeadAnchorRef(null);
            craftingComp.setDragTickCount(0);

            if (clickedRef != null && clickedRef.isValid()) {
                HoverableComponent hoverComp = accessor.getComponent(clickedRef,
                        HoverableComponent.getComponentType());
                if (hoverComp != null) {
                    switch (hoverComp.getType()) {
                        case GLYPH: {
                            GlyphComponent effect = accessor.getComponent(clickedRef,
                                    GlyphComponent.getComponentType());
                            if (effect != null) {
                                if (DetailsHandler.isOpenFor(accessor, playerData.getSlotNodeRefs(), clickedRef)) {
                                    DetailsHandler.closeDetails(accessor, playerData.getSlotNodeRefs());
                                    playerData.setSlotNodeRefs(null);
                                } else {
                                    if (playerData.getSlotNodeRefs() != null
                                            && !playerData.getSlotNodeRefs().isEmpty()) {
                                        DetailsHandler.closeDetails(accessor, playerData.getSlotNodeRefs());
                                    }
                                    List<Ref<EntityStore>> slotRefs = DetailsHandler.openDetails(
                                            accessor, clickedRef, ref);
                                    playerData.setSlotNodeRefs(slotRefs);
                                }
                            }
                        }
                            break;
                        case NODE:
                            NodeRouter.click(accessor, clickedRef, ref);
                            break;
                        default:
                            break;
                    }
                }
            }

            return InteractionState.Finished;
        }

        Ref<EntityStore> draggedRef = craftingComp.getDraggingRef();

        if (draggedRef == null || !draggedRef.isValid())
            return InteractionState.Failed;

        HoverableComponent hoverComp = accessor.getComponent(draggedRef,
                HoverableComponent.getComponentType());

        switch (hoverComp.getType()) {
            case GLYPH: {
                CraftingDragHandler.endDrag(accessor, draggedRef, craftingComp.getHeadAnchorRef());

                TransformComponent playerTransform = accessor.getComponent(ref,
                        TransformComponent.getComponentType());
                Ref<EntityStore> dropTargetRef = null;
                if (playerTransform != null) {
                    List<Ref<EntityStore>> nearby = HoverableUtils.getNearbyHoverables(accessor,
                            playerTransform.getPosition(), 8.0);

                    HiddenUtils.filterByOwner(accessor, nearby, ref);
                    List<Ref<EntityStore>> filtered = new ArrayList<>(nearby.size());
                    for (Ref<EntityStore> candidate : nearby) {
                        if (!candidate.equals(draggedRef)) {
                            filtered.add(candidate);
                        }
                    }
                    dropTargetRef = HoverableUtils.getSmallestTarget(accessor, ref, filtered);
                }

                CraftingDropHandler.DropResult result = CraftingDropHandler.handleDrop(
                        accessor, draggedRef, dropTargetRef);

                switch (result) {
                    case LINKED:
                    case PLACED:
                    case IGNORED:
                    default:
                        Vector3f dropOffset = CraftingPositionUtil.lookToHexOffset(accessor, ref,
                                playerData.getAnchorNodeRef(), 2.0f);
                        Vector3d dropWorldPos = CraftingPositionUtil.hexOffsetToWorld(accessor,
                                playerData.getAnchorNodeRef(), dropOffset);

                        GlyphComponent effect = accessor.getComponent(draggedRef,
                                GlyphComponent.getComponentType());
                        Vector3f rotation = effect != null ? effect.getRotation() : Vector3f.ZERO;

                        accessor.putComponent(draggedRef, TransformComponent.getComponentType(),
                                new TransformComponent(dropWorldPos, rotation));

                        if (effect != null) {
                            effect.getGlyph().setPosition(dropOffset);
                        }
                        updateNodePosition(accessor, draggedRef, dropWorldPos);
                        break;

                }

                craftingComp.setDraggingRef(null);
                craftingComp.setHeadAnchorRef(null);
                craftingComp.setDragTickCount(0);
                return InteractionState.Finished;
            }
            case NODE:
                return NodeRouter.exit(accessor, draggedRef, ref);
            default:
                break;

        }
        return InteractionState.Finished;
    }

    private static void updateNodePosition(CommandBuffer<EntityStore> buffer,
            Ref<EntityStore> glyphRef, Vector3d glyphWorldPos) {
        GlyphComponent effect = buffer.getComponent(glyphRef,
                GlyphComponent.getComponentType());
        if (effect == null || effect.getNodeRef() == null)
            return;
        TransformComponent nodeTransform = buffer.getComponent(effect.getNodeRef(),
                TransformComponent.getComponentType());
        if (nodeTransform != null) {
            nodeTransform.setPosition(new Vector3d(
                    glyphWorldPos.x, glyphWorldPos.y, glyphWorldPos.z));
        }
    }

    @Override
    public void onPlayerJoin(Holder<EntityStore> holder, HexcasterComponent comp) {
    }

    @Override
    public void onPlayerLeave(PlayerRef playerRef) {
    }
}
