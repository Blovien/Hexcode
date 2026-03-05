package com.riprod.hexcode.core.state.crafting.system;

import java.util.List;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.EffectComponent;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.common.hover.utils.HoverableUtils;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.entity.PedestalEntity;
import com.riprod.hexcode.core.state.crafting.utils.CraftingGlyphSpawner;
import com.riprod.hexcode.core.state.crafting.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.state.crafting.utils.PedestalState;

public class SelectingStateSystem {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String HOVER_PARTICLE = "Object_Hover";

    public static InteractionState enterInteraction(Ref<EntityStore> ref, HexcasterComponent comp,
            CommandBuffer<EntityStore> buffer) {
        PedestalBlockComponent pedestal = PedestalBlockUtil.resolvePedestal(ref, buffer);
        if (pedestal == null) {
            return InteractionState.Failed;
        }

        if (pedestal.getState() != PedestalState.SELECTING) {
            return InteractionState.NotFinished;
        }

        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null) {
            return InteractionState.Failed;
        }

        HoverableType hoverableType = craftingComp.getHoveredType();
        Ref<EntityStore> hoveredRef = craftingComp.getHoveredRef();
        if (hoveredRef == null || !hoveredRef.isValid()) {
            return InteractionState.NotFinished;
        }

        switch (hoverableType) {
            case HEX: {
                HexComponent hexComp = buffer.getComponent(hoveredRef, HexComponent.getComponentType());
                if (hexComp == null || hexComp.getHex() == null) {
                    return InteractionState.NotFinished;
                }

                pedestal.setActiveHex(hexComp.getHex());
                PedestalSystem.ActivateHexSelection(buffer, pedestal, hoveredRef);
                ObeliskSystem.ActivateHexSelection(buffer, pedestal, hoveredRef);
                craftingComp.setHoveredRef(null, null);

                craftingComp.setHexRootRef(hoveredRef);
                Vector3d anchorPos = PedestalEntity.getAnchorPosition(pedestal.getLocation());
                Vector3d activePos = new Vector3d(
                        anchorPos.x + PedestalSystem.ACTIVE_HEX_OFFSET.x,
                        anchorPos.y + PedestalSystem.ACTIVE_HEX_OFFSET.y,
                        anchorPos.z + PedestalSystem.ACTIVE_HEX_OFFSET.z);
                CraftingGlyphSpawner.spawnCraftingGlyphs(buffer, hexComp, activePos);
            }
                break;
            case CONTAINER: {
                // handle selection of an empty node
            }
        }
        return InteractionState.Finished;
    }

    public static void tickSelecting(CommandBuffer<EntityStore> buffer, float dt, Ref<EntityStore> ref,
            PedestalBlockComponent pedestal) {
        List<Ref<EntityStore>> previewRefs = pedestal.getHexPreviewRefs();

        if (previewRefs == null || previewRefs.isEmpty()) {
            return;
        }

        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null) {
            return;
        }

        List<Ref<EntityStore>> hexGlyphs = pedestal.getHexPreviewRefs();

        if (hexGlyphs.isEmpty())
            return;

        TransformComponent playerTransform = buffer.getComponent(ref, TransformComponent.getComponentType());

        List<Ref<EntityStore>> nearbyHoverables = HoverableUtils.getNearbyHoverables(buffer,
                playerTransform.getPosition(), 8);

        Ref<EntityStore> targetRef = HoverableUtils.getSmallestTarget(buffer, ref, nearbyHoverables);

        Ref<EntityStore> previousHovered = craftingComp.getHoveredRef();

        boolean changed = (targetRef == null) != (previousHovered == null)
                || (targetRef != null && !targetRef.equals(previousHovered));
        if (changed) {
            pedestal.setTickLength(HOVER_PARTICLE, 0f);
        }

        if (targetRef == null || !targetRef.isValid()) {
            craftingComp.setHoveredRef(null, null);
            return;
        }

        HoverableComponent targetRefHoverableComponent = buffer.getComponent(targetRef,
                HoverableComponent.getComponentType());
        if (targetRefHoverableComponent == null)
            return; // shouldn't be reachable
        HoverableType hoveredType = targetRefHoverableComponent.getType();

        craftingComp.setHoveredRef(targetRef, hoveredType);

        if (pedestal.getTickLength(HOVER_PARTICLE) < 0.5f) {
            pedestal.incrementTickLength(HOVER_PARTICLE, dt);
            return;
        }
        pedestal.setTickLength(HOVER_PARTICLE, 0f);

        if (targetRef != null && targetRef.isValid()) {
            TransformComponent transform = buffer.getComponent(targetRef, TransformComponent.getComponentType());
            if (transform != null) {
                Vector3d pos = transform.getPosition();
                ParticleUtil.spawnParticleEffect(HOVER_PARTICLE, pos, targetRef, List.of(ref), buffer);
            }
        }
    }
}
