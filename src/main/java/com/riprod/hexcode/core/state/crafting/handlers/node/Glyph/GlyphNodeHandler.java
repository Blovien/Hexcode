package com.riprod.hexcode.core.state.crafting.handlers.node.Glyph;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.glyphs.component.Slot;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.utils.CreateGlyph;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.common.pedestal.component.PedestalBlockComponent;
import com.riprod.hexcode.core.common.pedestal.utils.PedestalBlockUtil;
import com.riprod.hexcode.core.state.casting.utils.GlyphStyler;
import com.riprod.hexcode.core.state.crafting.component.CraftingData;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.NodeComponent;
import com.riprod.hexcode.core.state.crafting.constants.NodeType;
import com.riprod.hexcode.core.state.crafting.handlers.CraftingDragHandler;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeInterface;
import com.riprod.hexcode.core.state.crafting.handlers.node.Slot.SlotNodeHandler;
import com.riprod.hexcode.core.state.crafting.utils.CraftingPositionUtil;

public class GlyphNodeHandler implements NodeInterface {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final GlyphNodeHandler INSTANCE = new GlyphNodeHandler();

    @Override
    public InteractionState enter(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef) {
        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null) return InteractionState.Failed;

        Ref<EntityStore> headAnchor = CraftingDragHandler.startDrag(accessor, playerRef, node);
        craftingComp.setHeadAnchorRef(accessor, headAnchor);
        craftingComp.setDraggingRef(node);
        return InteractionState.Finished;
    }

    @Override
    public InteractionState tick(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef) {
        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null) return InteractionState.Failed;

        CraftingDragHandler.updateDrag(accessor, craftingComp.getHeadAnchorRef(), playerRef);
        return InteractionState.Finished;
    }

    @Override
    public InteractionState exit(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef) {
        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null) return InteractionState.Finished;

        PedestalBlockComponent blockComp = PedestalBlockUtil.resolvePedestal(playerRef, accessor);
        if (blockComp == null) return InteractionState.Finished;

        CraftingData playerData = blockComp.getCraftingDataComponent();
        if (playerData == null) return InteractionState.Finished;

        Vector3f dropOffset = CraftingPositionUtil.lookToHexOffset(accessor, playerRef,
                playerData.getAnchorNodeRef(), 2.0f);
        Vector3d dropWorldPos = CraftingPositionUtil.hexOffsetToWorld(accessor,
                playerData.getAnchorNodeRef(), dropOffset);

        GlyphComponent glyphComp = accessor.getComponent(nodeRef, GlyphComponent.getComponentType());
        if (glyphComp == null) return InteractionState.Finished;

        Ref<EntityStore> headAnchorRef = craftingComp.getHeadAnchorRef();
        TransformComponent headTransform = accessor.getComponent(headAnchorRef, TransformComponent.getComponentType());
        if (headTransform == null) return InteractionState.Finished;

        Vector3f playerRotation = headTransform.getRotation();
        TransformComponent nodeTransform = accessor.getComponent(nodeRef, TransformComponent.getComponentType());
        if (nodeTransform == null) return InteractionState.Finished;

        nodeTransform.getPosition().assign(dropWorldPos);
        nodeTransform.getRotation().assign(playerRotation);

        glyphComp.setOffset(dropOffset);
        glyphComp.setRotation(playerRotation);
        return InteractionState.Finished;
    }

    @Override
    public InteractionState click(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef) {
        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp != null) {
            Ref<EntityStore> headAnchorRef = craftingComp.getHeadAnchorRef();
            if (headAnchorRef != null && headAnchorRef.isValid()) {
                craftingComp.setHeadAnchorRef(accessor, null);
            }
        }

        GlyphComponent glyphComp = accessor.getComponent(nodeRef, GlyphComponent.getComponentType());
        if (glyphComp == null) return InteractionState.Failed;

        if (glyphComp.areSlotsVisible()) {
            SlotNodeHandler.INSTANCE.despawnSlotsForGlyph(accessor, nodeRef);
            glyphComp.setSlotsVisible(false);
        } else {
            glyphComp.setSlotsVisible(true);
            SlotNodeHandler.INSTANCE.spawnSlotsForGlyph(accessor, nodeRef, playerRef);
        }

        // reset position so the click doesn't accidentally drag the glyph
        resetGlyphTransform(accessor, nodeRef, playerRef, glyphComp);
        return InteractionState.Finished;
    }

    private void resetGlyphTransform(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef, GlyphComponent glyphComp) {
        PedestalBlockComponent blockComp = PedestalBlockUtil.resolvePedestal(playerRef, accessor);
        if (blockComp == null) return;
        CraftingData playerData = blockComp.getCraftingDataComponent();
        if (playerData == null) return;

        HeadRotation headRot = accessor.getComponent(playerRef, HeadRotation.getComponentType());
        if (headRot == null) return;

        Vector3d dropWorldPos = CraftingPositionUtil.hexOffsetToWorld(accessor,
                playerData.getAnchorNodeRef(), glyphComp.getOffset());
        TransformComponent transform = accessor.getComponent(nodeRef, TransformComponent.getComponentType());
        if (transform == null) return;

        transform.getPosition().assign(dropWorldPos);
        transform.getRotation().assign(headRot.getRotation());
    }

    @Override
    public InteractionState ability(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            InteractionType inputType, Ref<EntityStore> playerRef) {
        if (inputType != InteractionType.Ability3) return InteractionState.Failed;

        GlyphComponent glyphComp = accessor.getComponent(nodeRef, GlyphComponent.getComponentType());
        if (glyphComp == null) return InteractionState.Failed;

        Glyph glyph = glyphComp.getGlyph();
        boolean hasAnyLink = glyph.getSlots().values().stream()
                .anyMatch(s -> s.getLinks().length > 0);

        if (hasAnyLink) {
            glyph.clearAllSlots();
            return InteractionState.Finished;
        }

        // step 2: delete the glyph entirely
        return deleteGlyph(accessor, nodeRef, playerRef, glyphComp);
    }

    private InteractionState deleteGlyph(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef, GlyphComponent glyphComp) {
        Ref<EntityStore> hexEntityRef = glyphComp.getHexRef();
        HexComponent hexComp = hexEntityRef != null
                ? accessor.getComponent(hexEntityRef, HexComponent.getComponentType())
                : null;

        String glyphId = glyphComp.getId();
        if (hexComp != null) {
            hexComp.getHex().removeGlyph(glyphId);
            hexComp.removeChildGlyph(glyphId);
        }

        SlotNodeHandler.INSTANCE.despawnSlotsForGlyph(accessor, nodeRef);
        accessor.tryRemoveEntity(nodeRef, RemoveReason.REMOVE);
        LOGGER.atInfo().log("glyph node: deleted glyph %s", glyphId);
        return InteractionState.Finished;
    }

    public Ref<EntityStore> spawnNode(CommandBuffer<EntityStore> accessor, Ref<EntityStore> parentRef,
            Vector3d position, Ref<EntityStore> playerRef, GlyphComponent glyphComp,
            Ref<EntityStore> hexEntityRef) {
        Glyph glyph = glyphComp.getGlyph();
        Vector3f glyphRot = new Vector3f(glyph.getRotation().getPitch(), glyph.getRotation().getYaw(), 0);
        Holder<EntityStore> glyphHolder = CreateGlyph.createGlyphHolder(accessor, glyphComp, position, glyphRot);

        HoverableComponent hoverComp = new HoverableComponent(HoverableType.NODE);
        try {
            GlyphAsset glyphAsset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
            if (glyphAsset != null) {
                hoverComp.setHintText("title", glyphAsset.getTitle());
                hoverComp.setHintText("description", glyphAsset.getDescription());
                hoverComp.setHintText("extra", "V " + Math.round(glyph.getVolatility() * 100.0) / 100.0
                        + " | E " + Math.round(glyph.getEfficiency() * 100.0) / 100.0);
            }
        } catch (Exception e) {
            LOGGER.atWarning().log("glyph node: failed to set hover hints: %s", e.getMessage());
        }

        glyphHolder.addComponent(HoverableComponent.getComponentType(), hoverComp);
        glyphHolder.addComponent(NodeComponent.getComponentType(),
                new NodeComponent(hexEntityRef, NodeType.Glyph));

        Ref<EntityStore> glyphNodeRef = accessor.addEntity(glyphHolder, AddReason.SPAWN);
        glyphComp.setSelfRef(glyphNodeRef);
        glyphComp.setHexRef(hexEntityRef);
        glyphComp.setParentRef(hexEntityRef);

        HexComponent hexComp = accessor.getComponent(hexEntityRef, HexComponent.getComponentType());
        if (hexComp != null) {
            hexComp.addChildGlyphRef(glyph.getId(), glyphNodeRef);
        }

        return glyphNodeRef;
    }

    @Override
    public void despawn(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
        GlyphComponent glyphComp = accessor.getComponent(nodeRef, GlyphComponent.getComponentType());
        if (glyphComp == null) return;

        Ref<EntityStore> hexEntityRef = glyphComp.getHexRef();
        if (hexEntityRef != null) {
            HexComponent hexComp = accessor.getComponent(hexEntityRef, HexComponent.getComponentType());
            if (hexComp != null) {
                hexComp.getHex().removeGlyph(glyphComp.getId());
                hexComp.removeChildGlyph(glyphComp.getId());
            }
        }

        SlotNodeHandler.INSTANCE.despawnSlotsForGlyph(accessor, nodeRef);
        accessor.tryRemoveEntity(nodeRef, RemoveReason.REMOVE);
    }

    @Override
    public void hover(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
        GlyphComponent glyphComp = accessor.getComponent(nodeRef, GlyphComponent.getComponentType());
        if (glyphComp == null) return;
        GlyphStyler.enterGlyphHover(accessor, glyphComp);
    }

    @Override
    public void unhover(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
        GlyphComponent glyphComp = accessor.getComponent(nodeRef, GlyphComponent.getComponentType());
        if (glyphComp == null) return;
        GlyphStyler.exitGlyphHover(accessor, glyphComp);
    }
}
