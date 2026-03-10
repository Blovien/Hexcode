package com.riprod.hexcode.core.state.crafting.system;

import java.util.List;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.RemoveReason;
import com.riprod.hexcode.core.common.glyphs.component.EffectComponent;
import com.riprod.hexcode.core.common.hexcaster.component.HexcasterComponent;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hidden.utils.HiddenUtils;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.common.hover.utils.HoverableUtils;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalPlayerData;
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;
import com.riprod.hexcode.core.state.crafting.entity.PedestalEntity;
import com.riprod.hexcode.core.common.hexes.component.Hex;
import com.riprod.hexcode.core.state.crafting.utils.CraftingGlyphNodeSpawner;
import com.riprod.hexcode.core.state.crafting.utils.CraftingGlyphSpawner;
import com.riprod.hexcode.core.state.crafting.utils.HoverStyleUtils;
import com.riprod.hexcode.core.state.crafting.utils.PedestalBlockUtil;

import java.util.Map;

public class SelectingStateSystem {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String HOVER_PARTICLE = "Object_Hover";

    public static InteractionState enterInteraction(Ref<EntityStore> ref, HexcasterComponent comp,
            CommandBuffer<EntityStore> buffer) {
        PedestalBlockComponent pedestal = PedestalBlockUtil.resolvePedestal(ref, buffer);
        if (pedestal == null) {
            return InteractionState.Failed;
        }

        UUIDComponent uuidComp = buffer.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComp == null) {
            return InteractionState.Failed;
        }
        String playerId = uuidComp.getUuid().toString();
        PedestalPlayerData playerData = pedestal.getPlayerData(playerId);
        if (playerData == null) {
            return InteractionState.Failed;
        }

        if (playerData.getState() != PedestalState.SELECTING) {
            return InteractionState.Finished;
        }

        HexcasterCraftingComponent craftingComp = buffer.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null) {
            return InteractionState.Failed;
        }

        HoverableType hoverableType = craftingComp.getHoveredType();
        Ref<EntityStore> hoveredRef = craftingComp.getHoveredRef();
        if (hoveredRef == null || !hoveredRef.isValid()) {
            return InteractionState.Failed;
        }
        Ref<EntityStore> playerRefFlag = pedestal.isPerPlayer() ? ref : null;

        switch (hoverableType) {
            case HEX: {
                HexComponent hexComp = buffer.getComponent(hoveredRef, HexComponent.getComponentType());
                if (hexComp == null || hexComp.getHex() == null) {
                    return InteractionState.Failed;
                }

                playerData.setActiveHex(hexComp.getHex());
                PedestalSystem.enterCrafting(buffer, ref, pedestal, playerData, hoveredRef);
                ObeliskSystem.enterCrafting(buffer, pedestal, hoveredRef);
                playerData.setState(PedestalState.CRAFTING);
                craftingComp.setHoveredRef(null, null);

                craftingComp.setHexRootRef(hoveredRef);

                // despawn old preview child glyphs before spawning crafting glyphs
                Map<String, Ref<EntityStore>> oldChildren = hexComp.getChildGlyphRefs();
                if (oldChildren != null) {
                    for (Ref<EntityStore> childRef : oldChildren.values()) {
                        if (childRef == null || !childRef.isValid())
                            continue;
                        EffectComponent effect = buffer.getComponent(childRef, EffectComponent.getComponentType());
                        if (effect != null && effect.getNodeRef() != null && effect.getNodeRef().isValid()) {
                            buffer.removeEntity(effect.getNodeRef(), RemoveReason.REMOVE);
                        }
                        buffer.removeEntity(childRef, RemoveReason.REMOVE);
                    }
                    oldChildren.clear();
                }


                Vector3d anchorPos = PedestalEntity.getAnchorPosition(pedestal.getLocation());
                Vector3d activePos = new Vector3d(
                        anchorPos.x + PedestalSystem.ACTIVE_HEX_OFFSET.x,
                        anchorPos.y + PedestalSystem.ACTIVE_HEX_OFFSET.y,
                        anchorPos.z + PedestalSystem.ACTIVE_HEX_OFFSET.z);
                CraftingGlyphSpawner.spawnCraftingGlyphs(buffer, hexComp,
                        activePos, playerRefFlag);

                Ref<EntityStore> rootNodeRef = CraftingGlyphNodeSpawner.spawnRootNode(buffer, activePos, playerRefFlag);
                craftingComp.setRootNodeRef(rootNodeRef);
            }
                break;
            case CONTAINER: {
                HexComponent hexComp = buffer.getComponent(hoveredRef, HexComponent.getComponentType());
                if (hexComp == null) {
                    hexComp = new HexComponent(new Hex());
                    buffer.putComponent(hoveredRef, HexComponent.getComponentType(), hexComp);
                }
                hexComp.setSelfRef(hoveredRef);

                playerData.setActiveHex(hexComp.getHex());
                PedestalSystem.enterCrafting(buffer, ref, pedestal, playerData, hoveredRef);
                ObeliskSystem.enterCrafting(buffer, pedestal, hoveredRef);
                craftingComp.setHoveredRef(null, null);
                craftingComp.setHexRootRef(hoveredRef);

                Vector3d anchorPos = PedestalEntity.getAnchorPosition(pedestal.getLocation());
                Vector3d activePos = new Vector3d(
                        anchorPos.x + PedestalSystem.ACTIVE_HEX_OFFSET.x,
                        anchorPos.y + PedestalSystem.ACTIVE_HEX_OFFSET.y,
                        anchorPos.z + PedestalSystem.ACTIVE_HEX_OFFSET.z);

                Ref<EntityStore> rootNodeRef = CraftingGlyphNodeSpawner.spawnRootNode(buffer, activePos, playerRefFlag);
                craftingComp.setRootNodeRef(rootNodeRef);
            }
        }
        return InteractionState.Finished;
    }

    public static void tickSelecting(CommandBuffer<EntityStore> accessor, float dt, Ref<EntityStore> ref,
            PedestalBlockComponent pedestal, PedestalPlayerData playerData) {
        List<Ref<EntityStore>> previewRefs = playerData.getHexPreviewRefs();

        if (previewRefs == null || previewRefs.isEmpty()) {
            return;
        }

        HexcasterCraftingComponent craftingComp = accessor.getComponent(ref,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null) {
            return;
        }

        List<Ref<EntityStore>> hexGlyphs = playerData.getHexPreviewRefs();

        if (hexGlyphs.isEmpty())
            return;

        TransformComponent playerTransform = accessor.getComponent(ref, TransformComponent.getComponentType());
        if (playerTransform == null)
            return;

        List<Ref<EntityStore>> nearby = HoverableUtils.getNearbyHoverables(accessor,
                playerTransform.getPosition(), 8);

        HiddenUtils.filterByOwner(accessor, nearby, ref);

        Ref<EntityStore> targetRef = HoverableUtils.getSmallestTarget(accessor, ref, nearby);

        Ref<EntityStore> previousHovered = craftingComp.getHoveredRef();

        boolean changed = (targetRef == null) != (previousHovered == null)
                || (targetRef != null && !targetRef.equals(previousHovered));
        if (changed) {
            HoverStyleUtils.unhover(accessor, previousHovered);
        }

        if (targetRef == null || !targetRef.isValid()) {
            craftingComp.setHoveredRef(null, null);
            return;
        }

        HoverableComponent targetRefHoverableComponent = accessor.getComponent(targetRef,
                HoverableComponent.getComponentType());
        if (targetRefHoverableComponent == null)
            return; // shouldn't be reachable
        HoverableType hoveredType = targetRefHoverableComponent.getType();

        craftingComp.setHoveredRef(targetRef, hoveredType);
        HoverStyleUtils.hover(accessor, targetRef);

        HoverStyleUtils.hoverParticles(accessor, targetRef, dt, pedestal, ref);
    }
}
