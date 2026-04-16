package com.riprod.hexcode.core.common.construct.system;

import javax.annotation.Nonnull;

import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.riprod.hexcode.core.common.construct.ConstructHandler;
import com.riprod.hexcode.core.common.construct.ConstructRegistry;
import com.riprod.hexcode.core.common.construct.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.component.HexConstruct;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexRoot;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;

public class HexConstructSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public Query<EntityStore> getQuery() {
        return HexConstruct.getComponentType();
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {
        try {
            HexConstruct construct = chunk.getComponent(index, HexConstruct.getComponentType());
            Ref<EntityStore> entityRef = chunk.getReferenceTo(index);

            if (construct == null) {
                buffer.tryRemoveEntity(entityRef, RemoveReason.REMOVE);
                return;
            }

            ConstructHandler handler = null;
            String handlerId = construct.getHandlerId();
            if (handlerId != null) {
                handler = ConstructRegistry.get(handlerId);
                if (handler == null) {
                    LOGGER.atSevere().log("no construct handler for: %s", handlerId);
                    buffer.tryRemoveEntity(entityRef, RemoveReason.REMOVE);
                    return;
                }
            }

            ConstructTickContext ctx = new ConstructTickContext(entityRef, chunk, index, buffer, construct);

            // 1. validate root alive
            if (!isRootAlive(construct, buffer)) {
                cleanup(construct, handler, ctx, entityRef, buffer);
                return;
            }

            // 2. drain mana
            if (construct.getManaDrainPerSecond() > 0) {
                float drain = construct.getManaDrainPerSecond() * dt;
                HexRoot root = resolveRoot(construct, buffer);
                if (root == null || !root.tryConsumeMana(drain, buffer)) {
                    cleanup(construct, handler, ctx, entityRef, buffer);
                    return;
                }
            }

            // 3. decrement lifetime
            if (construct.getRemainingLifetime() >= 0) {
                construct.setRemainingLifetime(construct.getRemainingLifetime() - dt);
                if (construct.getRemainingLifetime() <= 0) {
                    cleanup(construct, handler, ctx, entityRef, buffer);
                    return;
                }
            }

            // 4. fire immediate branches
            if (!construct.hasFiredImmediate()) {
                construct.markImmediateFired();
                if (!construct.getImmediateBranchIds().isEmpty()) {
                    ctx.fireBranch(construct.getImmediateBranchIds());
                }
                if (handler != null) {
                    handler.onFirstTick(construct, ctx);
                }
            }

            // 5. handler tick
            if (handler != null) {
                boolean kill = handler.onTick(dt, construct, ctx);
                if (kill) {
                    cleanup(construct, handler, ctx, entityRef, buffer);
                    return;
                }
            }

            // 6. kill requested
            if (construct.isKillRequested()) {
                cleanup(construct, handler, ctx, entityRef, buffer);
            }
        } catch (Exception e) {
            LOGGER.atSevere().log("HexConstructSystem failed: %s", e.getMessage());
        }
    }

    private void cleanup(HexConstruct construct, ConstructHandler handler,
            ConstructTickContext ctx, Ref<EntityStore> entityRef,
            CommandBuffer<EntityStore> buffer) {

        if (handler != null) {
            try {
                handler.onCleanup(construct, ctx);
            } catch (Exception e) {
                LOGGER.atWarning().log("construct handler cleanup failed: %s", e.getMessage());
            }
        }

        if (!construct.getCleanupBranchIds().isEmpty()) {
            ctx.fireBranch(construct.getCleanupBranchIds());
        }

        removeDependent(construct, entityRef, buffer);
        buffer.tryRemoveComponent(entityRef, MountedComponent.getComponentType());
        buffer.tryRemoveEntity(entityRef, RemoveReason.REMOVE);
    }

    private boolean isRootAlive(HexConstruct construct, CommandBuffer<EntityStore> buffer) {
        Ref<EntityStore> rootRef = construct.getRootEntityRef();
        if (rootRef == null || !rootRef.isValid()) return false;

        RootGlyph rootGlyph = buffer.getComponent(rootRef, RootGlyph.getComponentType());
        if (rootGlyph == null) return false;

        HexRoot root = rootGlyph.getRoot();
        return root != null && root.isAlive();
    }

    private HexRoot resolveRoot(HexConstruct construct, CommandBuffer<EntityStore> buffer) {
        Ref<EntityStore> rootRef = construct.getRootEntityRef();
        if (rootRef == null || !rootRef.isValid()) return null;

        RootGlyph rootGlyph = buffer.getComponent(rootRef, RootGlyph.getComponentType());
        if (rootGlyph == null) return null;

        return rootGlyph.getRoot();
    }

    private void removeDependent(HexConstruct construct, Ref<EntityStore> entityRef,
            CommandBuffer<EntityStore> buffer) {
        Ref<EntityStore> rootRef = construct.getRootEntityRef();
        if (rootRef == null || !rootRef.isValid()) return;

        RootGlyph rootGlyph = buffer.getComponent(rootRef, RootGlyph.getComponentType());
        if (rootGlyph != null) {
            rootGlyph.removeDependent(entityRef);
        }
    }
}
