package com.riprod.hexcode.core.state.crafting.system;

import java.util.List;

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
import com.riprod.hexcode.core.state.crafting.constants.PedestalState;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeRouter;
import com.riprod.hexcode.core.state.crafting.utils.HoverStyleUtils;
import com.riprod.hexcode.core.state.crafting.utils.PedestalDataUtil;

public class SelectingStateSystem {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String HOVER_PARTICLE = "Object_Hover";

    public static InteractionState enterInteraction(Ref<EntityStore> playerRef, HexcasterComponent comp,
            CommandBuffer<EntityStore> buffer) {
        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(buffer, playerRef);
        if (playerData == null)
            return InteractionState.Failed;

        if (playerData.getState() != PedestalState.SELECTING)
            return InteractionState.Finished;

        HexcasterCraftingComponent craftingComp = buffer.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null)
            return InteractionState.Failed;

        Ref<EntityStore> hoveredRef = craftingComp.getHoveredRef();
        if (hoveredRef == null || !hoveredRef.isValid())
            return InteractionState.Failed;

        return NodeRouter.enter(buffer, hoveredRef, playerRef);
    }

    public static void tickSelecting(CommandBuffer<EntityStore> accessor, float dt, Ref<EntityStore> ref,
            PedestalBlockComponent pedestal, PedestalDataComponent playerData) {
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
            HoverStyleUtils.unhover(accessor, previousHovered, ref);
        }

        if (targetRef == null || !targetRef.isValid()) {
            craftingComp.setHoveredRef(null);
            return;
        }

        craftingComp.setHoveredRef(targetRef);
        HoverStyleUtils.hover(accessor, targetRef, ref);

        HoverStyleUtils.hoverParticles(accessor, targetRef, dt, pedestal, ref);
    }
}
