package com.riprod.hexcode.core.common.construct.system;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.construct.component.HexEffectsComponent;
import com.riprod.hexcode.core.common.construct.component.HexStatus;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class HexConstructSpawner {

    private HexConstructSpawner() {
    }

    public static Holder<EntityStore> create(
            @Nonnull CommandBuffer<EntityStore> buffer,
            @Nonnull HexContext hexContext,
            @Nullable Glyph triggeringGlyph,
            @Nullable String handlerId,
            @Nonnull Vector3d position) {

        UUID constructId = UUID.randomUUID();
        HexStatus construct = new HexStatus(
                handlerId,
                hexContext,
                constructId,
                triggeringGlyph);

        HexEffectsComponent component = new HexEffectsComponent();
        component.addEffect(constructId, construct);

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(position, new Vector3f()));
        holder.ensureComponent(UUIDComponent.getComponentType());
        holder.addComponent(NetworkId.getComponentType(),
                new NetworkId(buffer.getExternalData().takeNextNetworkId()));
        holder.addComponent(HexEffectsComponent.getComponentType(), component);
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

        return holder;
    }

    public static void apply(
            @Nonnull CommandBuffer<EntityStore> buffer,
            @Nonnull Ref<EntityStore> targetRef,
            @Nonnull HexContext hexContext,
            @Nullable Glyph triggeringGlyph,
            @Nullable String handlerId) {

        UUID constructId = UUID.randomUUID();
        HexStatus construct = new HexStatus(
                handlerId,
                hexContext,
                constructId,
                triggeringGlyph);

        HexEffectsComponent existing = buffer.getComponent(targetRef, HexEffectsComponent.getComponentType());

        if (existing != null) {
            existing.addEffect(constructId, construct);
        } else {
            HexEffectsComponent component = new HexEffectsComponent();
            component.addEffect(constructId, construct);
            buffer.addComponent(targetRef, HexEffectsComponent.getComponentType(), component);

        }
    }
}
