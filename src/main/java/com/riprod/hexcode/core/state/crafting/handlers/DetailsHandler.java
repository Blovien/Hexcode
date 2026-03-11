package com.riprod.hexcode.core.state.crafting.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.glyphs.component.GlyphComponent;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.registry.SlotDefinition;
import com.riprod.hexcode.core.common.glyphs.utils.GlyphSlotType;
import com.riprod.hexcode.core.common.hidden.utils.HiddenUtils;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.common.utilities.component.DebugComponent;
import com.riprod.hexcode.core.state.crafting.component.NodeComponent;
import com.riprod.hexcode.core.state.crafting.component.SlotComponent;
import com.riprod.hexcode.core.state.crafting.constants.CraftingColors;
import com.riprod.hexcode.core.state.crafting.constants.NodeType;
import com.riprod.hexcode.core.state.crafting.utils.RadialPositionUtil;

public class DetailsHandler {
    private static final float SLOT_RADIUS = 0.6f;
    private static final double SLOT_SCALE = 0.2;

    public static List<Ref<EntityStore>> openDetails(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> glyphRef, Ref<EntityStore> playerRef) {
        List<Ref<EntityStore>> slotRefs = new ArrayList<>();

        GlyphComponent effect = accessor.getComponent(glyphRef, GlyphComponent.getComponentType());
        Glyph glyph = effect.getGlyph();

        GlyphAsset asset = GlyphAsset.getAssetMap().getAsset(glyph.getGlyphId());
        if (asset == null)
            return slotRefs;

        List<String> inputKeys = new ArrayList<>(asset.getInputKeys());
        List<String> outputKeys = new ArrayList<>(asset.getOutputKeys());

        int totalSlots = inputKeys.size() + outputKeys.size();
        if (totalSlots <= 0)
            return slotRefs;

        List<Vector3f> offsets = RadialPositionUtil.calculateOffsets(totalSlots, SLOT_RADIUS, 0f, null);

        int idx = 0;
        for (String key : inputKeys) {
            Vector3f offset = offsets.get(idx++);
            SlotDefinition def = asset.getInputDef(key);
            String title = def != null ? def.getTitle() : key;
            Ref<EntityStore> slotRef = spawnSlotEntity(accessor, offset,
                    glyphRef, new SlotComponent(key, GlyphSlotType.Input), playerRef, title);
            slotRefs.add(slotRef);
        }

        for (String key : outputKeys) {
            Vector3f offset = offsets.get(idx++);
            SlotDefinition def = asset.getOutputDef(key);
            String title = def != null ? def.getTitle() : key;
            Ref<EntityStore> slotRef = spawnSlotEntity(accessor, offset,
                    glyphRef, new SlotComponent(key, GlyphSlotType.Output), playerRef, title);
            slotRefs.add(slotRef);
        }

        effect.setDetailsOpen(true);
        return slotRefs;
    }

    private static Ref<EntityStore> spawnSlotEntity(CommandBuffer<EntityStore> accessor, Vector3f offset,
            Ref<EntityStore> glyphRef, SlotComponent slotComp,
            Ref<EntityStore> playerRef, String hintText) {

        TransformComponent glyphTransform = accessor.getComponent(glyphRef,
                TransformComponent.getComponentType());
        if (glyphTransform == null)
            return null;
        Vector3d parentPos = glyphTransform.getPosition();

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        HiddenUtils.addHiddenToHolder(accessor, holder, playerRef);

        Vector3d slotPos = new Vector3d(
                parentPos.x + offset.x,
                parentPos.y + offset.y,
                parentPos.z + offset.z);

        Vector3f color = slotComp.getSlotType() == GlyphSlotType.Input ? CraftingColors.INPUT : CraftingColors.OUTPUT;

        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(slotPos, new Vector3f(0, 0, 0)));

        holder.addComponent(SlotComponent.getComponentType(), slotComp);
        holder.addComponent(DebugComponent.getComponentType(),
                new DebugComponent(DebugShape.Cube, color, SLOT_SCALE, 2.0f));

        Box slotBox = new Box(-SLOT_SCALE, -SLOT_SCALE, -SLOT_SCALE,
                SLOT_SCALE, SLOT_SCALE, SLOT_SCALE);
        holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(slotBox));

        holder.addComponent(UUIDComponent.getComponentType(),
                new UUIDComponent(UUID.randomUUID()));
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

        int networkId = accessor.getExternalData().takeNextNetworkId();
        holder.addComponent(NetworkId.getComponentType(), new NetworkId(networkId));
        HoverableComponent hoverable = new HoverableComponent(HoverableType.NODE);
        hoverable.setHintText(hintText);
        holder.addComponent(HoverableComponent.getComponentType(), hoverable);

        holder.addComponent(NodeComponent.getComponentType(),
                new NodeComponent(glyphRef, NodeType.Slot));

        holder.addComponent(MountedComponent.getComponentType(),
                new MountedComponent(glyphRef, offset, MountController.Minecart));

        Ref<EntityStore> ref = accessor.addEntity(holder, AddReason.SPAWN);

        return ref;
    }

    public static boolean isOpenFor(CommandBuffer<EntityStore> accessor,
            List<Ref<EntityStore>> slotRefs, Ref<EntityStore> glyphRef) {
        if (slotRefs == null || slotRefs.isEmpty() || glyphRef == null) return false;
        for (Ref<EntityStore> slotRef : slotRefs) {
            if (slotRef == null || !slotRef.isValid()) continue;
            NodeComponent nodeComp = accessor.getComponent(slotRef, NodeComponent.getComponentType());
            if (nodeComp != null && glyphRef.equals(nodeComp.getParentEntity())) {
                return true;
            }
        }
        return false;
    }

    public static void closeDetails(CommandBuffer<EntityStore> accessor,
            List<Ref<EntityStore>> slotRefs) {
        if (slotRefs == null) return;
        for (Ref<EntityStore> ref : slotRefs) {
            if (ref == null || !ref.isValid()) continue;
            NodeComponent nodeComp = accessor.getComponent(ref, NodeComponent.getComponentType());
            if (nodeComp != null && nodeComp.getParentEntity() != null && nodeComp.getParentEntity().isValid()) {
                GlyphComponent glyphComp = accessor.getComponent(nodeComp.getParentEntity(),
                        GlyphComponent.getComponentType());
                if (glyphComp != null) {
                    glyphComp.setDetailsOpen(false);
                }
            }
            accessor.tryRemoveEntity(ref, RemoveReason.REMOVE);
        }
        slotRefs.clear();
    }
}
