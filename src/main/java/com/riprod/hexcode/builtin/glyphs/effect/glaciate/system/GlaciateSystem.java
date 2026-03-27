package com.riprod.hexcode.builtin.glyphs.effect.glaciate.system;

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
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.builtin.glyphs.effect.glaciate.component.GlaciateComponent;
import com.riprod.hexcode.builtin.glyphs.effect.glaciate.style.GlaciateStyle;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexRoot;
import com.riprod.hexcode.core.state.execution.component.HexSignal;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;

public class GlaciateSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static int damageCauseIndex = -1;

    @Override
    public Query<EntityStore> getQuery() {
        return GlaciateComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        try {
            GlaciateComponent glaciate = chunk.getComponent(index, GlaciateComponent.getComponentType());
            Ref<EntityStore> entityRef = chunk.getReferenceTo(index);
            TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());

            if (glaciate == null || transform == null) {
                removeEntity(entityRef, buffer);
                return;
            }

            HexSignal signal = buffer.getComponent(entityRef, HexSignal.getComponentType());

            glaciate.decrementLifetime(dt);
            if (glaciate.getLifetime() <= 0) {
                GlaciateStyle.renderMelt(transform.getPosition(),
                        signal != null && signal.getPrimary() != null
                                ? signal.getPrimary().getHexContext().getColors() : null,
                        buffer);
                if (signal != null) signal.decrementAllWaiters(buffer);
                removeEntity(entityRef, buffer);
                return;
            }

            if (!glaciate.firedFirstBranch() && glaciate.getFirstBranchIds() != null) {
                fireFirstBranch(glaciate, signal, entityRef, buffer);
                glaciate.markFirstBranchFired();
            }

            Velocity vel = chunk.getComponent(index, Velocity.getComponentType());
            double speed = vel != null ? vel.getSpeed() : 0;

            detectEntities(glaciate, signal, entityRef, transform, speed, buffer);
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] GlaciateSystem failed: %s", e.getMessage());
        }
    }

    private void detectEntities(GlaciateComponent glaciate, HexSignal signal,
            Ref<EntityStore> iceRef, TransformComponent transform,
            double speed, CommandBuffer<EntityStore> buffer) {
        Vector3d center = transform.getPosition();
        List<Ref<EntityStore>> found = TargetUtil.getAllEntitiesInSphere(
                center, glaciate.getDamageRadius(), buffer);

        Ref<EntityStore> casterRef = null;
        if (signal != null && signal.getPrimary() != null) {
            casterRef = signal.getPrimary().getHexContext().getCasterRef();
        }

        for (Ref<EntityStore> ref : found) {
            if (ref == null || !ref.isValid()) continue;
            if (ref.equals(iceRef)) continue;
            if (ref.equals(casterRef)) continue;
            if (buffer.getComponent(ref, GlaciateComponent.getComponentType()) != null) continue;

            UUIDComponent uuid = buffer.getComponent(ref, UUIDComponent.getComponentType());
            if (uuid == null) continue;

            if (glaciate.getHitEntities().contains(uuid.getUuid())) continue;
            glaciate.getHitEntities().add(uuid.getUuid());

            if (speed > 0.1) {
                float damage = (float) (speed * glaciate.getDamageMultiplier());
                applyDamage(ref, damage, buffer);
            }

            GlaciateStyle.renderImpact(center,
                    signal != null && signal.getPrimary() != null
                            ? signal.getPrimary().getHexContext().getColors() : null,
                    buffer);

            fireOnEntity(signal, ref, uuid, buffer);
        }
    }

    private void fireFirstBranch(GlaciateComponent glaciate, HexSignal signal,
            Ref<EntityStore> iceRef, CommandBuffer<EntityStore> buffer) {
        if (signal == null || signal.getPrimary() == null) return;

        HexSignal.SignalEntry entry = signal.getPrimary();
        Ref<EntityStore> hexEntityRef = entry.getHexEntityRef();
        if (hexEntityRef == null || !hexEntityRef.isValid()) return;

        RootGlyph execComp = buffer.getComponent(hexEntityRef, RootGlyph.getComponentType());
        if (execComp == null) return;

        HexRoot root = execComp.getRoot();
        if (root == null || !root.isAlive()) return;

        ComponentAccessor<ChunkStore> chunkAccessor = buffer.getExternalData().getWorld()
                .getChunkStore().getStore();

        HexContext ctx = entry.getHexContext().copy();
        ctx.UpdateAccessor(buffer);
        ctx.UpdateChunkAccessor(chunkAccessor);

        Integer outputSlot = entry.getOutputSlot("result");
        if (outputSlot != null) {
            UUIDComponent uuid = buffer.getComponent(iceRef, UUIDComponent.getComponentType());
            if (uuid != null) {
                ctx.setVariable(outputSlot, new EntityVar(uuid.getUuid(), iceRef));
            }
        }

        Executor.continueExecution(glaciate.getFirstBranchIds(), ctx);
    }

    private void applyDamage(Ref<EntityStore> targetRef, float amount,
            CommandBuffer<EntityStore> buffer) {
        if (damageCauseIndex < 0) {
            damageCauseIndex = DamageCause.getAssetMap().getIndex("Environment");
        }
        if (damageCauseIndex == Integer.MIN_VALUE) return;

        DamageCause cause = DamageCause.getAssetMap().getAsset(damageCauseIndex);
        if (cause == null) return;

        Damage damage = new Damage(
                new Damage.EnvironmentSource("hex_glaciate"), cause, amount);
        DamageSystems.executeDamage(targetRef, buffer, damage);
    }

    private void fireOnEntity(HexSignal signal, Ref<EntityStore> entityRef,
            UUIDComponent entityUuid, CommandBuffer<EntityStore> buffer) {
        if (signal == null || signal.getEntries().isEmpty()) return;

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

            Integer outputSlot = entry.getOutputSlot("result");
            if (outputSlot != null) {
                ctx.setVariable(outputSlot, new EntityVar(entityUuid.getUuid(), entityRef));
            }

            Executor.continueExecution(entry.getNextGlyphIds(), ctx);
        }
    }

    private void removeEntity(Ref<EntityStore> entityRef, CommandBuffer<EntityStore> buffer) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        buffer.removeEntity(entityRef, holder, RemoveReason.REMOVE);
    }
}
