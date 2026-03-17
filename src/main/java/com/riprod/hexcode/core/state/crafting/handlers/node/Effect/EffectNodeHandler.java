package com.riprod.hexcode.core.state.crafting.handlers.node.Effect;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.glyphs.utils.CreateGlyph;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hidden.utils.HiddenUtils;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.common.hover.utils.HoverableUtils;
import com.riprod.hexcode.core.common.utilities.component.DebugComponent;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.NodeComponent;
import com.riprod.hexcode.core.state.crafting.component.PedestalDataComponent;
import com.riprod.hexcode.core.state.crafting.constants.CraftingColors;
import com.riprod.hexcode.core.state.crafting.constants.NodeType;
import com.riprod.hexcode.core.state.crafting.handlers.CraftingDragHandler;
import com.riprod.hexcode.core.state.crafting.handlers.CraftingDropHandler;
import com.riprod.hexcode.core.state.crafting.handlers.DetailsHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeInterface;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeRouter;
import com.riprod.hexcode.core.state.crafting.handlers.node.Glyph.GlyphNodeHandler;
import com.riprod.hexcode.core.state.crafting.utils.CraftingPositionUtil;
import com.riprod.hexcode.core.state.crafting.utils.PedestalDataUtil;
import com.riprod.hexcode.utils.CleanupUtils;

public class EffectNodeHandler implements NodeInterface {
    public static final EffectNodeHandler INSTANCE = new EffectNodeHandler();

    @Override
    public InteractionState enter(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'enter'");
    }

    @Override
    public InteractionState tick(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'tick'");
    }

    @Override
    public InteractionState exit(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef) {

        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(accessor, playerRef);

        TransformComponent playerTransform = accessor.getComponent(playerRef,
                TransformComponent.getComponentType());
        Ref<EntityStore> dropTargetRef = null;
        if (playerTransform != null) {
            List<Ref<EntityStore>> nearby = HoverableUtils.getNearbyHoverables(accessor,
                    playerTransform.getPosition(), 8.0);

            HiddenUtils.filterByOwner(accessor, nearby, playerRef);
            List<Ref<EntityStore>> filtered = new ArrayList<>(nearby.size());
            for (Ref<EntityStore> candidate : nearby) {
                if (!candidate.equals(nodeRef)) {
                    filtered.add(candidate);
                }
            }
            dropTargetRef = HoverableUtils.getSmallestTarget(accessor, playerRef, filtered);
        }

        CraftingDropHandler.DropResult dResult = CraftingDropHandler.handleDrop(
                accessor, nodeRef, dropTargetRef);

        Vector3f dropOffset = CraftingPositionUtil.lookToHexOffset(accessor, playerRef,
                playerData.getAnchorNodeRef(), 2.0f);
        Vector3d dropWorldPos = CraftingPositionUtil.hexOffsetToWorld(accessor,
                playerData.getAnchorNodeRef(), dropOffset);

        GlyphComponent effect = accessor.getComponent(nodeRef,
                GlyphComponent.getComponentType());
        Vector3f rotation = effect != null ? effect.getRotation() : Vector3f.ZERO;

        accessor.putComponent(nodeRef, TransformComponent.getComponentType(),
                new TransformComponent(dropWorldPos, rotation));

        if (effect != null) {
            effect.getGlyph().setPosition(dropOffset);
        } else {
            TransformComponent nodeTransform = accessor.getComponent(effect.getNodeRef(),
                    TransformComponent.getComponentType());
            if (nodeTransform != null) {
                nodeTransform.setPosition(new Vector3d(dropWorldPos));
            }
        }

        return InteractionState.Finished;
    }

    @Override
    public InteractionState click(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef) {

        PedestalDataComponent playerData = PedestalDataUtil.getPedestalData(accessor, playerRef);

        if (DetailsHandler.isOpenFor(accessor, playerData.getSlotNodeRefs(), nodeRef)) {
            DetailsHandler.closeDetails(accessor, playerData.getSlotNodeRefs());
            playerData.setSlotNodeRefs(null);
        } else {
            if (playerData.getSlotNodeRefs() != null
                    && !playerData.getSlotNodeRefs().isEmpty()) {
                DetailsHandler.closeDetails(accessor, playerData.getSlotNodeRefs());
            }
            List<Ref<EntityStore>> slotRefs = DetailsHandler.openDetails(
                    accessor, nodeRef, playerRef);
            playerData.setSlotNodeRefs(slotRefs);
        }

        // remove the head component if it's still there, since this is just a click,
        // not a drag
        if (nodeRef != null && nodeRef.isValid()) {
            accessor.tryRemoveComponent(nodeRef, MountedComponent.getComponentType());
        }

        return InteractionState.Finished;
    }

    public Ref<EntityStore> spawnNode(CommandBuffer<EntityStore> accessor, Ref<EntityStore> parentRef,
            Vector3d position, Ref<EntityStore> playerRef, GlyphComponent glyphComp) {

        Holder<EntityStore> glyphHolder = CreateGlyph.createGlyphHolder(accessor, glyphComp, position);
        HiddenUtils.addHiddenToHolder(accessor, glyphHolder, playerRef);
        Ref<EntityStore> glyphNodeRef = accessor.addEntity(glyphHolder, AddReason.SPAWN);

        Holder<EntityStore> glyphNode = GlyphNodeHandler.INSTANCE.spawnNode(accessor, glyphNodeRef, position,
                playerRef);
        glyphHolder.addComponent(HoverableComponent.getComponentType(),
                new HoverableComponent(HoverableType.GLYPH));

        Ref<EntityStore> rootNodeRef = accessor.addEntity(glyphNode, AddReason.SPAWN);
        glyphComp.setNodeRef(rootNodeRef);

        glyphComp.setHexRef(parentRef);

        glyphComp.setParentRef(playerRef);


        return glyphNodeRef;
    }

    @Override
    public void despawn(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'despawn'");
    }

    @Override
    public void hover(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
        DebugComponent debug = accessor.getComponent(nodeRef, DebugComponent.getComponentType());
        if (debug == null)
            return;
        debug.setScaleMultiplier(1.3f);
        debug.setIntervalMultiplier(0.25f);
        debug.setFadeMultiplier(0.25f);
        debug.setTimer(0);
    }

    @Override
    public void unhover(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
        DebugComponent debug = accessor.getComponent(nodeRef, DebugComponent.getComponentType());
        if (debug == null)
            return;
        debug.resetScaleMultiplier();
        debug.resetFadeMultipler();
        debug.resetIntervalMultiplier();
    }

    @Override
    public InteractionState ability(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            InteractionType inputType, Ref<EntityStore> playerRef) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'ability'");
    }

}
