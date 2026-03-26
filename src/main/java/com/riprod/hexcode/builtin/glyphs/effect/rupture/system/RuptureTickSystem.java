package com.riprod.hexcode.builtin.glyphs.effect.rupture.system;

import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.builtin.glyphs.effect.rupture.component.RuptureComponent;
import com.riprod.hexcode.builtin.glyphs.effect.rupture.component.SpikeEntry;
import com.riprod.hexcode.builtin.glyphs.effect.rupture.style.RuptureStyle;
import com.riprod.hexcode.core.state.execution.component.HexSignal;

public class RuptureTickSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final double SPIKE_HIT_RADIUS_SQ = 0.7 * 0.7;
    private static int damageCauseIndex = -1;

    @Override
    public Query<EntityStore> getQuery() {
        return RuptureComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> buffer) {
        RuptureComponent rupture = chunk.getComponent(index, RuptureComponent.getComponentType());
        Ref<EntityStore> entityRef = chunk.getReferenceTo(index);

        if (rupture == null) {
            removeEntity(entityRef, buffer);
            return;
        }

        rupture.incrementElapsed();

        if (!rupture.isExpired()) {
            processDamage(rupture, buffer);
            return;
        }

        HexSignal signal = buffer.getComponent(entityRef, HexSignal.getComponentType());

        removeSpikes(rupture, buffer);
        continueExecution(signal, buffer);
        removeEntity(entityRef, buffer);

        LOGGER.atInfo().log("rupture: expired after %d ticks, removed %d spikes",
                rupture.getDurationTicks(), rupture.getSpikes().size());
    }

    private void processDamage(RuptureComponent rupture, CommandBuffer<EntityStore> buffer) {
        Vector3d center = rupture.getCenter();
        double radius = rupture.getRadius() + 1.0;
        Vector3d min = new Vector3d(center.x - radius, center.y - 3, center.z - radius);
        Vector3d max = new Vector3d(center.x + radius, center.y + 4, center.z + radius);

        List<Ref<EntityStore>> nearbyEntities = TargetUtil.getAllEntitiesInBox(min, max, buffer);
        if (nearbyEntities.isEmpty()) return;

        for (Ref<EntityStore> targetRef : nearbyEntities) {
            if (targetRef == null || !targetRef.isValid()) continue;

            UUIDComponent uuidComp = buffer.getComponent(targetRef, UUIDComponent.getComponentType());
            if (uuidComp == null) continue;

            UUID targetId = uuidComp.getUuid();
            if (!rupture.canDamageTarget(targetId)) continue;

            TransformComponent tc = buffer.getComponent(targetRef, TransformComponent.getComponentType());
            if (tc == null) continue;

            Vector3d entityPos = tc.getPosition();
            SpikeEntry nearestSpike = findNearestSpike(entityPos, rupture.getSpikes());
            if (nearestSpike == null) continue;

            applyDamage(buffer, targetRef, rupture.getSpikeDamage());
            rupture.recordDamage(targetId);
            RuptureStyle.renderSpikeDamage(nearestSpike.getPosition(), buffer);
        }
    }

    private SpikeEntry findNearestSpike(Vector3d entityPos, List<SpikeEntry> spikes) {
        SpikeEntry nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (SpikeEntry spike : spikes) {
            Vector3d spikePos = spike.getPosition();
            double dx = entityPos.x - spikePos.x;
            double dz = entityPos.z - spikePos.z;
            double distSq = dx * dx + dz * dz;

            if (distSq < nearestDistSq && distSq <= SPIKE_HIT_RADIUS_SQ) {
                double dy = entityPos.y - spikePos.y;
                if (dy >= -0.5 && dy <= 1.5) {
                    nearestDistSq = distSq;
                    nearest = spike;
                }
            }
        }

        return nearest;
    }

    private static void applyDamage(CommandBuffer<EntityStore> buffer,
            Ref<EntityStore> targetRef, float amount) {
        if (damageCauseIndex < 0) {
            damageCauseIndex = DamageCause.getAssetMap().getIndex("Environment");
        }
        if (damageCauseIndex == Integer.MIN_VALUE) return;

        DamageCause cause = DamageCause.getAssetMap().getAsset(damageCauseIndex);
        if (cause == null) return;

        Damage damage = new Damage(
                new Damage.EnvironmentSource("hex_rupture"), cause, amount);
        DamageSystems.executeDamage(targetRef, buffer, damage);
    }

    private void removeSpikes(RuptureComponent rupture, CommandBuffer<EntityStore> buffer) {
        for (SpikeEntry spike : rupture.getSpikes()) {
            Ref<EntityStore> spikeRef = spike.getEntityRef();
            if (spikeRef != null && spikeRef.isValid()) {
                RuptureStyle.renderSpikeDespawn(spike.getPosition(), buffer);
                Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
                buffer.removeEntity(spikeRef, holder, RemoveReason.REMOVE);
            }
        }
    }

    private void continueExecution(HexSignal signal, CommandBuffer<EntityStore> buffer) {
        if (signal == null) return;
        signal.fireAllEntries(buffer);
        signal.decrementAllWaiters(buffer);
    }

    private void removeEntity(Ref<EntityStore> entityRef, CommandBuffer<EntityStore> buffer) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        buffer.removeEntity(entityRef, holder, RemoveReason.REMOVE);
    }
}
