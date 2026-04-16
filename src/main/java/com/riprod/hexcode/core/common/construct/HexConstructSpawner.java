package com.riprod.hexcode.core.common.construct;

import java.util.List;
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
import com.riprod.hexcode.core.common.construct.component.HexConstruct;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class HexConstructSpawner {

    private HexConstructSpawner() {
    }

    public static Holder<EntityStore> create(
            @Nonnull CommandBuffer<EntityStore> buffer,
            @Nonnull HexContext hexContext,
            @Nullable Glyph triggeringGlyph,
            @Nullable String handlerId,
            float lifetime,
            float manaDrainPerSecond,
            @Nullable List<String> immediateBranchIds,
            @Nullable List<String> conditionalBranchIds,
            @Nullable List<String> cleanupBranchIds,
            @Nonnull Vector3d position) {

        Ref<EntityStore> rootEntityRef = hexContext.getRoot() != null
                ? hexContext.getRoot().getRootEntityRef()
                : null;

        HexConstruct construct = new HexConstruct(
                handlerId, lifetime, manaDrainPerSecond,
                immediateBranchIds, conditionalBranchIds, cleanupBranchIds,
                hexContext.copy(), triggeringGlyph, rootEntityRef);

        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        holder.addComponent(TransformComponent.getComponentType(),
                new TransformComponent(position, new Vector3f()));
        holder.ensureComponent(UUIDComponent.getComponentType());
        holder.addComponent(NetworkId.getComponentType(),
                new NetworkId(buffer.getExternalData().takeNextNetworkId()));
        holder.addComponent(HexConstruct.getComponentType(), construct);
        holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());

        return holder;
    }

    public static void apply(
            @Nonnull CommandBuffer<EntityStore> buffer,
            @Nonnull Ref<EntityStore> targetRef,
            @Nonnull HexContext hexContext,
            @Nullable Glyph triggeringGlyph,
            @Nullable String handlerId,
            float lifetime,
            float manaDrainPerSecond,
            @Nullable List<String> immediateBranchIds,
            @Nullable List<String> conditionalBranchIds,
            @Nullable List<String> cleanupBranchIds) {

        HexConstruct existing = buffer.getComponent(targetRef, HexConstruct.getComponentType());

        if (existing != null) {
            merge(existing, handlerId, lifetime, manaDrainPerSecond,
                    immediateBranchIds, conditionalBranchIds, cleanupBranchIds,
                    hexContext);
        } else {
            Ref<EntityStore> rootEntityRef = hexContext.getRoot() != null
                    ? hexContext.getRoot().getRootEntityRef()
                    : null;

            HexConstruct construct = new HexConstruct(
                    handlerId, lifetime, manaDrainPerSecond,
                    immediateBranchIds, conditionalBranchIds, cleanupBranchIds,
                    hexContext.copy(), triggeringGlyph, rootEntityRef);

            buffer.addComponent(targetRef, HexConstruct.getComponentType(), construct);
        }
    }

    private static void merge(HexConstruct existing, String handlerId,
            float lifetime, float manaDrainPerSecond,
            List<String> immediateBranchIds,
            List<String> conditionalBranchIds,
            List<String> cleanupBranchIds,
            HexContext incomingContext) {

        // handler must match for merge
        if (handlerId != null && existing.getHandlerId() != null
                && !handlerId.equals(existing.getHandlerId())) {
            return;
        }

        // take the longer remaining lifetime
        if (lifetime > existing.getRemainingLifetime()) {
            existing.setRemainingLifetime(lifetime);
        }

        // sum drain rates
        existing.setManaDrainPerSecond(existing.getManaDrainPerSecond() + manaDrainPerSecond);

        // append branches
        if (immediateBranchIds != null) {
            existing.getImmediateBranchIds().addAll(immediateBranchIds);
        }
        if (conditionalBranchIds != null) {
            existing.getConditionalBranchIds().addAll(conditionalBranchIds);
        }
        if (cleanupBranchIds != null) {
            existing.getCleanupBranchIds().addAll(cleanupBranchIds);
        }

        // merge variables — existing wins on clash
        if (incomingContext.getVariables() != null) {
            for (var entry : incomingContext.getVariables().entrySet()) {
                existing.getHexContext().getVariables().putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
    }
}
