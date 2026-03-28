package com.riprod.hexcode.builtin.glyphs.effect.shatter.system;

import java.util.List;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.builtin.glyphs.effect.shatter.component.ShatterComponent;
import com.riprod.hexcode.builtin.glyphs.effect.shatter.style.ShatterStyle;
import com.riprod.hexcode.core.common.glyphs.variables.BlockVar;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.glyphs.variables.HexVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexRoot;
import com.riprod.hexcode.core.state.execution.component.HexSignal;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;

public class ShatterSystem extends EntityTickingSystem<EntityStore> {

    private static final double HIT_IDENTIFICATION_RADIUS = 1.5;

    @Override
    public Query<EntityStore> getQuery() {
        return ShatterComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        ShatterComponent shatter = chunk.getComponent(index, ShatterComponent.getComponentType());
        Ref<EntityStore> shardRef = chunk.getReferenceTo(index);
        TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());
        StandardPhysicsProvider physics = chunk.getComponent(index,
                StandardPhysicsProvider.getComponentType());

        if (shatter == null || transform == null || physics == null) {
            removeShard(shardRef, buffer);
            return;
        }

        HexSignal signal = buffer.getComponent(shardRef, HexSignal.getComponentType());
        if (signal == null || signal.getEntries().isEmpty()) {
            removeShard(shardRef, buffer);
            return;
        }

        if (!signal.hasLiveEntries(buffer)) {
            removeShard(shardRef, buffer);
            return;
        }

        StandardPhysicsProvider.STATE state = physics.getState();
        Vector3d currentPos = transform.getPosition();
        HexContext hexContext = signal.getPrimary().getHexContext();

        if (state == StandardPhysicsProvider.STATE.INACTIVE) {
            Vector3d contactPos = physics.getContactPosition();
            Ref<EntityStore> hitEntity = findEntityAtPosition(contactPos, shatter.getCasterRef(),
                    shardRef, buffer);

            HexVar resultVar = null;
            if (hitEntity != null) {
                UUIDComponent uuidComp = buffer.getComponent(hitEntity, UUIDComponent.getComponentType());
                if (uuidComp != null) {
                    resultVar = new EntityVar(uuidComp.getUuid(), hitEntity);
                }
            } else {
                resultVar = new BlockVar(contactPos.toVector3i());
            }

            ShatterStyle.renderShardHit(contactPos, hexContext.getColors(), buffer);
            continueShardExecution(signal, resultVar, buffer);
            removeShard(shardRef, buffer);
            return;
        }

        if (state == StandardPhysicsProvider.STATE.RESTING) {
            Vector3d contactPos = physics.getContactPosition();
            HexVar resultVar = new BlockVar(contactPos.toVector3i());

            ShatterStyle.renderShardHit(contactPos, hexContext.getColors(), buffer);
            continueShardExecution(signal, resultVar, buffer);
            removeShard(shardRef, buffer);
            return;
        }

        double distanceTraveled = new Vector3d(currentPos).subtract(shatter.getSpawnPosition()).length();
        if (distanceTraveled >= shatter.getMaxDistance()) {
            ShatterStyle.renderMiss(currentPos, hexContext.getColors(), buffer);
            continueShardExecution(signal, null, buffer);
            removeShard(shardRef, buffer);
        }
    }

    private Ref<EntityStore> findEntityAtPosition(Vector3d position, Ref<EntityStore> casterRef,
            Ref<EntityStore> shardRef, CommandBuffer<EntityStore> buffer) {
        List<Ref<EntityStore>> nearby = TargetUtil.getAllEntitiesInSphere(
                position, HIT_IDENTIFICATION_RADIUS, buffer);

        Ref<EntityStore> closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Ref<EntityStore> ref : nearby) {
            if (ref == null || !ref.isValid()) continue;
            if (ref.equals(casterRef) || ref.equals(shardRef)) continue;
            if (buffer.getComponent(ref, ShatterComponent.getComponentType()) != null) continue;

            TransformComponent tc = buffer.getComponent(ref, TransformComponent.getComponentType());
            UUIDComponent uuid = buffer.getComponent(ref, UUIDComponent.getComponentType());
            if (tc == null || uuid == null) continue;

            double dist = new Vector3d(position).subtract(tc.getPosition()).squaredLength();
            if (dist < closestDist) {
                closestDist = dist;
                closest = ref;
            }
        }

        return closest;
    }

    private void continueShardExecution(HexSignal signal, HexVar resultVar,
            CommandBuffer<EntityStore> buffer) {
        if (signal == null) return;

        ComponentAccessor<ChunkStore> chunkAccessor = buffer.getExternalData().getWorld()
                .getChunkStore().getStore();

        for (HexSignal.SignalEntry entry : signal.getEntries()) {
            if (entry.getNextGlyphIds() == null || entry.getNextGlyphIds().isEmpty()) continue;

            Ref<EntityStore> hexEntityRef = entry.getHexEntityRef();
            if (hexEntityRef == null || !hexEntityRef.isValid()) continue;

            RootGlyph execComp = buffer.getComponent(hexEntityRef, RootGlyph.getComponentType());
            if (execComp == null) continue;

            HexRoot root = execComp.getRoot();
            if (root == null || !root.isAlive()) continue;

            HexContext ctx = entry.getHexContext().copy();
            ctx.UpdateAccessor(buffer);
            ctx.UpdateChunkAccessor(chunkAccessor);

            if (resultVar != null) {
                Integer outputSlot = entry.getOutputSlot("result");
                if (outputSlot != null) {
                    ctx.setVariable(outputSlot, resultVar);
                }
            }

            Executor.continueExecution(entry.getNextGlyphIds(), ctx);
            execComp.decrementExternalWaiters();
        }
    }

    private void removeShard(Ref<EntityStore> shardRef, CommandBuffer<EntityStore> buffer) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        buffer.removeEntity(shardRef, holder, RemoveReason.REMOVE);
    }
}
