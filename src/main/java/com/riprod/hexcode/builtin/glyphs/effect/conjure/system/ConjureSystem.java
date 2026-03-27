package com.riprod.hexcode.builtin.glyphs.effect.conjure.system;

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
import com.riprod.hexcode.builtin.glyphs.effect.conjure.component.ConjureZoneComponent;
import com.riprod.hexcode.builtin.glyphs.effect.conjure.style.ConjureStyle;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexRoot;
import com.riprod.hexcode.core.state.execution.component.HexSignal;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;

public class ConjureSystem extends EntityTickingSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public Query<EntityStore> getQuery() {
        return ConjureZoneComponent.getComponentType();
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> buffer) {

        try {
            ConjureZoneComponent zone = chunk.getComponent(index, ConjureZoneComponent.getComponentType());
            Ref<EntityStore> zoneRef = chunk.getReferenceTo(index);

            if (zone == null) {
                removeZone(zoneRef, buffer);
                return;
            }

            HexSignal signal = buffer.getComponent(zoneRef, HexSignal.getComponentType());
            HexSignal.SignalEntry entry = signal != null ? signal.getPrimary() : null;

            TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());

            zone.setRemainingLifetime(zone.getRemainingLifetime() - dt);
            if (zone.getRemainingLifetime() <= 0) {
                decrementWaiters(signal, buffer);
                if (transform != null) {
                    ConjureStyle.renderDespawn(transform.getPosition(),
                            entry != null ? entry.getHexContext().getColors() : null, buffer);
                }
                removeZone(zoneRef, buffer);
                return;
            }

            if (transform == null) {
                return;
            }

            if (!zone.firedFirstBranch() && zone.getFirstBranchIds() != null) {
                fireFirstBranch(zone, signal, zoneRef, buffer);
                zone.markFirstBranchFired();
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

            if (signal == null || signal.getEntries().isEmpty()) {
                return;
            }

            Vector3d pos = transform.getPosition();
            Vector3d half = zone.getHalfExtents();
            Vector3d min = new Vector3d(pos.x - half.x, pos.y - half.y, pos.z - half.z);
            Vector3d max = new Vector3d(pos.x + half.x, pos.y + half.y, pos.z + half.z);

            List<Ref<EntityStore>> found = TargetUtil.getAllEntitiesInBox(min, max, buffer);

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
                    fireOnEntity(zone, signal, ref, uuid, buffer);
                }
            }

            if (zone.getInterval() > 0) {
                zone.setIntervalTimer(zone.getIntervalTimer() - dt);
                if (zone.getIntervalTimer() <= 0) {
                    zone.setIntervalTimer(zone.getInterval());
                    for (Ref<EntityStore> ref : zone.getNewOccupants()) {
                        if (!ref.isValid()) continue;
                        UUIDComponent uuid = buffer.getComponent(ref, UUIDComponent.getComponentType());
                        if (uuid == null) continue;
                        fireOnEntity(zone, signal, ref, uuid, buffer);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.atSevere().log("[hexcode] ConjureSystem failed: %s", e.getMessage());
        }
    }

    private void fireFirstBranch(ConjureZoneComponent zone, HexSignal signal,
            Ref<EntityStore> zoneRef, CommandBuffer<EntityStore> buffer) {
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

        Integer entityOutputSlot = entry.getOutputSlot("entity");
        if (entityOutputSlot != null && zoneRef.isValid()) {
            UUIDComponent uuid = buffer.getComponent(zoneRef, UUIDComponent.getComponentType());
            if (uuid != null) {
                ctx.setVariable(entityOutputSlot, new EntityVar(uuid.getUuid(), zoneRef));
            }
        }

        Integer conjurationOutputSlot = entry.getOutputSlot("conjuration");
        if (conjurationOutputSlot != null && zoneRef.isValid()) {
            UUIDComponent uuid = buffer.getComponent(zoneRef, UUIDComponent.getComponentType());
            if (uuid != null) {
                ctx.setVariable(conjurationOutputSlot, new EntityVar(uuid.getUuid(), zoneRef));
            }
        }

        Executor.continueExecution(zone.getFirstBranchIds(), ctx);
    }

    private void fireOnEntity(ConjureZoneComponent zone, HexSignal signal,
            Ref<EntityStore> entityRef, UUIDComponent entityUuid, CommandBuffer<EntityStore> buffer) {

        TransformComponent entityTransform = buffer.getComponent(entityRef, TransformComponent.getComponentType());

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

            if (entityTransform != null) {
                ConjureStyle.renderTrigger(entityTransform.getPosition(), ctx.getColors(), buffer);
            }

            Integer entityOutputSlot = entry.getOutputSlot("entity");
            if (entityOutputSlot != null) {
                ctx.setVariable(entityOutputSlot, new EntityVar(entityUuid.getUuid(), entityRef));
            }

            Integer conjurationOutputSlot = entry.getOutputSlot("conjuration");
            if (conjurationOutputSlot != null && zone.getZoneRef() != null && zone.getZoneRef().isValid()) {
                UUIDComponent zoneUuid = buffer.getComponent(zone.getZoneRef(), UUIDComponent.getComponentType());
                if (zoneUuid != null) {
                    ctx.setVariable(conjurationOutputSlot,
                            new EntityVar(zoneUuid.getUuid(), zone.getZoneRef()));
                }
            }

            Executor.continueExecution(entry.getNextGlyphIds(), ctx);
        }
    }

    private void decrementWaiters(HexSignal signal, CommandBuffer<EntityStore> buffer) {
        if (signal == null) return;
        signal.decrementAllWaiters(buffer);
    }

    private void removeZone(Ref<EntityStore> zoneRef, CommandBuffer<EntityStore> buffer) {
        Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
        buffer.removeEntity(zoneRef, holder, RemoveReason.REMOVE);
    }
}
