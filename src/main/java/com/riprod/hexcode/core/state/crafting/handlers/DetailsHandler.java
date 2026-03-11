package com.riprod.hexcode.core.state.crafting.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
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
import com.riprod.hexcode.core.common.glyphs.utils.GlyphSlotType;
import com.riprod.hexcode.core.common.glyphs.values.HexValInterface;
import com.riprod.hexcode.core.common.hidden.utils.HiddenUtils;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.common.utilities.component.DebugComponent;
import com.riprod.hexcode.core.state.crafting.component.NodeComponent;
import com.riprod.hexcode.core.state.crafting.component.SlotComponent;
import com.riprod.hexcode.core.state.crafting.constants.NodeType;
import com.riprod.hexcode.core.state.crafting.utils.RadialPositionUtil;

public class DetailsHandler {

    private static final Vector3f INPUT_COLOR_FILLED = new Vector3f(0.0f, 0.8f, 0.8f); // Light cyan blue
    private static final Vector3f INPUT_COLOR = new Vector3f(0.0f, 0.4f, 0.4f); // Dark cyan blue
    private static final Vector3f OUTPUT_COLOR_FILLED = new Vector3f(1.0f, 0.6f, 0.2f); // Light orange
    private static final Vector3f OUTPUT_COLOR = new Vector3f(0.8f, 0.4f, 0.0f); // Dark orange
    private static final float SLOT_RADIUS = 0.6f;
    private static final double SLOT_SCALE = 0.2;

    public static List<Ref<EntityStore>> openDetails(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> glyphRef, Ref<EntityStore> playerRef) {
        List<Ref<EntityStore>> slotRefs = new ArrayList<>();

        GlyphComponent effect = accessor.getComponent(glyphRef, GlyphComponent.getComponentType());
        Glyph glyph = effect.getGlyph();

        int inputCount = glyph.getTotalInputs();
        int outputCount = glyph.getTotalOutputs();
        List<HexValInterface> inputs = glyph.getInputs();
        List<HexValInterface> outputs = glyph.getOutputs();

        int displayInputs = inputCount == -1 ? inputs.size() + 1 : inputCount;
        int displayOutputs = outputCount == -1 ? outputs.size() + 1 : outputCount;

        int totalSlots = displayInputs + displayOutputs;
        if (totalSlots <= 0)
            return slotRefs;

        List<Vector3f> offsets = RadialPositionUtil.calculateOffsets(totalSlots, SLOT_RADIUS, 0f, null);

        for (int i = 0; i < displayInputs; i++) {
            Vector3f offset = offsets.get(i);
            boolean filled = i < inputs.size() && inputs.get(i) != null;

            Ref<EntityStore> slotRef = spawnSlotEntity(accessor, offset,
                    glyphRef,
                     new SlotComponent(i, GlyphSlotType.Input), playerRef);
            slotRefs.add(slotRef);
        }

        for (int i = 0; i < displayOutputs; i++) {
            Vector3f offset = offsets.get(displayInputs + i);
            boolean filled = i < outputs.size() && outputs.get(i) != null;
            HexValInterface output = i < outputs.size() ? outputs.get(i) : null;

            Ref<EntityStore> slotRef = spawnSlotEntity(accessor, offset,
                    glyphRef, new SlotComponent(i, GlyphSlotType.Output), playerRef);
            slotRefs.add(slotRef);
        }

        return slotRefs;
    }

    private static Ref<EntityStore> spawnSlotEntity(CommandBuffer<EntityStore> accessor, Vector3f offset,
            Ref<EntityStore> glyphRef, SlotComponent slotComp,
            Ref<EntityStore> playerRef) {

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

        Vector3f color = INPUT_COLOR;

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
        holder.addComponent(HoverableComponent.getComponentType(),
                new HoverableComponent(HoverableType.NODE));

        holder.addComponent(NodeComponent.getComponentType(),
                new NodeComponent(glyphRef, NodeType.Slot));

        Ref<EntityStore> ref = accessor.addEntity(holder, AddReason.SPAWN);

        return ref;
    }

    public static void closeDetails(CommandBuffer<EntityStore> accessor,
            List<Ref<EntityStore>> slotRefs) {
        for (Ref<EntityStore> ref : slotRefs) {
            if (ref != null && ref.isValid()) {
                accessor.tryRemoveEntity(ref, RemoveReason.REMOVE);
            }
        }
        slotRefs.clear();
    }
}
