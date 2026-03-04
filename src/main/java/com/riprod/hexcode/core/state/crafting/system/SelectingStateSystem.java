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
import com.riprod.hexcode.core.common.hover.utils.HoverableUtils;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalBlockComponent;
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

        Ref<EntityStore> hoveredRef = craftingComp.getHoveredHexRef();
        LOGGER.atInfo().log("crafting enterInteraction: hoveredRef=%s", hoveredRef);
        if (hoveredRef == null || !hoveredRef.isValid()) {
            return InteractionState.NotFinished;
        }

        HexComponent hexComp = buffer.getComponent(hoveredRef, HexComponent.getComponentType());
        if (hexComp == null || hexComp.getHex() == null) {
            return InteractionState.NotFinished;
        }

        pedestal.setActiveHex(hexComp.getHex());
        PedestalSystem.ActivateHexSelection(buffer, pedestal, hoveredRef);
        ObeliskSystem.ActivateHexSelection(buffer, pedestal, hoveredRef);
        craftingComp.setHoveredHexRef(null);

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

        List<Ref<EntityStore>> nearbyHoverables = HoverableUtils.getNearbyHoverables(buffer, playerTransform.getPosition(), 8);
        Ref<EntityStore> targetRef = HoverableUtils.getSmallestTarget(buffer, ref, nearbyHoverables);
        Ref<EntityStore> hoveredRef = resolveHoveredPreview(targetRef, previewRefs, buffer);

        Ref<EntityStore> previousHovered = craftingComp.getHoveredHexRef();
        boolean changed = (hoveredRef == null) != (previousHovered == null)
                || (hoveredRef != null && !hoveredRef.equals(previousHovered));
        if (changed) {
            LOGGER.atInfo().log("crafting: hover changed from %s to %s", previousHovered, hoveredRef);
            pedestal.setTickLength(HOVER_PARTICLE, 0f);
        }

        craftingComp.setHoveredHexRef(hoveredRef);

        if (pedestal.getTickLength(HOVER_PARTICLE) < 0.5f) {
            pedestal.incrementTickLength(HOVER_PARTICLE, dt);
            return;
        }
        pedestal.setTickLength(HOVER_PARTICLE, 0f);

        if (hoveredRef != null && hoveredRef.isValid()) {
            TransformComponent transform = buffer.getComponent(hoveredRef, TransformComponent.getComponentType());
            if (transform != null) {
                Vector3d pos = transform.getPosition();
                ParticleUtil.spawnParticleEffect(HOVER_PARTICLE, pos, hoveredRef, List.of(ref), buffer);
            }
        }
    }

    public static Ref<EntityStore> resolveHoveredPreview(Ref<EntityStore> targetRef,
            List<Ref<EntityStore>> previewRefs, CommandBuffer<EntityStore> buffer) {
        if (targetRef == null || !targetRef.isValid()) {
            return null;
        }

        for (Ref<EntityStore> previewRef : previewRefs) {
            if (previewRef != null && previewRef.equals(targetRef)) {
                return targetRef;
            }
        }

        EffectComponent glyphComp = buffer.getComponent(targetRef, EffectComponent.getComponentType());
        if (glyphComp != null) {
            Ref<EntityStore> hexRef = glyphComp.getHexRef();
            if (hexRef != null && hexRef.isValid()) {
                for (Ref<EntityStore> previewRef : previewRefs) {
                    if (previewRef != null && previewRef.equals(hexRef)) {
                        return hexRef;
                    }
                }
            }
        }

        return null;
    }
}
