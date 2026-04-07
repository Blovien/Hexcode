package com.riprod.hexcode.builtin.glyphs.effect.conjure;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
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
import com.riprod.hexcode.core.common.trigger.TriggerHandler;
import com.riprod.hexcode.core.common.trigger.component.TriggerComponent;
import com.riprod.hexcode.core.state.execution.Executor;
import com.riprod.hexcode.core.state.execution.component.HexContext;
import com.riprod.hexcode.core.state.execution.component.HexRoot;
import com.riprod.hexcode.core.state.execution.component.HexSignal;
import com.riprod.hexcode.core.state.execution.component.RootGlyph;

public class ConjureTriggerHandler implements TriggerHandler {

    @Override
    public boolean onTick(float dt, Ref<EntityStore> entityRef,
            ArchetypeChunk<EntityStore> chunk, int index,
            TriggerComponent trigger, HexSignal signal,
            CommandBuffer<EntityStore> buffer) {

        ConjureZoneComponent zone = chunk.getComponent(index, ConjureZoneComponent.getComponentType());
        TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());
        if (zone == null || transform == null) return false;

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

        if (signal == null || signal.getEntries().isEmpty()) return false;

        Vector3d pos = transform.getPosition();
        Vector3d half = zone.getHalfExtents();
        Vector3d min = new Vector3d(pos.x - half.x, pos.y - half.y, pos.z - half.z);
        Vector3d max = new Vector3d(pos.x + half.x, pos.y + half.y, pos.z + half.z);

        // copy immediately — the thread-local list returned by TargetUtil is
        // invalidated by any nested spatial query from downstream glyph execution
        List<Ref<EntityStore>> found = new java.util.ArrayList<>(
                TargetUtil.getAllEntitiesInBox(min, max, buffer));

        Set<UUID> previousOccupants = zone.getNewOccupants();
        zone.setLastOccupants(previousOccupants);
        zone.setNewOccupants(new HashSet<>());

        for (Ref<EntityStore> ref : found) {
            if (ref == null || !ref.isValid()) continue;
            if (buffer.getComponent(ref, ConjureZoneComponent.getComponentType()) != null) continue;

            UUIDComponent uuid = buffer.getComponent(ref, UUIDComponent.getComponentType());
            if (uuid == null) continue;

            UUID entityId = uuid.getUuid();
            zone.getNewOccupants().add(entityId);

            if (!previousOccupants.contains(entityId)) {
                fireOnEntity(zone, signal, ref, uuid, buffer);
            }
        }

        if (zone.getInterval() > 0) {
            zone.setIntervalTimer(zone.getIntervalTimer() - dt);
            if (zone.getIntervalTimer() <= 0) {
                zone.setIntervalTimer(zone.getInterval());
                for (Ref<EntityStore> ref : found) {
                    if (ref == null || !ref.isValid()) continue;
                    UUIDComponent uuid = buffer.getComponent(ref, UUIDComponent.getComponentType());
                    if (uuid == null) continue;
                    if (!zone.getNewOccupants().contains(uuid.getUuid())) continue;
                    fireOnEntity(zone, signal, ref, uuid, buffer);
                }
            }
        }

        return false;
    }

    @Override
    public void onCleanup(Ref<EntityStore> entityRef, TriggerComponent trigger,
            HexSignal signal, CommandBuffer<EntityStore> buffer) {
        TransformComponent transform = buffer.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform != null) {
            HexSignal.SignalEntry entry = signal != null ? signal.getPrimary() : null;
            ConjureStyle.renderDespawn(transform.getPosition(),
                    entry != null ? entry.getHexContext().getColors() : null, buffer);
        }
    }

    private void fireOnEntity(ConjureZoneComponent zone, HexSignal signal,
            Ref<EntityStore> entityRef, UUIDComponent entityUuid,
            CommandBuffer<EntityStore> buffer) {

        TransformComponent entityTransform = buffer.getComponent(entityRef,
                TransformComponent.getComponentType());

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
                UUIDComponent zoneUuid = buffer.getComponent(zone.getZoneRef(),
                        UUIDComponent.getComponentType());
                if (zoneUuid != null) {
                    ctx.setVariable(conjurationOutputSlot,
                            new EntityVar(zoneUuid.getUuid(), zone.getZoneRef()));
                }
            }

            Executor.continueExecution(entry.getNextGlyphIds(), ctx);
        }
    }
}
