package com.riprod.hexcode.builtin.glyphs.effect.conjure;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

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
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexRoot;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;

public class ConjureTickSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public Query<EntityStore> getQuery() {
        return ConjureZoneComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {

        ConjureZoneComponent zone = chunk.getComponent(index, ConjureZoneComponent.getComponentType());
        Ref<EntityStore> zoneRef = chunk.getReferenceTo(index);

        if (zone == null) {
            removeZone(zoneRef, zone, buffer);
            return;
        }

        TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());

        zone.setRemainingLifetime(zone.getRemainingLifetime() - dt);
        if (zone.getRemainingLifetime() <= 0) {
            decrementWaiters(zone, buffer);
            if (transform != null) {
                ConjureGlyphStyle.renderDespawn(transform.getPosition(), buffer);
            }
            removeZone(zoneRef, zone, buffer);
            return;
        }

        if (transform == null) {
            return;
        }

        Velocity vel = chunk.getComponent(index, Velocity.getComponentType());
        if (vel != null) {
            Vector3d velocity = vel.getVelocity();
            if (velocity.squaredLength() > 0) {
                Vector3d pos = transform.getPosition();
                transform.setPosition(new Vector3d(
                        pos.x + velocity.x * dt,
                        pos.y + velocity.y * dt,
                        pos.z + velocity.z * dt));
            }
        }

        if (zone.getNextGlyphs() == null || zone.getNextGlyphs().isEmpty()) {
            return;
        }

        Vector3d pos = transform.getPosition();
        Vector3d half = zone.getHalfExtents();
        Vector3d min = new Vector3d(pos.x - half.x, pos.y - half.y, pos.z - half.z);
        Vector3d max = new Vector3d(pos.x + half.x, pos.y + half.y, pos.z + half.z);

        List<Ref<EntityStore>> found = TargetUtil.getAllEntitiesInBox(min, max, buffer);

        // pointer-swap occupant tracking
        Set<Ref<EntityStore>> previousOccupants = zone.getNewOccupants();
        zone.setLastOccupants(previousOccupants);
        zone.setNewOccupants(new HashSet<>());

        for (Ref<EntityStore> ref : found) {
            if (ref == null || !ref.isValid()) continue;
            if (ref.equals(zoneRef)) continue;
            if (buffer.getComponent(ref, ConjureZoneComponent.getComponentType()) != null) continue;

            UUIDComponent uuid = buffer.getComponent(ref, UUIDComponent.getComponentType());
            if (uuid == null) continue;

            zone.getNewOccupants().add(ref);

            if (!previousOccupants.contains(ref)) {
                LOGGER.atInfo().log("conjure: entity entered zone: %s", uuid.getUuid());
                fireOnEntity(zone, ref, uuid, buffer);
            } else {
                LOGGER.atInfo().log("conjure: entity already in zone (repeat): %s", uuid.getUuid());
            }
        }

        // interval re-fire on all current occupants
        if (zone.getInterval() > 0) {
            zone.setIntervalTimer(zone.getIntervalTimer() - dt);
            if (zone.getIntervalTimer() <= 0) {
                zone.setIntervalTimer(zone.getInterval());
                for (Ref<EntityStore> ref : zone.getNewOccupants()) {
                    if (!ref.isValid()) continue;
                    UUIDComponent uuid = buffer.getComponent(ref, UUIDComponent.getComponentType());
                    if (uuid == null) continue;
                    fireOnEntity(zone, ref, uuid, buffer);
                }
            }
        }
    }

    private void fireOnEntity(ConjureZoneComponent zone, Ref<EntityStore> entityRef,
            UUIDComponent entityUuid, CommandBuffer<EntityStore> buffer) {

        TransformComponent entityTransform = buffer.getComponent(entityRef, TransformComponent.getComponentType());
        if (entityTransform != null) {
            ConjureGlyphStyle.renderTrigger(entityTransform.getPosition(), buffer);
        }

        Ref<EntityStore> hexEntityRef = zone.getHexEntityRef();
        if (hexEntityRef == null || !hexEntityRef.isValid()) return;

        RootGlyph execComp = buffer.getComponent(hexEntityRef, RootGlyph.getComponentType());
        if (execComp == null) return;

        HexRoot root = execComp.getRoot();
        if (root == null || !root.isAlive()) return;

        ComponentAccessor<ChunkStore> chunkAccessor = buffer.getExternalData().getWorld()
                .getChunkStore().getStore();

        HexContext ctx = zone.getHexContext().copy();
        ctx.UpdateAccessor(buffer);
        ctx.UpdateChunkAccessor(chunkAccessor);

        if (zone.getEntityOutputSlot() != null) {
            ctx.setVariable(zone.getEntityOutputSlot(), new EntityVar(entityUuid.getUuid(), entityRef));
        }

        if (zone.getConjurationOutputSlot() != null && zone.getZoneRef() != null && zone.getZoneRef().isValid()) {
            UUIDComponent zoneUuid = buffer.getComponent(zone.getZoneRef(), UUIDComponent.getComponentType());
            if (zoneUuid != null) {
                ctx.setVariable(zone.getConjurationOutputSlot(),
                        new EntityVar(zoneUuid.getUuid(), zone.getZoneRef()));
            }
        }

        Executor.continueExecution(zone.getNextGlyphs(), ctx);
    }

    private void decrementWaiters(ConjureZoneComponent zone, CommandBuffer<EntityStore> buffer) {
        Ref<EntityStore> hexEntityRef = zone.getHexEntityRef();
        if (hexEntityRef == null || !hexEntityRef.isValid()) return;

        RootGlyph execComp = buffer.getComponent(hexEntityRef, RootGlyph.getComponentType());
        if (execComp == null) return;

        execComp.decrementExternalWaiters();
    }

    private void removeZone(Ref<EntityStore> zoneRef, ConjureZoneComponent zone,
            CommandBuffer<EntityStore> buffer) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        buffer.removeEntity(zoneRef, holder, RemoveReason.REMOVE);
    }
}
