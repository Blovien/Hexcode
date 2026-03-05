package com.riprod.hexcode.core.state.crafting.system;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.EffectComponent;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.common.hover.utils.HoverableUtils;
import com.riprod.hexcode.core.common.utilities.component.DebugComponent;
import com.riprod.hexcode.core.state.casting.utils.GlyphStyler;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.NodeComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.utils.CraftingDragUtil;
import com.riprod.hexcode.core.state.crafting.utils.DetailsRenderer;
import com.riprod.hexcode.core.state.crafting.utils.HoverStyleUtils;
import com.riprod.hexcode.core.state.crafting.utils.LinkRenderer;

public class CraftingStateSystem {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String HOVER_PARTICLE = "Object_Hover";

    public static InteractionState enterInteraction(Ref<EntityStore> ref, HexcasterComponent comp,
            CommandBuffer<EntityStore> buffer) {
        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null)
            return InteractionState.Failed;

        HoverableType hoveredType = craftingComp.getHoveredType();
        Ref<EntityStore> hoveredRef = craftingComp.getHoveredRef();
        if (hoveredRef == null || !hoveredRef.isValid()) {
            return InteractionState.Failed;
        }

        craftingComp.setDragTickCount(0);

        switch (hoveredType) {
            case GLYPH:
                Ref<EntityStore> headAnchor = CraftingDragUtil.startDrag(buffer, ref, hoveredRef);
                craftingComp.setHeadAnchorRef(headAnchor);
                craftingComp.setDraggingRef(hoveredRef, hoveredType);
                if (craftingComp.getDetailsGlyphRef() == null)
                    return InteractionState.NotFinished;

                DetailsRenderer.closeDetails(buffer, craftingComp.getDetailSlotRefs());
                craftingComp.setDetailsGlyphRef(null);
                return InteractionState.NotFinished;

            case NODE: {
                // reset details
                if (craftingComp.getDetailsGlyphRef() != null) {
                    DetailsRenderer.closeDetails(buffer, craftingComp.getDetailSlotRefs());
                    craftingComp.setDetailsGlyphRef(null);
                }

                // get if the node is allowed ti be dragged
                NodeComponent nodeComp = buffer.getComponent(hoveredRef,
                        NodeComponent.getComponentType());

                if (nodeComp == null || nodeComp.getParentGlyphRef() == null
                        || !nodeComp.getParentGlyphRef().isValid())
                    return InteractionState.Failed;

                EffectComponent parentEffect = buffer.getComponent(
                        nodeComp.getParentGlyphRef(), EffectComponent.getComponentType());
                if (parentEffect == null || !parentEffect.getGlyph().getPrevious().isEmpty())
                    return InteractionState.Failed;

                Ref<EntityStore> hexRootRef = craftingComp.getHexRootRef();

                if (hexRootRef == null)
                    return InteractionState.Failed;

                HexComponent hexComp = buffer.getComponent(hexRootRef,
                        HexComponent.getComponentType());

                if (hexComp == null)
                    return InteractionState.Failed;

                String firstId = hexComp.getHex().getFirstGlyphId();

                if (firstId == null || !parentEffect.getId().equals(firstId)) {
                    return InteractionState.Failed;
                }

                craftingComp.setDraggingRef(hoveredRef, hoveredType);
                return InteractionState.NotFinished;
            }
            default:
                return InteractionState.Failed;
        }

    }

    public static void tickInteraction(CommandBuffer<EntityStore> buffer, float dt, Ref<EntityStore> ref,
            PedestalBlockComponent pedestal) {
        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null)
            return;

        craftingComp.setDragTickCount(craftingComp.getDragTickCount() + 1);

        HoverableType draggingType = craftingComp.getDraggingType();

        if (draggingType == null) return;

        switch (draggingType) {
            case GLYPH: {
                CraftingDragUtil.updateDrag(buffer, craftingComp.getHeadAnchorRef(), ref);

                TransformComponent playerTransform = buffer.getComponent(ref,
                        TransformComponent.getComponentType());
                if (playerTransform != null) {
                    List<Ref<EntityStore>> nearby = HoverableUtils.getNearbyHoverables(buffer,
                            playerTransform.getPosition(), 8.0);
                    List<Ref<EntityStore>> filtered = new ArrayList<>(nearby.size());
                    for (Ref<EntityStore> candidate : nearby) {
                        if (!candidate.equals(craftingComp.getDraggingRef())) {
                            filtered.add(candidate);
                        }
                    }
                    HoverableUtils.getSmallestTarget(buffer, ref, filtered);
                }
            }
                break;
            case NODE: {

                TransformComponent nodeTransform = buffer.getComponent(
                        craftingComp.getDraggingRef(), TransformComponent.getComponentType());

                Transform look = TargetUtil.getLook(ref, buffer);
                Vector3d targetPoint = new Vector3d(
                        look.getPosition().x + look.getDirection().x * 5,
                        look.getPosition().y + look.getDirection().y * 5,
                        look.getPosition().z + look.getDirection().z * 5);

                if (nodeTransform != null) {
                    LinkRenderer.renderActiveLink(buffer, buffer.getExternalData().getWorld(),
                            nodeTransform.getPosition(), targetPoint,
                            new Vector3f(0.3f, 0.7f, 1.0f));
                }

                TransformComponent playerTransform = buffer.getComponent(ref,
                        TransformComponent.getComponentType());
                if (playerTransform != null) {
                    List<Ref<EntityStore>> nearby = HoverableUtils.getNearbyHoverables(buffer,
                            playerTransform.getPosition(), 8.0);
                    List<Ref<EntityStore>> filtered = new ArrayList<>(nearby.size());
                    for (Ref<EntityStore> candidate : nearby) {
                        if (!candidate.equals(craftingComp.getDraggingRef())) {
                            filtered.add(candidate);
                        }
                    }
                    HoverableUtils.getSmallestTarget(buffer, ref, filtered);
                }
            }
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
        Ref<EntityStore> targetRef = HoverableUtils.getSmallestTarget(accessor, ref, nearby);
        
        HoverableType hoverableType = null;
        if (targetRef != null && targetRef.isValid()) {
            HoverableComponent comp = accessor.getComponent(targetRef, HoverableComponent.getComponentType());
            hoverableType = comp.getType();
        }
        
        Ref<EntityStore> previousHovered = craftingComp.getHoveredRef();
        boolean changed = !Objects.equals(targetRef, previousHovered);
        
        if (changed) {
            
            HoverStyleUtils.unhover(accessor, previousHovered);
            
            craftingComp.setHoveredRef(targetRef, hoverableType);
            craftingComp.setRemoveWarning(false);
            craftingComp.setRemoveWarningRef(null);
            pedestal.setTickLength(HOVER_PARTICLE, 1f);

            HoverStyleUtils.hover(accessor, targetRef);
        }

        HoverStyleUtils.hoverParticles(accessor, previousHovered, dt, pedestal, ref);

        LinkRenderer.renderLinks(accessor, craftingComp, pedestal, dt);
    }
}
