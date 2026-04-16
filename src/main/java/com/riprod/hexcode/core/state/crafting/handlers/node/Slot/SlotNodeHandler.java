package com.riprod.hexcode.core.state.crafting.handlers.node.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.glyphs.component.Slot;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.registry.SlotAsset;
import com.riprod.hexcode.core.common.hexes.component.HexComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.common.hover.utils.HoverableUtils;
import com.riprod.hexcode.core.common.utilities.component.DebugComponent;
import com.riprod.hexcode.core.state.crafting.component.HexcasterCraftingComponent;
import com.riprod.hexcode.core.state.crafting.component.NodeComponent;
import com.riprod.hexcode.core.state.crafting.component.SlotComponent;
import com.riprod.hexcode.core.state.crafting.constants.CraftingColors;
import com.riprod.hexcode.core.state.crafting.constants.NodeType;
import com.riprod.hexcode.core.state.crafting.handlers.node.NodeInterface;
import com.riprod.hexcode.core.state.crafting.utils.LinkRenderer;
import com.riprod.hexcode.core.state.crafting.utils.RadialPositionUtil;

public class SlotNodeHandler implements NodeInterface {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final SlotNodeHandler INSTANCE = new SlotNodeHandler();

    private static final double SLOT_SCALE = 0.2;
    private static final float SLOT_RESPAWN_INTERVAL = 2.0f;
    private static final float SLOT_RADIUS = 0.6f;

    @Override
    public InteractionState enter(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef) {
        NodeComponent nodeComp = accessor.getComponent(node, NodeComponent.getComponentType());
        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        if (nodeComp == null || craftingComp == null) return InteractionState.Failed;

        Ref<EntityStore> parentRef = nodeComp.getParentEntity();
        if (parentRef == null || !parentRef.isValid()) return InteractionState.Failed;

        craftingComp.setDraggingRef(node);
        return InteractionState.Finished;
    }

    @Override
    public InteractionState tick(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef) {
        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null) return InteractionState.Failed;

        TransformComponent nodeTransform = accessor.getComponent(
                craftingComp.getDraggingRef(), TransformComponent.getComponentType());
        if (nodeTransform == null) return InteractionState.Finished;

        Transform look = TargetUtil.getLook(playerRef, accessor);
        Vector3d targetPoint = new Vector3d(
                look.getPosition().x + look.getDirection().x * 2,
                look.getPosition().y + look.getDirection().y * 2,
                look.getPosition().z + look.getDirection().z * 2);

        Vector3f color = resolveActiveLinkColor(accessor, node);
        LinkRenderer.renderActiveLink(accessor, accessor.getExternalData().getWorld(),
                nodeTransform.getPosition(), targetPoint, color);
        return InteractionState.Finished;
    }

    private Vector3f resolveActiveLinkColor(CommandBuffer<EntityStore> accessor, Ref<EntityStore> slotRef) {
        SlotComponent slotComp = accessor.getComponent(slotRef, SlotComponent.getComponentType());
        NodeComponent nodeComp = accessor.getComponent(slotRef, NodeComponent.getComponentType());
        if (slotComp == null || nodeComp == null) return CraftingColors.GLYPH_LINK;

        Ref<EntityStore> parentRef = nodeComp.getParentEntity();
        if (parentRef == null) return CraftingColors.GLYPH_LINK;

        GlyphComponent parentGlyph = accessor.getComponent(parentRef, GlyphComponent.getComponentType());
        if (parentGlyph == null) return CraftingColors.ANCHOR;

        Slot slot = parentGlyph.getGlyph().getSlot(slotComp.getSlotKey());
        if (slot == null || slot.getColor() == null) return CraftingColors.GLYPH_LINK;
        return slot.getColor();
    }

    @Override
    public InteractionState exit(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            Ref<EntityStore> playerRef) {
        HexcasterCraftingComponent craftingComp = accessor.getComponent(playerRef,
                HexcasterCraftingComponent.getComponentType());
        if (craftingComp == null) return InteractionState.Failed;

        Ref<EntityStore> dropTargetRef = craftingComp.getHoveredRef();
        craftingComp.setDraggingRef(null);
        craftingComp.setDragTickCount(0);
        if (dropTargetRef == null || !dropTargetRef.isValid()) return InteractionState.Finished;

        Ref<EntityStore> targetGlyphRef = HoverableUtils.getGlyphFromHoverable(accessor, dropTargetRef);
        if (targetGlyphRef == null || !targetGlyphRef.isValid()) return InteractionState.Finished;

        NodeComponent slotNode = accessor.getComponent(nodeRef, NodeComponent.getComponentType());
        SlotComponent slotComp = accessor.getComponent(nodeRef, SlotComponent.getComponentType());
        if (slotNode == null || slotComp == null) return InteractionState.Finished;

        Ref<EntityStore> sourceRef = slotNode.getParentEntity();
        if (sourceRef == null || sourceRef.equals(targetGlyphRef)) return InteractionState.Finished;

        GlyphComponent targetGlyph = accessor.getComponent(targetGlyphRef, GlyphComponent.getComponentType());
        if (targetGlyph == null) return InteractionState.Finished;

        GlyphComponent sourceGlyph = accessor.getComponent(sourceRef, GlyphComponent.getComponentType());
        if (sourceGlyph == null) return InteractionState.Finished;

        Slot slot = sourceGlyph.getGlyph().getSlot(slotComp.getSlotKey());
        if (slot != null && slot.isUnique() && slot.getLinks().length >= 1) {
            LOGGER.atInfo().log("slot: rejected link to unique slot '%s' on %s (already has %d link(s))",
                    slotComp.getSlotKey(), sourceGlyph.getGlyphId(), slot.getLinks().length);
            return InteractionState.Failed;
        }
        sourceGlyph.getGlyph().addSlotLink(slotComp.getSlotKey(), targetGlyph.getId());
        LOGGER.atInfo().log("slot: connected '%s' on %s to glyph %s",
                slotComp.getSlotKey(), sourceGlyph.getGlyphId(), targetGlyph.getId());
        return InteractionState.Finished;
    }

    @Override
    public InteractionState ability(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef,
            InteractionType inputType, Ref<EntityStore> playerRef) {
        if (inputType != InteractionType.Ability3) return InteractionState.Failed;

        SlotComponent slotComp = accessor.getComponent(nodeRef, SlotComponent.getComponentType());
        NodeComponent nodeComp = accessor.getComponent(nodeRef, NodeComponent.getComponentType());
        if (slotComp == null || nodeComp == null) return InteractionState.Failed;

        Ref<EntityStore> parentRef = nodeComp.getParentEntity();
        if (parentRef == null) return InteractionState.Failed;

        GlyphComponent parentGlyph = accessor.getComponent(parentRef, GlyphComponent.getComponentType());
        if (parentGlyph == null) return InteractionState.Failed;

        parentGlyph.getGlyph().clearSlot(slotComp.getSlotKey());
        return InteractionState.Finished;
    }

    @Override
    public InteractionState click(CommandBuffer<EntityStore> accessor, Ref<EntityStore> node,
            Ref<EntityStore> playerRef) {
        return InteractionState.Finished;
    }

    public void spawnSlotsForGlyph(CommandBuffer<EntityStore> accessor, Ref<EntityStore> glyphRef,
            Ref<EntityStore> playerRef) {
        GlyphComponent glyphComp = accessor.getComponent(glyphRef, GlyphComponent.getComponentType());
        if (glyphComp == null) return;

        Glyph glyph = glyphComp.getGlyph();
        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null) return;

        Map<String, SlotAsset> assetSlots = asset.getSlots();
        if (assetSlots.isEmpty()) return;

        List<Vector3f> radialFallbacks = computeRadialFallbacks(assetSlots);
        int radialIndex = 0;

        for (Map.Entry<String, SlotAsset> entry : assetSlots.entrySet()) {
            String key = entry.getKey();
            SlotAsset slotAsset = entry.getValue();
            Vector3f offset = slotAsset.getOffset();
            if (offset == null) {
                offset = radialFallbacks.get(radialIndex++);
            }

            Slot slot = glyph.getOrCreateSlot(key);
            slot.hydrateFrom(slotAsset, key, offset);

            TransformComponent parentTransform = accessor.getComponent(glyphRef, TransformComponent.getComponentType());
            if (parentTransform == null) continue;

            Ref<EntityStore> slotRef = spawnSlotEntityAt(accessor, glyphRef, key, slotAsset, offset,
                    parentTransform.getPosition());
            if (slotRef != null) {
                glyphComp.getSlotEntityRefs().add(slotRef);
            }
        }
    }

    private List<Vector3f> computeRadialFallbacks(Map<String, SlotAsset> assetSlots) {
        int unsetCount = 0;
        for (SlotAsset s : assetSlots.values()) {
            if (s.getOffset() == null) unsetCount++;
        }
        if (unsetCount == 0) return new ArrayList<>();
        return RadialPositionUtil.calculateOffsets(unsetCount, SLOT_RADIUS, 0f, null);
    }

    private Ref<EntityStore> spawnSlotEntityAt(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> parentRef, String slotKey, SlotAsset asset, Vector3f offset,
            Vector3d parentWorldPos) {
        Vector3d slotPos = new Vector3d(
                parentWorldPos.x + offset.x,
                parentWorldPos.y + offset.y,
                parentWorldPos.z + offset.z);

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(slotPos, new Vector3f(0, 0, 0)));

        holder.addComponent(SlotComponent.getComponentType(), new SlotComponent(slotKey));

        holder.addComponent(DebugComponent.getComponentType(),
                new DebugComponent(asset.getShape(), asset.getColor(), SLOT_SCALE, SLOT_RESPAWN_INTERVAL));

        Box slotBox = new Box(-SLOT_SCALE, -SLOT_SCALE, -SLOT_SCALE,
                SLOT_SCALE, SLOT_SCALE, SLOT_SCALE);
        holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(slotBox));

        holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(UUID.randomUUID()));
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

        int networkId = accessor.getExternalData().takeNextNetworkId();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));

        HoverableComponent hoverable = new HoverableComponent(HoverableType.NODE);
        hoverable.setHintText("title", asset.getLabel());
        hoverable.setHintText("description", asset.getDescription());
        if (asset.getDefaultDisplay() != null) {
            hoverable.setHintText("default", asset.getDefaultDisplay());
        }
        holder.addComponent(HoverableComponent.getComponentType(), hoverable);

        holder.addComponent(NodeComponent.getComponentType(),
                new NodeComponent(parentRef, NodeType.Slot));

        holder.addComponent(MountedComponent.getComponentType(),
                new MountedComponent(parentRef, offset, MountController.Minecart));

        return accessor.addEntity(holder, AddReason.SPAWN);
    }

    public void despawnSlotsForGlyph(CommandBuffer<EntityStore> accessor, Ref<EntityStore> glyphRef) {
        GlyphComponent glyphComp = accessor.getComponent(glyphRef, GlyphComponent.getComponentType());
        if (glyphComp == null) return;

        for (Ref<EntityStore> slotRef : glyphComp.getSlotEntityRefs()) {
            if (slotRef == null || !slotRef.isValid()) continue;
            accessor.tryRemoveEntity(slotRef, RemoveReason.REMOVE);
        }
        glyphComp.getSlotEntityRefs().clear();
    }

    @Override
    public void despawn(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
        accessor.tryRemoveEntity(nodeRef, RemoveReason.REMOVE);
    }

    // legacy bulk-despawn entry point used by pedestal close + crafting close.
    // under per-glyph SlotsVisible there's no global "close all slots" \u2014 slot entities
    // ride along with their parent glyph entity removal via MountedComponent.
    public void despawn(CommandBuffer<EntityStore> accessor,
            com.riprod.hexcode.core.state.crafting.session.HexcodeSessionComponent session) {
        // intentionally a no-op
    }

    @Override
    public void hover(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
        DebugComponent debug = accessor.getComponent(nodeRef, DebugComponent.getComponentType());
        if (debug == null) return;
        debug.setScaleMultiplier(1.3f);
        debug.setIntervalMultiplier(0.25f);
        debug.setFadeMultiplier(0.25f);
        debug.setTimer(0);
    }

    @Override
    public void unhover(CommandBuffer<EntityStore> accessor, Ref<EntityStore> nodeRef, Ref<EntityStore> playerRef) {
        DebugComponent debug = accessor.getComponent(nodeRef, DebugComponent.getComponentType());
        if (debug == null) return;
        debug.resetScaleMultiplier();
        debug.resetFadeMultipler();
        debug.resetIntervalMultiplier();
    }
}
