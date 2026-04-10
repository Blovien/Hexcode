package com.riprod.hexcode.builtin.glyphs.effect.glaciate;

import java.util.List;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.builtin.glyphs.effect.glaciate.component.GlaciateComponent;
import com.riprod.hexcode.builtin.glyphs.effect.glaciate.style.GlaciateStyle;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.common.trigger.TriggerHandler;
import com.riprod.hexcode.core.common.trigger.component.TriggerComponent;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexRoot;
import com.riprod.hexcode.core.state.execution.component.HexSignal;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;

public class GlaciateTriggerHandler implements TriggerHandler {

    private static int damageCauseIndex = -1;

    @Override
    public boolean onTick(float dt, Ref<EntityStore> entityRef,
            ArchetypeChunk<EntityStore> chunk, int index,
            TriggerComponent trigger, HexSignal signal,
            CommandBuffer<EntityStore> buffer) {

        GlaciateComponent glaciate = chunk.getComponent(index, GlaciateComponent.getComponentType());
        TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());
        if (glaciate == null || transform == null) return false;

        if (!glaciate.firedFirstBranch() && glaciate.getFirstBranchIds() != null
                && trigger.getRemainingLifetime() <= trigger.getInitialLifetime() * 0.99f) {
            fireFirstBranch(glaciate, signal, entityRef, buffer);
            glaciate.markFirstBranchFired();
        }

        Velocity vel = chunk.getComponent(index, Velocity.getComponentType());
        Vector3d iceVelocity = vel != null ? new Vector3d(vel.getVelocity()) : Vector3d.ZERO;
        double speed = vel != null ? vel.getSpeed() : 0;

        detectEntities(glaciate, signal, entityRef, transform, speed, iceVelocity, buffer);
        return false;
    }

    @Override
    public void onCleanup(Ref<EntityStore> entityRef, TriggerComponent trigger,
            HexSignal signal, CommandBuffer<EntityStore> buffer) {
        TransformComponent transform = buffer.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform != null) {
            GlaciateStyle.renderMelt(transform.getPosition(),
                    signal != null && signal.getPrimary() != null
                            ? signal.getPrimary().getHexContext().getColors() : null,
                    buffer);
        }
    }

    private void detectEntities(GlaciateComponent glaciate, HexSignal signal,
            Ref<EntityStore> iceRef, TransformComponent transform,
            double speed, Vector3d iceVelocity, CommandBuffer<EntityStore> buffer) {
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
                applyKnockback(ref, iceVelocity, speed, buffer);
            }

            GlaciateStyle.renderImpact(center,
                    signal != null && signal.getPrimary() != null
                            ? signal.getPrimary().getHexContext().getColors() : null,
                    buffer);

            fireOnEntity(signal, ref, uuid, buffer);
        }
    }

    private void applyKnockback(Ref<EntityStore> ref, Vector3d iceVelocity, double speed,
            CommandBuffer<EntityStore> buffer) {
        Vector3d kbVelocity = new Vector3d(iceVelocity).normalize().scale(speed * 0.3);
        kbVelocity.setY(Math.max(kbVelocity.getY(), 2.0));
        KnockbackComponent kb = new KnockbackComponent();
        kb.setVelocity(kbVelocity);
        kb.setVelocityType(ChangeVelocityType.Add);
        kb.setDuration(0.0f);
        buffer.putComponent(ref, KnockbackComponent.getComponentType(), kb);
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

        UUIDComponent uuid = buffer.getComponent(iceRef, UUIDComponent.getComponentType());
        if (uuid != null && entry.getSourceGlyph() != null) {
            entry.getSourceGlyph().writeSlot("result", new EntityVar(uuid.getUuid(), iceRef), ctx);
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

            if (entry.getSourceGlyph() != null) {
                entry.getSourceGlyph().writeSlot("result",
                        new EntityVar(entityUuid.getUuid(), entityRef), ctx);
            }

            Executor.continueExecution(entry.getNextGlyphIds(), ctx);
        }
    }
}
