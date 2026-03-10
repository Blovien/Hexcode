package com.riprod.hexcode.core.state.crafting.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

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
import com.riprod.hexcode.core.common.glyphs.component.EffectComponent;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.component.VariableComponent;
import com.riprod.hexcode.core.common.glyphs.utils.GlyphSlotType;
import com.riprod.hexcode.core.common.glyphs.values.HexVal;
import com.riprod.hexcode.core.common.hidden.utils.HiddenUtils;
import com.riprod.hexcode.core.common.hover.component.HoverableComponent;
import com.riprod.hexcode.core.common.hover.component.HoverableType;
import com.riprod.hexcode.core.common.utilities.component.DebugComponent;

public class DetailsRenderer {

    private static final Vector3f INPUT_COLOR = new Vector3f(0.2f, 0.5f, 1.0f);
    private static final Vector3f OUTPUT_COLOR = new Vector3f(1.0f, 0.3f, 0.3f);
    private static final Vector3f EMPTY_COLOR = new Vector3f(0.5f, 0.5f, 0.5f);
    private static final float SLOT_RADIUS = 0.6f;
    private static final double SLOT_SCALE = 0.2;

    public static List<Ref<EntityStore>> openDetails(CommandBuffer<EntityStore> accessor,
            Ref<EntityStore> glyphRef, EffectComponent effect, @Nullable Ref<EntityStore> playerRefFlag) {
        List<Ref<EntityStore>> slotRefs = new ArrayList<>();
        Glyph glyph = effect.getGlyph();

        TransformComponent glyphTransform = accessor.getComponent(glyphRef,
                TransformComponent.getComponentType());
        if (glyphTransform == null) return slotRefs;

        Vector3d glyphPos = glyphTransform.getPosition();

        int inputCount = glyph.getTotalInputs();
        int outputCount = glyph.getTotalOutputs();
        List<HexVal> inputs = glyph.getInputs();
        List<HexVal> outputs = glyph.getOutputs();

        int displayInputs = inputCount == -1 ? inputs.size() + 1 : inputCount;
        int displayOutputs = outputCount == -1 ? outputs.size() + 1 : outputCount;

        int totalSlots = displayInputs + displayOutputs;
        if (totalSlots <= 0) return slotRefs;

        List<Vector3f> offsets = RadialPositionUtil.calculateOffsets(totalSlots, SLOT_RADIUS, 0f, null);

        for (int i = 0; i < displayInputs; i++) {
            Vector3f offset = offsets.get(i);
            boolean filled = i < inputs.size() && inputs.get(i) != null;
            Vector3f color = filled ? INPUT_COLOR : EMPTY_COLOR;

            Ref<EntityStore> slotRef = spawnSlotEntity(accessor, glyphPos, offset,
                    new VariableComponent(i, GlyphSlotType.Input, offset, glyphRef),
                    color, playerRefFlag);
            slotRefs.add(slotRef);
        }

        for (int i = 0; i < displayOutputs; i++) {
            Vector3f offset = offsets.get(displayInputs + i);
            boolean filled = i < outputs.size() && outputs.get(i) != null;
            Vector3f color = filled ? OUTPUT_COLOR : EMPTY_COLOR;

            Ref<EntityStore> slotRef = spawnSlotEntity(accessor, glyphPos, offset,
                    new VariableComponent(i, GlyphSlotType.Output, offset, glyphRef),
                    color, playerRefFlag);
            slotRefs.add(slotRef);
        }

        return slotRefs;
    }

    private static Ref<EntityStore> spawnSlotEntity(CommandBuffer<EntityStore> accessor,
            Vector3d parentPos, Vector3f offset, VariableComponent varComp, Vector3f color, @Nullable Ref<EntityStore> playerRefFlag) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();

        HiddenUtils.addHiddenToHolder(holder, playerRefFlag);

        Vector3d slotPos = new Vector3d(
            parentPos.x + offset.x,
            parentPos.y + offset.y,
            parentPos.z + offset.z
        );

        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(slotPos, new Vector3f(0, 0, 0)));
        holder.addComponent(VariableComponent.getComponentType(), varComp);
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
                new HoverableComponent(HoverableType.CONTAINER));

        Ref<EntityStore> ref = accessor.addEntity(holder, AddReason.SPAWN);
        varComp.setSelf(ref);

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
