package com.riprod.hexcode.builtin.glyphs.ensnare;

import java.util.List;
import java.util.UUID;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.builtin.glyphs.ensnare.component.EnsnareComponent;
import com.riprod.hexcode.builtin.glyphs.ensnare.component.SpikeEntry;
import com.riprod.hexcode.builtin.glyphs.ensnare.style.EnsnareStyle;
import com.riprod.hexcode.core.common.construct.component.ConstructTickContext;
import com.riprod.hexcode.core.common.construct.component.HexStatus;
import com.riprod.hexcode.core.common.construct.handler.ConstructHandler;
import com.riprod.hexcode.core.common.construct.state.NoState;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.state.execution.HexExecuter;
import com.riprod.hexcode.core.state.execution.component.HexContext;

public class EnsnareConstructHandler implements ConstructHandler<NoState> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final double SPIKE_HIT_RADIUS_SQ = 0.7 * 0.7;
    private static int damageCauseIndex = -1;

    @Override
    public boolean onTick(float dt, HexStatus<NoState> status, ConstructTickContext ctx) {
        EnsnareComponent ensnare = ctx.getChunk().getComponent(
                ctx.getIndex(), EnsnareComponent.getComponentType());
        if (ensnare == null) return true;

        ensnare.incrementElapsed(dt);
        if (ensnare.getElapsedSeconds() >= ensnare.getDurationSeconds()) return true;

        processDamage(ensnare, status, ctx);
        return !drainSustain(dt, status);
    }

    @Override
    public void onCleanup(HexStatus<NoState> status, ConstructTickContext ctx) {
        EnsnareComponent ensnare = ctx.getChunk().getComponent(
                ctx.getIndex(), EnsnareComponent.getComponentType());
        if (ensnare != null) {
            removeSpikes(ensnare, status, ctx.getBuffer());
            LOGGER.atInfo().log("ensnare: expired after %.1fs, removed %d spikes",
                    ensnare.getDurationSeconds(), ensnare.getSpikes().size());
        }

        ctx.getBuffer().tryRemoveEntity(ctx.getEntityRef(), RemoveReason.REMOVE);
    }

    private void processDamage(EnsnareComponent ensnare, HexStatus<NoState> status,
            ConstructTickContext ctx) {
        CommandBuffer<EntityStore> buffer = ctx.getBuffer();
        Vector3d center = ensnare.getCenter();
        double radius = ensnare.getRadius() + 1.0;
        Vector3d min = new Vector3d(center.x - radius, center.y - 3, center.z - radius);
        Vector3d max = new Vector3d(center.x + radius, center.y + 4, center.z + radius);

        List<Ref<EntityStore>> nearbyEntities = TargetUtil.getAllEntitiesInBox(min, max, buffer);
        if (nearbyEntities.isEmpty()) return;

        for (Ref<EntityStore> targetRef : nearbyEntities) {
            if (targetRef == null || !targetRef.isValid()) continue;

            UUIDComponent uuidComp = buffer.getComponent(targetRef, UUIDComponent.getComponentType());
            if (uuidComp == null) continue;

            UUID targetId = uuidComp.getUuid();
            if (!ensnare.canDamageTarget(targetId)) continue;

            TransformComponent tc = buffer.getComponent(targetRef, TransformComponent.getComponentType());
            if (tc == null) continue;

            Vector3d entityPos = tc.getPosition();
            SpikeEntry nearestSpike = findNearestSpike(entityPos, ensnare.getSpikes());
            if (nearestSpike == null) continue;

            applyDamage(buffer, targetRef, ensnare.getSpikeDamage());
            ensnare.recordDamage(targetId);
            EnsnareStyle.renderSpikeDamage(nearestSpike.getPosition(), status.getHexContext(), buffer);

            fireOnHit(status, targetRef, targetId);
        }
    }

    private void fireOnHit(HexStatus<NoState> status, Ref<EntityStore> targetRef, UUID targetId) {
        Glyph triggering = status.getTriggeringGlyph();
        if (triggering == null) return;
        HexContext hc = status.getHexContext();
        triggering.writeDefaultOutput(new EntityVar(targetId, targetRef), hc);
        HexExecuter.continueExecution(triggering.getNextLinks(), hc);
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
                new Damage.EnvironmentSource("hex_ensnare"), cause, amount);
        DamageSystems.executeDamage(targetRef, buffer, damage);
    }

    private void removeSpikes(EnsnareComponent ensnare, HexStatus<NoState> status, CommandBuffer<EntityStore> buffer) {
        for (SpikeEntry spike : ensnare.getSpikes()) {
            Ref<EntityStore> spikeRef = spike.getEntityRef();
            if (spikeRef != null && spikeRef.isValid()) {
                EnsnareStyle.renderSpikeDespawn(spike.getPosition(), status.getHexContext(), buffer);
                Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
                buffer.removeEntity(spikeRef, holder, RemoveReason.REMOVE);
            }
        }
    }
}
