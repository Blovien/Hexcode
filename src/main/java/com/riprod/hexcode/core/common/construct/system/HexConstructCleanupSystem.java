package com.riprod.hexcode.core.common.construct.system;

import javax.annotation.Nonnull;

import com.hypixel.hytale.builtin.mounts.MountedByComponent;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.construct.component.HexConstruct;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;
import com.riprod.hexcode.utils.CleanupUtils;

public class HexConstructCleanupSystem extends RefSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return HexConstruct.getComponentType();
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {
        try {
            MountedByComponent ridden = store.getComponent(ref, MountedByComponent.getComponentType());
            if (ridden != null) {
                for (Ref<EntityStore> passenger : ridden.getPassengers()) {
                    CleanupUtils.safeRemoveEntity(buffer, passenger);
                }
            }

            HexConstruct construct = store.getComponent(ref, HexConstruct.getComponentType());
            if (construct == null) return;

            Ref<EntityStore> rootRef = construct.getRootEntityRef();
            if (rootRef == null || !rootRef.isValid()) return;

            RootGlyph rootGlyph = store.getComponent(rootRef, RootGlyph.getComponentType());
            if (rootGlyph != null) {
                rootGlyph.removeDependent(ref);
            }
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] HexConstructCleanupSystem.onEntityRemove failed: %s", e.getMessage());
        }
    }
}
