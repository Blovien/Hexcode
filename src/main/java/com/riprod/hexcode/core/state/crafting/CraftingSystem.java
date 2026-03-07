package com.riprod.hexcode.core.state.crafting;

import java.util.ArrayList;
import java.util.List;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.EffectComponent;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.common.hover.utils.HoverableUtils;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.NodeComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.system.CraftingStateSystem;
import com.riprod.hexcode.core.state.crafting.system.SelectingStateSystem;
import com.riprod.hexcode.core.state.crafting.utils.CraftingDragUtil;
import com.riprod.hexcode.core.state.crafting.utils.CraftingGlyphNodeSpawner;
import com.riprod.hexcode.core.state.crafting.utils.CraftingDropHandler;
import com.riprod.hexcode.core.state.crafting.utils.CraftingGlyphRemover;
import com.riprod.hexcode.core.state.crafting.utils.CraftingPositionUtil;
import com.riprod.hexcode.core.state.crafting.utils.DetailsRenderer;
import com.riprod.hexcode.core.state.crafting.utils.GravityUtil;
import com.riprod.hexcode.core.state.crafting.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.state.crafting.utils.PedestalState;

import com.riprod.hexcode.state.HexState;
import com.riprod.hexcode.state.HexcodeManager;

public class CraftingSystem extends HexcodeManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public void firstTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer,
            HexState previousState) {

        Ref<EntityStore> anchorRef = comp.consumePendingPedestalRef();

        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());

        if (anchorRef != null && anchorRef.isValid()) {
            if (craftingComp == null) {
                craftingComp = new HexcasterCraftingComponent();
                buffer.putComponent(ref, HexcasterCraftingComponent.getComponentType(), craftingComp);
            }
            craftingComp.setPedestalRef(anchorRef);
        }

        if (previousState != HexState.DRAWING) {
            GravityUtil.enterFly(buffer, ref);
        }
    }

    @Override
    public void lastTick(Ref<EntityStore> ref, HexcasterComponent comp,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer,
            HexState nextState) {

        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null) {
            comp.clearCraftingState();
            return;
        }

        if (nextState != HexState.DRAWING) {
            GravityUtil.exitFly(buffer, ref);
        }

        DetailsRenderer.closeDetails(buffer, craftingComp.getDetailSlotRefs());

        CraftingDragUtil.endDrag(buffer, craftingComp.getDraggingRef(),
                craftingComp.getHeadAnchorRef());

        if (nextState != HexState.DRAWING) {
            craftingComp.clearCraftingState();
            comp.clearCraftingState();
        }
    }

    @Override
    public void tick0(Ref<EntityStore> ref, HexcasterComponent comp, float dt,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {

        PedestalBlockComponent pedestal = PedestalBlockUtil.resolvePedestal(ref, buffer);
        if (pedestal == null) {
            return;
        }

        switch (pedestal.getState()) {
            case SELECTING:
                SelectingStateSystem.tickSelecting(buffer, dt, ref, pedestal);
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
        }
    }

    @Override
    public InteractionState enterInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref,
            HexcasterComponent comp) {

        PedestalBlockComponent pedestal = PedestalBlockUtil.resolvePedestal(ref, buffer);
        if (pedestal == null) {
            return InteractionState.Failed;
        }

        switch (pedestal.getState()) {
            case SELECTING:
                return SelectingStateSystem.enterInteraction(ref, comp, buffer);
            case CRAFTING:
                return CraftingStateSystem.enterInteraction(ref, comp, buffer);
        }

        return InteractionState.Finished;
    }

    @Override
    public InteractionState enterAbilityTwo(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref,
            HexcasterComponent comp) {
        PedestalBlockComponent pedestal = PedestalBlockUtil.resolvePedestal(ref, buffer);
        if (pedestal == null || pedestal.getState() != PedestalState.CRAFTING) {
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
        if (hover == null || hover.getType() != HoverableType.GLYPH) {
            return InteractionState.Finished;
        }

        HexComponent hexComp = buffer.getComponent(craftingComp.getHexRootRef(),
                HexComponent.getComponentType());
        if (hexComp == null)
            return InteractionState.Finished;

        if (craftingComp.isRemoveWarning()
                && hoveredRef.equals(craftingComp.getRemoveWarningRef())) {
            if (hoveredRef.equals(craftingComp.getDetailsGlyphRef())) {
                DetailsRenderer.closeDetails(buffer, craftingComp.getDetailSlotRefs());
                craftingComp.setDetailsGlyphRef(null);
            }
            CraftingGlyphRemover.removeGlyph(buffer, hoveredRef, hexComp);
            craftingComp.setRemoveWarning(false);
            craftingComp.setRemoveWarningRef(null);
            craftingComp.setHoveredRef(null, null);
        } else {
            CraftingGlyphRemover.removeLinks(buffer, hoveredRef, hexComp);
            craftingComp.setRemoveWarning(true);
            craftingComp.setRemoveWarningRef(hoveredRef);
        }

        return InteractionState.Finished;
    }

    @Override
    public InteractionState tickInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref, float dt,
            HexcasterComponent comp) {

        PedestalBlockComponent pedestal = PedestalBlockUtil.resolvePedestal(ref, buffer);
        if (pedestal == null)
            return InteractionState.NotFinished;

        if (pedestal.getState() == PedestalState.CRAFTING) {
            CraftingStateSystem.tickInteraction(buffer, dt, ref, pedestal);
        }

        return InteractionState.NotFinished;
    }

    @Override
    public InteractionState exitInteraction(CommandBuffer<EntityStore> buffer, Ref<EntityStore> ref,
            HexcasterComponent comp) {
        PedestalBlockComponent pedestal = PedestalBlockUtil.resolvePedestal(ref, buffer);
        if (pedestal == null || pedestal.getState() != PedestalState.CRAFTING) {
            return InteractionState.Finished;
        }

        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null)
            return InteractionState.Finished;

        if (craftingComp.getDragTickCount() < 3) {
            Ref<EntityStore> draggedRef = craftingComp.getDraggingRef();
            CraftingDragUtil.endDrag(buffer, draggedRef, craftingComp.getHeadAnchorRef());
            LOGGER.atInfo().log("Exiting interaction after click and setting drag ref to null");
            craftingComp.setDraggingRef(null, null);
            craftingComp.setHeadAnchorRef(null);
            craftingComp.setDragTickCount(0);

            if (craftingComp.getDetailsGlyphRef() != null) {
                DetailsRenderer.closeDetails(buffer, craftingComp.getDetailSlotRefs());
                craftingComp.setDetailsGlyphRef(null);
            }

            Ref<EntityStore> hoveredRef = craftingComp.getHoveredRef();
            if (hoveredRef != null && hoveredRef.isValid()) {
                HoverableComponent hoverComp = buffer.getComponent(hoveredRef,
                        HoverableComponent.getComponentType());
                if (hoverComp != null && hoverComp.getType() == HoverableType.GLYPH) {
                    EffectComponent effect = buffer.getComponent(hoveredRef,
                            EffectComponent.getComponentType());
                    if (effect != null) {
                        List<Ref<EntityStore>> slotRefs = DetailsRenderer.openDetails(
                                buffer, hoveredRef, effect);
                        craftingComp.setDetailsGlyphRef(hoveredRef);
                        craftingComp.setDetailSlotRefs(slotRefs);
                    }
                }
            }

            return InteractionState.Finished;
        }

        HoverableType draggedType = craftingComp.getDraggingType();

        if (draggedType == null)
            return InteractionState.Failed;

        Ref<EntityStore> draggedRef = craftingComp.getDraggingRef();

        if (draggedRef == null || !draggedRef.isValid())
            return InteractionState.Failed;

        switch (draggedType) {
            case GLYPH: {
                CraftingDragUtil.endDrag(buffer, draggedRef, craftingComp.getHeadAnchorRef());

                TransformComponent playerTransform = buffer.getComponent(ref,
                        TransformComponent.getComponentType());
                Ref<EntityStore> dropTargetRef = null;
                if (playerTransform != null) {
                    List<Ref<EntityStore>> nearby = HoverableUtils.getNearbyHoverables(buffer,
                            playerTransform.getPosition(), 8.0);
                    List<Ref<EntityStore>> filtered = new ArrayList<>(nearby.size());
                    for (Ref<EntityStore> candidate : nearby) {
                        if (!candidate.equals(draggedRef)) {
                            filtered.add(candidate);
                        }
                    }
                    dropTargetRef = HoverableUtils.getSmallestTarget(buffer, ref, filtered);
                }

                CraftingDropHandler.DropResult result = CraftingDropHandler.handleDrop(
                        buffer, draggedRef, dropTargetRef);

                switch (result) {
                    case LINKED:
                    case PLACED:
                    case IGNORED:
                    default:
                        Vector3f dropOffset = CraftingPositionUtil.lookToHexOffset(buffer, ref,
                                craftingComp.getHexRootRef(), 2.0f);
                        Vector3d dropWorldPos = CraftingPositionUtil.hexOffsetToWorld(buffer,
                                craftingComp.getHexRootRef(), dropOffset);

                        EffectComponent effect = buffer.getComponent(draggedRef,
                                EffectComponent.getComponentType());
                        Vector3f rotation = effect != null ? effect.getRotation() : Vector3f.ZERO;

                        buffer.putComponent(draggedRef, TransformComponent.getComponentType(),
                                new TransformComponent(dropWorldPos, rotation));

                        if (effect != null) {
                            effect.getGlyph().setPosition(dropOffset);
                        }
                        updateNodePosition(buffer, draggedRef, dropWorldPos);
                        break;

                    case SLOTTED:
                        if (dropTargetRef != null && dropTargetRef.isValid()) {
                            Ref<EntityStore> slottedGlyphRef = resolveGlyphFromTarget(buffer, dropTargetRef);
                            if (slottedGlyphRef != null && slottedGlyphRef.isValid()) {
                                if (craftingComp.getDetailsGlyphRef() != null) {
                                    DetailsRenderer.closeDetails(buffer, craftingComp.getDetailSlotRefs());
                                }
                                EffectComponent slottedEffect = buffer.getComponent(slottedGlyphRef,
                                        EffectComponent.getComponentType());
                                if (slottedEffect != null) {
                                    List<Ref<EntityStore>> slotRefs = DetailsRenderer.openDetails(
                                            buffer, slottedGlyphRef, slottedEffect);
                                    craftingComp.setDetailsGlyphRef(slottedGlyphRef);
                                    craftingComp.setDetailSlotRefs(slotRefs);
                                }
                            }
                        }
                        break;

                    case SWAPPED:
                        LOGGER.atInfo().log("crafting: variable swap not yet implemented");
                        break;
                }

                LOGGER.atInfo().log("Exiting interaction after drop and setting drag ref to null");
                craftingComp.setDraggingRef(null, null);
                craftingComp.setHeadAnchorRef(null);
                craftingComp.setDragTickCount(0);
                return InteractionState.Finished;
            }
            case NODE: {

                NodeComponent nodeComp = buffer.getComponent(draggedRef, NodeComponent.getComponentType());

                if (nodeComp != null && nodeComp.getParentGlyphRef() != null) {
                    TransformComponent playerTransform = buffer.getComponent(ref,
                            TransformComponent.getComponentType());

                    Ref<EntityStore> dropTargetRef = null;

                    if (playerTransform != null) {
                        List<Ref<EntityStore>> nearby = HoverableUtils.getNearbyHoverables(buffer,
                                playerTransform.getPosition(), 8.0);

                        nearby.remove(draggedRef);
                        dropTargetRef = HoverableUtils.getSmallestTarget(buffer, ref, nearby);
                    }

                    if (dropTargetRef == null || !dropTargetRef.isValid())
                        return InteractionState.Failed;

                    Ref<EntityStore> targetGlyphRef = resolveGlyphFromTarget(buffer, dropTargetRef);
                    Ref<EntityStore> sourceGlyphRef = nodeComp.getParentGlyphRef();

                    LOGGER.atInfo().log("node drop: target=%s (valid=%s), source=%s (valid=%s)",
                            targetGlyphRef, targetGlyphRef != null ? targetGlyphRef.isValid() : "null",
                            sourceGlyphRef, sourceGlyphRef != null ? sourceGlyphRef.isValid() : "null");

                    if (targetGlyphRef != null && targetGlyphRef.isValid()
                            && sourceGlyphRef != null && sourceGlyphRef.isValid()
                            && !targetGlyphRef.equals(sourceGlyphRef)) {
                        EffectComponent sourceEffect = buffer.getComponent(sourceGlyphRef,
                                EffectComponent.getComponentType());
                        EffectComponent targetEffect = buffer.getComponent(targetGlyphRef,
                                EffectComponent.getComponentType());
                        if (sourceEffect != null && targetEffect != null) {
                            sourceEffect.getGlyph().addNext(targetEffect.getId());
                            targetEffect.getGlyph().addPrevious(sourceEffect.getId());
                            LOGGER.atInfo().log("linked glyph %s to %s",
                                    sourceEffect.getId(), targetEffect.getId());
                        }
                    }
                }

                LOGGER.atInfo().log("Exiting interaction and setting hoveredRef to null");
                craftingComp.setDraggingRef(null, null);
                craftingComp.setDragTickCount(0);
                return InteractionState.Finished;
            }
        }
        return InteractionState.Finished;
    }

    private static void updateNodePosition(CommandBuffer<EntityStore> buffer,
            Ref<EntityStore> glyphRef, Vector3d glyphWorldPos) {
        EffectComponent effect = buffer.getComponent(glyphRef,
                EffectComponent.getComponentType());
        if (effect == null || effect.getNodeRef() == null)
            return;
        TransformComponent nodeTransform = buffer.getComponent(effect.getNodeRef(),
                TransformComponent.getComponentType());
        if (nodeTransform != null) {
            nodeTransform.setPosition(new Vector3d(
                    glyphWorldPos.x, glyphWorldPos.y + CraftingGlyphNodeSpawner.NODE_OFFSET_Y, glyphWorldPos.z));
        }
    }

    private static Ref<EntityStore> resolveGlyphFromTarget(CommandBuffer<EntityStore> buffer,
            Ref<EntityStore> targetRef) {
        HoverableComponent hoverComp = buffer.getComponent(targetRef,
                HoverableComponent.getComponentType());
        if (hoverComp == null)
            return null;

        switch (hoverComp.getType()) {
            case GLYPH:
                return targetRef;
            case NODE:
                NodeComponent targetNode = buffer.getComponent(targetRef,
                        NodeComponent.getComponentType());
                return targetNode != null ? targetNode.getParentGlyphRef() : null;
            case HEX:
                HexComponent hexComp = buffer.getComponent(targetRef,
                        HexComponent.getComponentType());
                if (hexComp != null && hexComp.getHex() != null) {
                    String firstId = hexComp.getHex().getFirstGlyphId();
                    if (firstId != null) {
                        return hexComp.getChildGlyphRef(firstId);
                    }
                }
                return null;
            default:
                return null;
        }
    }

    @Override
    public void onPlayerJoin(Holder<EntityStore> holder, HexcasterComponent comp) {
    }

    @Override
    public void onPlayerLeave(PlayerRef playerRef) {
    }
}
